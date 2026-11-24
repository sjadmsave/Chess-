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
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
    val engineName: String = "محرك رُقعة المدمج (TT + PVS + LMR)",
    val isNativeStockfishReady: Boolean = false,
    val nativeBinaryPath: String? = null,
    val oexEngines: List<OexEngineInfo> = emptyList(),
    val diagnosticMessageAr: String = "لم يتم العثور على libstockfish.so بعد؛ يعمل المحرك المدمج المتطور تلقائياً."
)

/**
 * Real Universal Chess Interface (UCI) driver for Stockfish on Android,
 * adhering to Ruqaa Sections 5, 6, 7, 8:
 * - EvalFile / Threads = min(2, cores) / Hash = 32 MB
 * - SyzygyPath & Syzygy50MoveRule = true when .rtbw tables exist, with safe auto-fallback on failure
 * - ucinewgame on match/session ID change, Clear Hash on Elo change
 * - MultiPV up to 6 lines for human style & personalities, 3 lines for Analysis
 * - Ignores lowerbound/upperbound info lines and takes deepest complete MultiPV set
 */
object StockfishUciEngine {

    private const val OEX_ACTION = "com.kalab.chess.enginesupport.ENGINE"
    private val CANDIDATE_SO_NAMES = listOf(
        "libstockfish.so",
        "libstockfish_nnue.so",
        "libstockfish19.so",
        "libstockfish17.so",
        "libstockfish16.so",
        "stockfish"
    )

    private val uciMutex = Mutex()
    private val nnueCopyLock = Any()
    private const val EXPECTED_DEFAULT_NNUE_NAME = "nn-1a298aa575a0.nnue"
    private const val EXPECTED_DEFAULT_NNUE_BYTES = 98_511_183L

    @Volatile private var activeProcess: Process? = null
    @Volatile private var activeWriter: BufferedWriter? = null
    @Volatile private var activeReader: BufferedReader? = null
    @Volatile private var activeBinaryPath: String? = null
    @Volatile private var detectedEngineName: String? = null
    @Volatile private var cachedProbeResult: StockfishProbeResult? = null
    private var lastSessionId: String? = null
    private var lastConfiguredElo: Int? = null
    private var lastConfiguredThreads: Int = 2
    private var lastConfiguredHashMb: Int = 32
    private var syzygyDisabledForSession: Boolean = false

    fun resetSyzygyFallbackFlag() {
        syzygyDisabledForSession = false
    }

    private fun findNativeBinary(context: Context): File? {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        if (!nativeDir.exists()) return null
        val candidate = CANDIDATE_SO_NAMES
            .map { File(nativeDir, it) }
            .firstOrNull { it.exists() }
            ?: nativeDir.listFiles()?.firstOrNull {
                it.isFile && it.name.contains("stockfish", ignoreCase = true)
            }
        if (candidate != null && !candidate.canExecute()) {
            runCatching { candidate.setExecutable(true, false) }
        }
        return candidate?.takeIf { it.exists() && it.canExecute() }
    }

    /**
     * Fast non-blocking initial inspection safe to call on startup before Dispatchers.IO warmup.
     */
    fun quickInspectStatic(context: Context): StockfishProbeResult {
        cachedProbeResult?.let { return it }
        val bundledBinary = findNativeBinary(context)
        val oexList = queryOexProviders(context)
        return when {
            bundledBinary != null -> StockfishProbeResult(
                backend = ActiveEngineBackend.NATIVE_STOCKFISH,
                engineName = detectedEngineName ?: "Stockfish 19 NNUE",
                isNativeStockfishReady = true,
                nativeBinaryPath = bundledBinary.absolutePath,
                oexEngines = oexList,
                diagnosticMessageAr = "جاري تهيئة محرك ستوك فيش الأصلي..."
            )
            oexList.isNotEmpty() -> {
                val firstOex = oexList.first()
                StockfishProbeResult(
                    backend = ActiveEngineBackend.OEX_STOCKFISH,
                    engineName = firstOex.label,
                    isNativeStockfishReady = false,
                    nativeBinaryPath = null,
                    oexEngines = oexList,
                    diagnosticMessageAr = "تم رصد محرك خارجي (${firstOex.label}). يعمل المحرك المدمج مع جاهزية OEX."
                )
            }
            else -> StockfishProbeResult(
                backend = ActiveEngineBackend.BUILTIN_FALLBACK,
                engineName = "محرك رُقعة المدمج (TT + PVS + LMR)",
                isNativeStockfishReady = false,
                nativeBinaryPath = null,
                oexEngines = emptyList(),
                diagnosticMessageAr = "لم يتم العثور على libstockfish.so؛ يعمل المحرك المدمج المتطور تلقائياً."
            )
        }
    }

