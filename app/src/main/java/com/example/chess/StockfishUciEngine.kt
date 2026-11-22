package com.example.chess

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

enum class ActiveEngineBackend(val labelAr: String) {
    NATIVE_STOCKFISH("ستوك فيش الأصلي (Native UCI)"),
    OEX_STOCKFISH("ستوك فيش الخارجي (Open Exchange)"),
    BUILTIN_FALLBACK("المحرك المدمج (Stockfish-Architecture Fallback)")
}

data class OexEngineInfo(
    val packageName: String,
    val label: String,
    val authority: String
)

data class StockfishProbeResult(
    val backend: ActiveEngineBackend = ActiveEngineBackend.BUILTIN_FALLBACK,
    val engineName: String = "محرك أرينا المدمج (TT + PVS + LMR)",
    val isNativeStockfishReady: Boolean = false,
    val nativeBinaryPath: String? = null,
    val oexEngines: List<OexEngineInfo> = emptyList(),
    val diagnosticMessageAr: String = "لم يتم العثور على libstockfish.so بعد؛ يعمل المحرك المدمج المتطور تلقائياً."
)

/**
 * Real Universal Chess Interface (UCI) driver for Stockfish on Android.
 *
 * Discovery order when [EngineBackendPreference.STOCKFISH_AUTO] is selected:
 * 1. Bundled native Stockfish executable (`libstockfish.so` / `libstockfish_nnue.so`) inside `nativeLibraryDir`
 * 2. Installed Open Exchange (`com.kalab.chess.enginesupport.ENGINE`) UCI Stockfish app if executable
 * 3. Seamless offline fallback to the upgraded [ChessEngine] (Zobrist TT + PVS + Null-Move + LMR + Quiescence)
 */
object StockfishUciEngine {

    private const val OEX_ACTION = "com.kalab.chess.enginesupport.ENGINE"
    private val CANDIDATE_SO_NAMES = listOf(
        "libstockfish.so",
        "libstockfish_nnue.so",
        "libstockfish17.so",
        "libstockfish16.so",
        "stockfish"
    )

    private val uciMutex = Mutex()
    private var activeProcess: Process? = null
    private var activeWriter: BufferedWriter? = null
    private var activeReader: BufferedReader? = null
    private var activeBinaryPath: String? = null
    private var detectedEngineName: String? = null

    fun probeAvailableEngines(context: Context): StockfishProbeResult {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val bundledBinary = CANDIDATE_SO_NAMES
            .map { File(nativeDir, it) }
            .firstOrNull { it.exists() && it.canExecute() }
            ?: nativeDir.listFiles()?.firstOrNull {
                it.name.contains("stockfish", ignoreCase = true) && it.canExecute()
            }

        val oexList = queryOexProviders(context)

        return if (bundledBinary != null) {
            val probedName = quickIdentifyUciName(bundledBinary) ?: "Stockfish Native UCI"
            StockfishProbeResult(
                backend = ActiveEngineBackend.NATIVE_STOCKFISH,
                engineName = probedName,
                isNativeStockfishReady = true,
                nativeBinaryPath = bundledBinary.absolutePath,
                oexEngines = oexList,
                diagnosticMessageAr = "محرك ستوك فيش الأصلي متصل وجاهز ($probedName)"
            )
        } else if (oexList.isNotEmpty()) {
            val firstOex = oexList.first()
            StockfishProbeResult(
                backend = ActiveEngineBackend.OEX_STOCKFISH,
                engineName = firstOex.label,
                isNativeStockfishReady = false,
                nativeBinaryPath = null,
                oexEngines = oexList,
                diagnosticMessageAr = "تم رصد محرك خارجي (${firstOex.label}). لتشغيل مباشر بدون قيود أندرويد الحديثة، يُفضّل وضع libstockfish.so في jniLibs."
            )
        } else {
            StockfishProbeResult(
                backend = ActiveEngineBackend.BUILTIN_FALLBACK,
                engineName = "المحرك المدمج (TT + PVS + LMR)",
                isNativeStockfishReady = false,
                nativeBinaryPath = null,
                oexEngines = emptyList(),
                diagnosticMessageAr = "لم يتم العثور على ملف libstockfish.so أو تطبيق Stockfish OEX؛ يعمل الآن المحرك المدمج المتطور."
            )
        }
    }

    private fun queryOexProviders(context: Context): List<OexEngineInfo> {
        return runCatching {
            val pm = context.packageManager
            val intent = Intent(OEX_ACTION)
            val resolved = pm.queryIntentContentProviders(intent, PackageManager.GET_META_DATA)
            resolved.mapNotNull { info ->
                val pInfo = info.providerInfo ?: return@mapNotNull null
                OexEngineInfo(
                    packageName = pInfo.packageName,
                    label = pInfo.loadLabel(pm).toString().ifBlank { pInfo.packageName },
                    authority = pInfo.authority
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun quickIdentifyUciName(binary: File): String? {
        return runCatching {
            val proc = ProcessBuilder(binary.absolutePath)
                .directory(binary.parentFile)
                .redirectErrorStream(true)
                .start()
            val writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            writer.write("uci\n")
            writer.flush()
            var engineName: String? = null
            val deadline = System.currentTimeMillis() + 1200L
            while (System.currentTimeMillis() < deadline) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("id name ")) {
                        engineName = line.removePrefix("id name ").trim()
                    } else if (line.trim() == "uciok") {
                        break
                    }
                } else {
                    Thread.sleep(10)
                }
            }
            writer.write("quit\n")
            writer.flush()
            proc.destroy()
            engineName
        }.getOrNull()
    }

    suspend fun computeMove(
        context: Context?,
        state: ChessBoardState,
        backendPreference: EngineBackendPreference,
        targetElo: Int,
        personality: EnginePersonality,
        humanStyle: Boolean,
        moveTimeMs: Int,
        threads: Int
    ): ChessEngine.SearchResult = withContext(Dispatchers.Default) {
        if (backendPreference == EngineBackendPreference.STOCKFISH_AUTO && context != null) {
            val probe = probeAvailableEngines(context)
            if (probe.nativeBinaryPath != null) {
                val uciResult = queryNativeUciBestMove(
                    binaryPath = probe.nativeBinaryPath,
                    state = state,
                    targetElo = targetElo,
                    personality = personality,
                    moveTimeMs = moveTimeMs,
                    threads = threads
                )
                if (uciResult != null && uciResult.bestMove != null) {
                    return@withContext uciResult
                }
            }
        }

        // Seamless high-strength offline fallback using upgraded ChessEngine
        ChessEngine.chooseCustomEngineMove(
            state = state,
            targetElo = targetElo,
            personality = personality,
            humanStyle = humanStyle,
            moveTimeMs = moveTimeMs
        )
    }

    suspend fun computeBotMove(
        context: Context?,
        state: ChessBoardState,
        backendPreference: EngineBackendPreference,
        bot: BotPersona,
        adaptivePlayerElo: Int?,
        moveTimeMs: Int,
        threads: Int
    ): ChessEngine.SearchResult = withContext(Dispatchers.Default) {
        val elo = adaptivePlayerElo ?: bot.elo
        if (backendPreference == EngineBackendPreference.STOCKFISH_AUTO && context != null) {
            val probe = probeAvailableEngines(context)
            if (probe.nativeBinaryPath != null) {
                val mappedPersonality = when {
                    bot.aggressionWeight >= 1.25f -> EnginePersonality.SACRIFICE_KING
                    bot.aggressionWeight <= 0.90f -> EnginePersonality.IRON_FORTRESS
                    else -> EnginePersonality.TRADITIONAL
                }
                val uciResult = queryNativeUciBestMove(
                    binaryPath = probe.nativeBinaryPath,
                    state = state,
                    targetElo = elo,
                    personality = mappedPersonality,
                    moveTimeMs = moveTimeMs,
                    threads = threads
                )
                if (uciResult != null && uciResult.bestMove != null) {
                    return@withContext uciResult
                }
            }
        }
        ChessEngine.chooseBotMove(state, bot, adaptivePlayerElo)
    }

    suspend fun queryHintMove(
        context: Context?,
        state: ChessBoardState,
        backendPreference: EngineBackendPreference,
        moveTimeMs: Int = 900,
        threads: Int = 2
    ): ChessEngine.SearchResult = withContext(Dispatchers.Default) {
        if (backendPreference == EngineBackendPreference.STOCKFISH_AUTO && context != null) {
            val probe = probeAvailableEngines(context)
            if (probe.nativeBinaryPath != null) {
                val uciResult = queryNativeUciBestMove(
                    binaryPath = probe.nativeBinaryPath,
                    state = state,
                    targetElo = 3000,
                    personality = EnginePersonality.TRADITIONAL,
                    moveTimeMs = moveTimeMs,
                    threads = threads
                )
                if (uciResult != null && uciResult.bestMove != null) {
                    return@withContext uciResult
                }
            }
        }
        ChessEngine.evaluatePositionBestMove(state, depth = 4, timeLimitMs = moveTimeMs.toLong())
    }

    private suspend fun queryNativeUciBestMove(
        binaryPath: String,
        state: ChessBoardState,
        targetElo: Int,
        personality: EnginePersonality,
        moveTimeMs: Int,
        threads: Int
    ): ChessEngine.SearchResult? = uciMutex.withLock {
        val legal = state.generateLegalMoves()
        if (legal.isEmpty()) return@withLock null

        return@withLock runCatching {
            ensureSession(binaryPath, threads)
            val writer = activeWriter ?: return@runCatching null
            val reader = activeReader ?: return@runCatching null

            val skill = ((targetElo - 200) * 20 / 2800).coerceIn(0, 20)
            val limitStrength = targetElo < 2850
            val clampedUciElo = targetElo.coerceIn(1320, 3190)

            sendUciLine(writer, "setoption name Skill Level value $skill")
            sendUciLine(writer, "setoption name UCI_LimitStrength value ${if (limitStrength) "true" else "false"}")
            if (limitStrength) {
                sendUciLine(writer, "setoption name UCI_Elo value $clampedUciElo")
            }
            if (personality == EnginePersonality.IRON_FORTRESS || personality == EnginePersonality.PATIENT_SERPENT) {
                sendUciLine(writer, "setoption name Ponder value false")
            }
            sendUciLine(writer, "isready")
            waitForToken(reader, "readyok", 1500L)

            sendUciLine(writer, "position fen ${state.toFen()}")
            val safeMs = moveTimeMs.coerceIn(150, 5000)
            sendUciLine(writer, "go movetime $safeMs")

            var bestMoveStr: String? = null
            var latestDepth = 1
            var latestNodes = 0
            var latestScoreSideCp = 0
            var latestPv = emptyList<String>()

            val deadline = System.currentTimeMillis() + safeMs + 2500L
            while (System.currentTimeMillis() < deadline) {
                val line = reader.readLine() ?: break
                if (line.startsWith("info ")) {
                    parseUciInfoLine(line)?.let { parsed ->
                        latestDepth = parsed.depth
                        latestNodes = parsed.nodes
                        latestScoreSideCp = parsed.scoreCp
                        if (parsed.pv.isNotEmpty()) latestPv = parsed.pv
                    }
                } else if (line.startsWith("bestmove ")) {
                    val tokens = line.trim().split(Regex("\\s+"))
                    bestMoveStr = tokens.getOrNull(1)
                    break
                }
            }

            val matchedMove = bestMoveStr?.let { uci ->
                legal.find { it.toUci().equals(uci, ignoreCase = true) }
            }
            val evalWhiteCp = if (state.turn == PieceColor.WHITE) latestScoreSideCp else -latestScoreSideCp
            ChessEngine.SearchResult(
                bestMove = matchedMove,
                evalWhiteCp = evalWhiteCp,
                depthReached = latestDepth,
                nodesSearched = latestNodes,
                pvUci = latestPv,
                sourceName = detectedEngineName ?: "Stockfish UCI"
            )
        }.onFailure {
            closeSession()
        }.getOrNull()
    }

    private fun ensureSession(binaryPath: String, threads: Int) {
        if (activeProcess != null && activeBinaryPath == binaryPath) {
            val alive = runCatching { activeProcess?.exitValue(); false }.getOrDefault(true)
            if (alive) return
        }
        closeSession()
        val file = File(binaryPath)
        val proc = ProcessBuilder(file.absolutePath)
            .directory(file.parentFile)
            .redirectErrorStream(true)
            .start()
        val writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
        val reader = BufferedReader(InputStreamReader(proc.inputStream))

        sendUciLine(writer, "uci")
        val deadline = System.currentTimeMillis() + 2000L
        while (System.currentTimeMillis() < deadline) {
            val line = reader.readLine() ?: break
            if (line.startsWith("id name ")) {
                detectedEngineName = line.removePrefix("id name ").trim()
            } else if (line.trim() == "uciok") {
                break
            }
        }
        sendUciLine(writer, "setoption name Threads value ${threads.coerceIn(1, 4)}")
        sendUciLine(writer, "setoption name Hash value 32")
        sendUciLine(writer, "isready")
        waitForToken(reader, "readyok", 1500L)

        activeProcess = proc
        activeWriter = writer
        activeReader = reader
        activeBinaryPath = binaryPath
    }

    private fun sendUciLine(writer: BufferedWriter, cmd: String) {
        writer.write(cmd)
        writer.newLine()
        writer.flush()
    }

    private fun waitForToken(reader: BufferedReader, expected: String, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val line = reader.readLine() ?: break
            if (line.trim() == expected) return
        }
    }