    /**
     * Extracts bundled NNUE weights on IO thread and performs a real UCI handshake probe.
     */
    suspend fun warmupAndProbe(context: Context): StockfishProbeResult = withContext(Dispatchers.IO) {
        ensureBundledNnueCopied(context)
        probeAvailableEngines(context, forceRefresh = true)
    }

    fun probeAvailableEngines(context: Context, forceRefresh: Boolean = false): StockfishProbeResult {
        if (!forceRefresh) {
            cachedProbeResult?.let { return it }
        }

        val bundledBinary = findNativeBinary(context)
        val oexList = queryOexProviders(context)

        val result = if (bundledBinary != null) {
            val (probedName, execError) = quickIdentifyUciName(bundledBinary)
            if (!probedName.isNullOrBlank()) {
                detectedEngineName = probedName
                val nnueReady = hasBundledOrExtractedNnue(context)
                StockfishProbeResult(
                    backend = ActiveEngineBackend.NATIVE_STOCKFISH,
                    engineName = probedName,
                    isNativeStockfishReady = true,
                    nativeBinaryPath = bundledBinary.absolutePath,
                    oexEngines = oexList,
                    diagnosticMessageAr = if (nnueReady) {
                        "محرك ستوك فيش الأصلي متصل وجاهز ($probedName • NNUE)"
                    } else {
                        "محرك ستوك فيش الأصلي متصل وجاهز ($probedName)"
                    }
                )
            } else {
                StockfishProbeResult(
                    backend = ActiveEngineBackend.BUILTIN_FALLBACK,
                    engineName = "محرك رُقعة المدمج (TT + PVS + LMR)",
                    isNativeStockfishReady = false,
                    nativeBinaryPath = null,
                    oexEngines = oexList,
                    diagnosticMessageAr = "تم العثور على ${bundledBinary.name} لكن تعذّر تشغيله (${execError ?: "خطأ UCI"})؛ يعمل المحرك المدمج احتياطياً."
                )
            }
        } else if (oexList.isNotEmpty()) {
            val firstOex = oexList.first()
            StockfishProbeResult(
                backend = ActiveEngineBackend.OEX_STOCKFISH,
                engineName = firstOex.label,
                isNativeStockfishReady = false,
                nativeBinaryPath = null,
                oexEngines = oexList,
                diagnosticMessageAr = "تم رصد محرك خارجي (${firstOex.label}). يعمل المحرك المدمج مع جاهزية OEX."
            )
        } else {
            StockfishProbeResult(
                backend = ActiveEngineBackend.BUILTIN_FALLBACK,
                engineName = "محرك رُقعة المدمج (TT + PVS + LMR)",
                isNativeStockfishReady = false,
                nativeBinaryPath = null,
                oexEngines = emptyList(),
                diagnosticMessageAr = "لم يتم العثور على libstockfish.so؛ يعمل المحرك المدمج المتطور تلقائياً."
            )
        }

        cachedProbeResult = result
        return result
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

    private fun quickIdentifyUciName(binary: File): Pair<String?, String?> {
        var proc: Process? = null
        return try {
            proc = ProcessBuilder(binary.absolutePath)
                .directory(binary.parentFile)
                .redirectErrorStream(true)
                .start()
            val writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            writer.write("uci\n")
            writer.flush()
            var engineName: String? = null
            var uciOk = false
            val deadline = System.currentTimeMillis() + 3000L
            while (System.currentTimeMillis() < deadline) {
                if (reader.ready()) {
                    val line = reader.readLine() ?: break
                    if (line.startsWith("id name ")) {
                        engineName = line.removePrefix("id name ").trim()
                    } else if (line.trim() == "uciok") {
                        uciOk = true
                        break
                    }
                } else {
                    val exited = runCatching { proc.exitValue(); true }.getOrDefault(false)
                    if (exited) break
                    Thread.sleep(10)
                }
            }
            runCatching {
                writer.write("quit\n")
                writer.flush()
            }
            runCatching { writer.close() }
            runCatching { reader.close() }
            runCatching { proc.waitFor(250, java.util.concurrent.TimeUnit.MILLISECONDS) }
            runCatching { proc.destroyForcibly() }
            if (uciOk && !engineName.isNullOrBlank()) {
                engineName to null
            } else {
                (engineName?.takeIf { uciOk }) to "لم يستجب المحرك لـ uciok"
            }
        } catch (t: Throwable) {
            runCatching { proc?.destroyForcibly() }
            null to (t.message ?: t.javaClass.simpleName)
        }
    }

    private fun hasBundledOrExtractedNnue(context: Context): Boolean {
        val target = File(context.filesDir, EXPECTED_DEFAULT_NNUE_NAME)
        if (target.exists() && target.length() == EXPECTED_DEFAULT_NNUE_BYTES) return true
        return runCatching {
            context.assets.list("")?.any { it.endsWith(".nnue") } == true
        }.getOrDefault(false)
    }

    private fun ensureBundledNnueCopied(context: Context): String? = synchronized(nnueCopyLock) {
        return runCatching {
            val nnueAsset = context.assets.list("")?.firstOrNull { it.endsWith(".nnue") }
                ?: return@runCatching null
            val target = File(context.filesDir, nnueAsset)
            val expectedLength = runCatching {
                context.assets.openFd(nnueAsset).use { it.length }
            }.getOrNull() ?: if (nnueAsset == EXPECTED_DEFAULT_NNUE_NAME) EXPECTED_DEFAULT_NNUE_BYTES else null

            val isAlreadyValid = target.exists() && (
                (expectedLength != null && expectedLength > 0L && target.length() == expectedLength) ||
                    (expectedLength == null && target.length() > 50_000_000L)
                )
            if (isAlreadyValid) {
                return@runCatching target.absolutePath
            }

            val tmp = File(context.filesDir, "$nnueAsset.tmp")
            if (tmp.exists()) tmp.delete()
            context.assets.open(nnueAsset).use { input ->
                tmp.outputStream().use { out ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
            }
            if (target.exists()) target.delete()
            if (tmp.renameTo(target)) target.absolutePath else null
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
        threads: Int,
        hashMb: Int = 32,
        powerSavingMode: Boolean = false,
        personalityIntensity: Int = 50,
        personalityRisk: PersonalityRisk = PersonalityRisk.BALANCED,
        sessionId: String = "match",
        remainingClockMs: Long? = null,
        gameSeed: Long = 0x52757161L,
        lastMoveFromWing: Int? = null,
        lastMovedPieceType: PieceType? = null
    ): ChessEngine.SearchResult = withContext(Dispatchers.Default) {
        val legal = state.generateLegalMoves()
        if (legal.size == 1) {
            val single = legal[0]
            return@withContext ChessEngine.SearchResult(
                bestMove = single,
                evalWhiteCp = ChessEngine.evaluateWhitePerspective(state.copyWithMoveFast(single), 1.0f),
                depthReached = 1,
                nodesSearched = 1,
                pvUci = listOf(single.toUci()),
                sourceName = "نقلة وحيدة قانونية"
            )
        }

        val effectiveMoveTimeMs = ChessEngine.computeMoveTimeWithClockMs(targetElo, remainingClockMs)
            .coerceAtMost(max(80, moveTimeMs))

        // Under 1320 Elo probabilistic heuristic split applies consistently
        if (targetElo in 1 until 1320) {
            val pStockfish = ChessEngine.computeStockfishProbability(targetElo)
            if (ThreadLocalRandom.current().nextDouble() > pStockfish) {
                return@withContext ChessEngine.chooseHeuristicMove(
                    state = state,
                    elo = targetElo,
                    personality = personality,
                    intensity = personalityIntensity
                )
            }
        }

        val rawCapMs = if (powerSavingMode) min(420, effectiveMoveTimeMs) else effectiveMoveTimeMs
        if (backendPreference == EngineBackendPreference.STOCKFISH_AUTO && context != null) {
            val nativeBinary = findNativeBinary(context)
            if (nativeBinary != null) {
                val multiPvCount = when {
                    personality != EnginePersonality.CLASSIC && personalityIntensity > 0 -> 6
                    humanStyle && targetElo > 0 -> 4
                    else -> 1
                }
                val candidates = queryNativeMultiPv(
                    context = context,
                    binaryPath = nativeBinary.absolutePath,
                    state = state,
                    targetElo = targetElo,
                    multiPv = multiPvCount,
                    moveTimeMs = rawCapMs,
                    threads = if (powerSavingMode) 1 else threads,
                    hashMb = hashMb,
                    nodeCap = ChessEngine.computeNodeCap(targetElo),
                    sessionId = sessionId
                )
                if (!candidates.isNullOrEmpty()) {
                    return@withContext ChessEngine.selectFromCandidates(
                        state = state,
                        candidates = candidates,
                        targetElo = targetElo,
                        personality = personality,
                        humanStyle = humanStyle,
                        personalityIntensity = personalityIntensity,
                        personalityRisk = personalityRisk,
                        gameSeed = gameSeed,
                        lastMoveFromWing = lastMoveFromWing,
                        lastMovedPieceType = lastMovedPieceType,
                        sourceLabel = detectedEngineName ?: "Stockfish 19 UCI"
                    )
                }
            }
        }

        ChessEngine.chooseCustomEngineMove(
            state = state,
            targetElo = targetElo,
            personality = personality,
            humanStyle = humanStyle,
            moveTimeMs = effectiveMoveTimeMs,
            personalityIntensity = personalityIntensity,
            personalityRisk = personalityRisk,
            gameSeed = gameSeed,
            lastMoveFromWing = lastMoveFromWing,
            lastMovedPieceType = lastMovedPieceType
        )
    }

    suspend fun computeBotMove(
        context: Context?,
        state: ChessBoardState,
        backendPreference: EngineBackendPreference,
        bot: BotPersona,
        adaptivePlayerElo: Int?,
        moveTimeMs: Int,
        threads: Int,
        sessionId: String = "bot_${bot.id}"
    ): ChessEngine.SearchResult = withContext(Dispatchers.Default) {
        val elo = adaptivePlayerElo ?: bot.elo
        if (backendPreference == EngineBackendPreference.STOCKFISH_AUTO && context != null) {
            val nativeBinary = findNativeBinary(context)
            if (nativeBinary != null) {
                val mappedPersonality = when {
                    bot.aggressionWeight >= 1.25f -> EnginePersonality.SACRIFICE_KING
                    bot.aggressionWeight <= 0.90f -> EnginePersonality.IRON_FORTRESS
                    else -> EnginePersonality.CLASSIC
                }
                val candidates = queryNativeMultiPv(
                    context = context,
                    binaryPath = nativeBinary.absolutePath,
                    state = state,
                    targetElo = elo,
                    multiPv = 4,
                    moveTimeMs = moveTimeMs,
                    threads = threads,
                    nodeCap = ChessEngine.computeNodeCap(elo),
                    sessionId = sessionId
                )
                if (!candidates.isNullOrEmpty()) {
                    return@withContext ChessEngine.selectFromCandidates(
                        state = state,
                        candidates = candidates,
                        targetElo = elo,
                        personality = mappedPersonality,
                        humanStyle = true,
                        personalityIntensity = 50,
                        personalityRisk = PersonalityRisk.BALANCED,
                        sourceLabel = detectedEngineName ?: "Stockfish 19 UCI"
                    )
                }
            }
        }
        ChessEngine.chooseBotMove(state, bot, adaptivePlayerElo)
    }

    suspend fun queryHintMove(
        context: Context?,
        state: ChessBoardState,
        backendPreference: EngineBackendPreference,
        moveTimeMs: Int = 320,
        threads: Int = 2
    ): ChessEngine.SearchResult = withContext(Dispatchers.Default) {
        if (backendPreference == EngineBackendPreference.STOCKFISH_AUTO && context != null) {
            val nativeBinary = findNativeBinary(context)
            if (nativeBinary != null) {
                val candidates = queryNativeMultiPv(
                    context = context,
                    binaryPath = nativeBinary.absolutePath,
                    state = state,
                    targetElo = -1,
                    multiPv = 1,
                    moveTimeMs = moveTimeMs,
                    threads = threads,
                    nodeCap = 60_000,
                    maxDepth = 12,
                    sessionId = "hint"
                )
                val first = candidates?.firstOrNull()
                if (first != null) {
                    return@withContext ChessEngine.SearchResult(
                        bestMove = first.move,
                        evalWhiteCp = first.evalWhiteCp,
                        depthReached = first.depth,
                        nodesSearched = first.nodes,
                        pvUci = first.pvUci,
                        sourceName = detectedEngineName ?: "Stockfish Hint"
                    )
                }
            }
        }
        ChessEngine.evaluatePositionBestMove(state, depth = 4, timeLimitMs = moveTimeMs.toLong())
    }

    /**
     * MultiPV-3 Analysis Explorer per Ruqaa Section 8.1 (1,700ms, unlimited depth/nodes, 3 lines).
     */
    suspend fun analyzeMultiPv(
        context: Context?,
        state: ChessBoardState,
        backendPreference: EngineBackendPreference,
        multiPv: Int = 3,
        moveTimeMs: Int = 1700,
        threads: Int = 2,
        hashMb: Int = 32,
        powerSavingMode: Boolean = false,
        sessionId: String = "analysis"
    ): List<MultiPvLine> = withContext(Dispatchers.Default) {
        val legal = state.generateLegalMoves()
        if (legal.isEmpty()) return@withContext emptyList()
        val effMs = if (powerSavingMode) min(650, moveTimeMs) else moveTimeMs
        val effThreads = if (powerSavingMode) 1 else threads

        if (backendPreference == EngineBackendPreference.STOCKFISH_AUTO && context != null) {
            val nativeBinary = findNativeBinary(context)
            if (nativeBinary != null) {
                val candidates = queryNativeMultiPv(
                    context = context,
                    binaryPath = nativeBinary.absolutePath,
                    state = state,
                    targetElo = -1,
                    multiPv = multiPv.coerceIn(1, 6),
                    moveTimeMs = effMs,
                    threads = effThreads,
                    hashMb = hashMb,
                    nodeCap = null,
                    sessionId = sessionId
                )
                if (!candidates.isNullOrEmpty()) {
                    return@withContext candidates.take(multiPv).mapIndexed { index, c ->
                        val san = state.toSan(c.move, legal)
                        val pvSan = ChessEngine.convertPvUciToSan(state, c.pvUci)
                        MultiPvLine(
                            rank = index + 1,
                            move = c.move,
                            san = san,
                            evalWhiteCp = c.evalWhiteCp,
                            mateIn = c.mateIn,
                            depth = c.depth,
                            pvSan = pvSan,
                            pvUci = c.pvUci,
                            nodes = c.nodes.toLong()
                        )
                    }
                }
            }
        }

        ChessEngine.evaluateMultiPv(state, multiPv = multiPv, timeLimitMs = moveTimeMs.toLong(), maxDepth = 5)
    }

    private suspend fun queryNativeMultiPv(
        context: Context,
        binaryPath: String,
        state: ChessBoardState,
        targetElo: Int,
        multiPv: Int,
        moveTimeMs: Int,
        threads: Int,
        hashMb: Int = 32,
        nodeCap: Int? = null,
        maxDepth: Int? = null,
        sessionId: String = "default"
    ): List<ChessEngine.CandidateMoveEval>? = withContext(Dispatchers.IO) {
        uciMutex.withLock {
            val legal = state.generateLegalMoves()
            if (legal.isEmpty()) return@withLock null

            val useSyzygy = !syzygyDisabledForSession && SyzygyManager.hasWdlTables(context)
            val result = runNativeUciQuery(
                context = context,
                binaryPath = binaryPath,
                state = state,
                legal = legal,
                targetElo = targetElo,
                multiPv = multiPv,
                moveTimeMs = moveTimeMs,
                threads = threads,
                hashMb = hashMb,
                nodeCap = nodeCap,
                maxDepth = maxDepth,
                sessionId = sessionId,
                enableSyzygy = useSyzygy
            )
            if (result == null && useSyzygy) {
                syzygyDisabledForSession = true
                closeSession()
                return@withLock runNativeUciQuery(
                    context = context,
                    binaryPath = binaryPath,
                    state = state,
                    legal = legal,
                    targetElo = targetElo,
                    multiPv = multiPv,
                    moveTimeMs = moveTimeMs,
                    threads = threads,
                    hashMb = hashMb,
                    nodeCap = nodeCap,
                    maxDepth = maxDepth,
                    sessionId = sessionId,
                    enableSyzygy = false
                )
            }
            return@withLock result
        }
    }

    private fun runNativeUciQuery(
        context: Context,
        binaryPath: String,
        state: ChessBoardState,
        legal: List<ChessMove>,
        targetElo: Int,
        multiPv: Int,
        moveTimeMs: Int,
        threads: Int,
        hashMb: Int,
        nodeCap: Int?,
        maxDepth: Int?,
        sessionId: String,
        enableSyzygy: Boolean
    ): List<ChessEngine.CandidateMoveEval>? {
        return runCatching {
            ensureSession(context, binaryPath, threads, hashMb, enableSyzygy)
            val writer = activeWriter ?: return@runCatching null
            val reader = activeReader ?: return@runCatching null

            val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            val safeThreads = threads.coerceIn(1, min(4, cpuCores))
            val safeHash = hashMb.coerceIn(16, 256)
            if (safeThreads != lastConfiguredThreads) {
                sendUciLine(writer, "setoption name Threads value $safeThreads")
                lastConfiguredThreads = safeThreads
            }
            if (safeHash != lastConfiguredHashMb) {
                sendUciLine(writer, "setoption name Hash value $safeHash")
                lastConfiguredHashMb = safeHash
            }

            if (sessionId != lastSessionId) {
                sendUciLine(writer, "ucinewgame")
                lastSessionId = sessionId
            }
            if (lastConfiguredElo != targetElo) {
                sendUciLine(writer, "setoption name Clear Hash")
                lastConfiguredElo = targetElo
            }

            val limited = targetElo > 0
            sendUciLine(writer, "setoption name Skill Level value 20")
            sendUciLine(writer, "setoption name UCI_LimitStrength value ${if (limited) "true" else "false"}")
            if (limited) {
                sendUciLine(writer, "setoption name UCI_Elo value ${targetElo.coerceIn(1320, 3190)}")
            }
            val safeMultiPv = multiPv.coerceIn(1, min(6, legal.size))
            sendUciLine(writer, "setoption name MultiPV value $safeMultiPv")
            sendUciLine(writer, "isready")
            check(waitForToken(reader, "readyok", 20_000L)) { "Stockfish did not answer readyok" }

            sendUciLine(writer, "position fen ${state.toFen()}")
            val safeMs = moveTimeMs.coerceIn(80, 5000)
            val goCmd = buildString {
                append("go movetime ").append(safeMs)
                if (maxDepth != null) append(" depth ").append(maxDepth)
                if (nodeCap != null) append(" nodes ").append(nodeCap)
            }
            sendUciLine(writer, goCmd)

            // Group completed MultiPV lines by depth (Ruqaa Section 5.4)
            val byDepth = linkedMapOf<Int, MutableMap<Int, ParsedUciInfo>>()
            var bestMoveStr: String? = null
            val waitDeadline = System.currentTimeMillis() + max(20_000L, safeMs + 7_000L)

            while (System.currentTimeMillis() < waitDeadline) {
                val line = reader.readLine() ?: break
                if (line.startsWith("info ")) {
                    parseUciInfoLine(line)?.let { parsed ->
                        val depthMap = byDepth.getOrPut(parsed.depth) { mutableMapOf() }
                        depthMap[parsed.multipv] = parsed
                    }
                } else if (line.startsWith("bestmove ")) {
                    val tokens = line.trim().split(Regex("\\s+"))
                    bestMoveStr = tokens.getOrNull(1)
                    break
                }
            }

            if (bestMoveStr == null) {
                throw IllegalStateException("Stockfish terminated without bestmove")
            }

            // Pick deepest depth that collected at least as many PV lines or highest depth overall
            val deepestCompleted = byDepth.entries
                .filter { it.value.size >= safeMultiPv }
                .maxByOrNull { it.key }
                ?: byDepth.maxByOrNull { it.key }

            val candidates = mutableListOf<ChessEngine.CandidateMoveEval>()
            deepestCompleted?.value?.entries?.sortedBy { it.key }?.forEach { (_, info) ->
                val pvMoveUci = info.pv.firstOrNull() ?: return@forEach
                val matched = legal.find { it.toUci().equals(pvMoveUci, ignoreCase = true) } ?: return@forEach
                val evalWhite = if (state.turn == PieceColor.WHITE) info.scoreCp else -info.scoreCp
                val mateWhite = info.mateIn?.let { if (state.turn == PieceColor.WHITE) it else -it }
                candidates.add(
                    ChessEngine.CandidateMoveEval(
                        move = matched,
                        scoreSideCp = info.scoreCp,
                        evalWhiteCp = evalWhite,
                        mateIn = mateWhite,
                        pvUci = info.pv,
                        depth = info.depth,
                        nodes = info.nodes
                    )
                )
            }

            if (candidates.isEmpty()) {
                val matched = legal.find { it.toUci().equals(bestMoveStr, ignoreCase = true) }
                if (matched != null) {
                    candidates.add(
                        ChessEngine.CandidateMoveEval(
                            move = matched,
                            scoreSideCp = 0,
                            evalWhiteCp = ChessEngine.evaluateWhitePerspective(state.copyWithMoveFast(matched), 1.0f),
                            pvUci = listOf(matched.toUci()),
                            depth = 1,
                            nodes = 0
                        )
                    )
                }
            }

            if (candidates.isEmpty()) null else candidates
        }.onFailure {
            closeSession()
        }.getOrNull()
    }

    private fun ensureSession(
        context: Context,
        binaryPath: String,
        threads: Int,
        hashMb: Int,
        enableSyzygy: Boolean
    ) {
        if (activeProcess != null && activeBinaryPath == binaryPath) {
            val alive = runCatching { activeProcess?.exitValue(); false }.getOrDefault(true)
            if (alive) return
        }
        closeSession()
        val file = File(binaryPath)
        val nnuePath = ensureBundledNnueCopied(context)
        val proc = ProcessBuilder(file.absolutePath)
            .directory(context.filesDir)
            .redirectErrorStream(true)
            .start()
        val writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
        val reader = BufferedReader(InputStreamReader(proc.inputStream))

        sendUciLine(writer, "uci")
        var uciOk = false
        val deadline = System.currentTimeMillis() + 45_000L
        while (System.currentTimeMillis() < deadline) {
            val line = reader.readLine() ?: break
            if (line.startsWith("id name ")) {
                detectedEngineName = line.removePrefix("id name ").trim()
            } else if (line.trim() == "uciok") {
                uciOk = true
                break
            }
        }
        if (!uciOk) {
            runCatching { proc.destroyForcibly() }
            throw IllegalStateException("Stockfish uci handshake timed out")
        }

        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val boundedThreads = max(1, min(4, min(cpuCores, threads.coerceAtLeast(1))))
        val boundedHash = hashMb.coerceIn(16, 256)
        sendUciLine(writer, "setoption name Threads value $boundedThreads")
        sendUciLine(writer, "setoption name Hash value $boundedHash")
        lastConfiguredThreads = boundedThreads
        lastConfiguredHashMb = boundedHash

        if (nnuePath != null) {
            sendUciLine(writer, "setoption name EvalFile value $nnuePath")
        }

        if (enableSyzygy && SyzygyManager.hasWdlTables(context)) {
            val syzPath = SyzygyManager.syzygyDir(context).absolutePath
            sendUciLine(writer, "setoption name SyzygyPath value $syzPath")
            sendUciLine(writer, "setoption name Syzygy50MoveRule value true")
        }

        sendUciLine(writer, "isready")
        if (!waitForToken(reader, "readyok", 20_000L)) {
            runCatching { proc.destroyForcibly() }
            throw IllegalStateException("Stockfish readyok timed out")
        }

        activeProcess = proc
        activeWriter = writer
        activeReader = reader
        activeBinaryPath = binaryPath
        lastSessionId = null
        lastConfiguredElo = null
    }

    private fun sendUciLine(writer: BufferedWriter, cmd: String) {
        writer.write(cmd)
        writer.newLine()
        writer.flush()
    }

    private fun waitForToken(reader: BufferedReader, expected: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val line = reader.readLine() ?: return false
            if (line.trim() == expected) return true
        }
        return false
    }

    fun closeSession() {
        runCatching {
            activeWriter?.write("quit\n")
            activeWriter?.flush()
        }
        runCatching { activeWriter?.close() }
        runCatching { activeReader?.close() }
        runCatching { activeProcess?.destroyForcibly() }
        activeProcess = null
        activeWriter = null
        activeReader = null
        activeBinaryPath = null
        lastSessionId = null
        lastConfiguredElo = null
    }

    data class ParsedUciInfo(
        val depth: Int,
        val multipv: Int,
        val scoreCp: Int,
        val mateIn: Int?,
        val nodes: Int,
        val tbHits: Long,
        val pv: List<String>
    )

    fun parseUciInfoLine(line: String): ParsedUciInfo? {
        if (!line.startsWith("info ") || !line.contains(" score ") || !line.contains(" pv ")) return null
        if (line.contains(" lowerbound") || line.contains(" upperbound")) return null

        val tokens = line.trim().split(Regex("\\s+"))
        var depth = 1
        var multipv = 1
        var scoreCp = 0
        var mateIn: Int? = null
        var nodes = 0
        var tbHits = 0L
        val pv = mutableListOf<String>()

        var i = 1
        while (i < tokens.size) {
            when (tokens[i]) {
                "depth" -> {
                    depth = tokens.getOrNull(i + 1)?.toIntOrNull() ?: depth
                    i += 2
                }
                "multipv" -> {
                    multipv = tokens.getOrNull(i + 1)?.toIntOrNull() ?: multipv
                    i += 2
                }
                "nodes" -> {
                    nodes = tokens.getOrNull(i + 1)?.toIntOrNull() ?: nodes
                    i += 2
                }
                "tbhits" -> {
                    tbHits = tokens.getOrNull(i + 1)?.toLongOrNull() ?: tbHits
                    i += 2
                }
                "score" -> {
                    val kind = tokens.getOrNull(i + 1)
                    val value = tokens.getOrNull(i + 2)?.toIntOrNull() ?: 0
                    when (kind) {
                        "cp" -> scoreCp = value.coerceIn(-10_000, 10_000)
                        "mate" -> {
                            mateIn = value
                            scoreCp = if (value >= 0) 32_000 - abs(value) else -(32_000 - abs(value))
                        }
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
        if (pv.isEmpty()) return null
        return ParsedUciInfo(depth, multipv, scoreCp, mateIn, nodes, tbHits, pv)
    }
}