    fun closeSession() {
        runCatching {
            activeWriter?.write("quit\n")
            activeWriter?.flush()
        }
        runCatching { activeWriter?.close() }
        runCatching { activeReader?.close() }
        runCatching { activeProcess?.destroy() }
        activeProcess = null
        activeWriter = null
        activeReader = null
        activeBinaryPath = null
    }

    data class ParsedUciInfo(
        val depth: Int,
        val scoreCp: Int,
        val nodes: Int,
        val pv: List<String>
    )

    fun parseUciInfoLine(line: String): ParsedUciInfo? {
        if (!line.startsWith("info ") || !line.contains(" score ")) return null
        val tokens = line.trim().split(Regex("\\s+"))
        var depth = 1
        var scoreCp = 0
        var nodes = 0
        val pv = mutableListOf<String>()

        var i = 1
        while (i < tokens.size) {
            when (tokens[i]) {
                "depth" -> {
                    depth = tokens.getOrNull(i + 1)?.toIntOrNull() ?: depth
                    i += 2
                }
                "nodes" -> {
                    nodes = tokens.getOrNull(i + 1)?.toIntOrNull() ?: nodes
                    i += 2
                }
                "score" -> {
                    val kind = tokens.getOrNull(i + 1)
                    val value = tokens.getOrNull(i + 2)?.toIntOrNull() ?: 0
                    scoreCp = when (kind) {
                        "cp" -> value
                        "mate" -> if (value > 0) 99_000 - value else -99_000 - value
                        else -> scoreCp
                    }
                    i += 3
                }
                "pv" -> {
                    for (j in (i + 1) until tokens.size) {
                        pv.add(tokens[j])
                    }
                    break
                }
                else -> i++
            }
        }
        return ParsedUciInfo(depth, scoreCp, nodes, pv)
    }
}
