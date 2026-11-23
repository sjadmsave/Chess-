package com.example.data

import com.example.chess.BotPersona
import com.example.chess.ChessBoardState
import com.example.chess.EnginePersonality
import com.example.chess.GameMode
import com.example.chess.GameResult
import com.example.chess.MoveQuality
import com.example.chess.PieceColor
import com.example.chess.PostGameReport
import com.example.chess.RecordedMove
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.roundToInt

fun formatOpponentTitle(botId: String): String {
    if (botId == "local_2p") return "لاعبان محليان (جهاز واحد)"
    if (botId.startsWith("custom:")) {
        val parts = botId.split(":")
        val personality = parts.getOrNull(1)?.let { EnginePersonality.byId(it) } ?: EnginePersonality.CLASSIC
        val elo = parts.getOrNull(2)?.toIntOrNull() ?: -1
        val eloText = if (elo <= 0) "بلا حد (أقصى قوة)" else "نحو $elo إيلو"
        return "ضد المحرك • $eloText • ${personality.nameAr}"
    }
    val bot = BotPersona.byId(botId)
    return "${bot.nameAr} • ${bot.elo} ELO"
}

enum class ReportPeriod(val labelAr: String, val daysBack: Int?) {
    WEEKLY("أسبوعي", 7),
    MONTHLY("شهري", 30),
    ALL_TIME("الشامل", null)
}

data class PeriodicReport(
    val period: ReportPeriod,
    val totalGames: Int,
    val wins: Int,
    val avgAccuracy: Int,
    val avgOpeningAcc: Int,
    val avgMiddlegameAcc: Int,
    val avgEndgameAcc: Int,
    val totalBlunders: Int,
    val totalMistakes: Int,
    val weakestPhaseAr: String,
    val strongestPhaseAr: String,
    val diagnosisSummaryAr: String,
    val nextActionAr: String
)

data class WdlRecord(
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0
) {
    val total: Int get() = wins + draws + losses
}

data class RuqaaEngineStatsSummary(
    val totalEngineGames: Int = 0,
    val unassistedCount: Int = 0,
    val assistedCount: Int = 0,
    val customPositionCount: Int = 0,
    val strengthMixedCount: Int = 0,
    val byStrength: Map<String, WdlRecord> = emptyMap(),
    val byPersonality: Map<String, WdlRecord> = emptyMap()
)

class ChessRepository(
    private val db: ChessDatabase,
    val prefs: UserPreferencesRepository
) {
    private val savedGameDao = db.savedGameDao()
    private val ratingPointDao = db.ratingPointDao()
    private val puzzleRecordDao = db.puzzleRecordDao()
    private val achievementDao = db.achievementDao()

    val ongoingGame: Flow<SavedGameEntity?> = savedGameDao.observeLatestOngoingGame()
    val completedGames: Flow<List<SavedGameEntity>> = savedGameDao.observeCompletedGames()
    val allGames: Flow<List<SavedGameEntity>> = savedGameDao.observeAllGames()
    val ratingHistory: Flow<List<RatingPointEntity>> = ratingPointDao.observeAll()
    val puzzleRecords: Flow<List<PuzzleRecordEntity>> = puzzleRecordDao.observeAll()
    val achievements: Flow<List<AchievementEntity>> = achievementDao.observeAll()

    suspend fun saveOngoingGame(
        existingId: Long?,
        botId: String,
        playerColor: PieceColor,
        currentFen: String,
        moves: List<RecordedMove>,
        openingNameAr: String,
        gameMode: String = GameMode.VS_ENGINE.id,
        startFen: String = ChessBoardState.INITIAL_FEN,
        timeControlId: String = "unlimited",
        whiteTimeSec: Long = 0L,
        blackTimeSec: Long = 0L,
        hintsUsed: Int = 0,
        assisted: Boolean = false,
        strengthMixed: Boolean = false,
        edited: Boolean = false,
        flipped: Boolean = false
    ): Long {
        val entity = SavedGameEntity(
            id = existingId ?: 0L,
            botId = botId,
            playerColor = playerColor.name,
            currentFen = currentFen,
            movesEncoded = MoveCodec.encode(moves),
            moveCount = moves.size,
            resultName = GameResult.ONGOING.name,
            isCompleted = false,
            playerAccuracy = 0,
            opponentAccuracy = 0,
            acpl = 0,
            openingNameAr = openingNameAr,
            openingAccuracy = 0,
            middlegameAccuracy = 0,
            endgameAccuracy = 0,
            eloDelta = 0,
            updatedAt = System.currentTimeMillis(),
            gameMode = gameMode,
            startFen = startFen,
            timeControlId = timeControlId,
            whiteTimeSec = whiteTimeSec,
            blackTimeSec = blackTimeSec,
            hintsUsed = hintsUsed,
            assisted = assisted,
            strengthMixed = strengthMixed,
            edited = edited,
            flipped = flipped
        )
        return savedGameDao.upsert(entity)
    }

    suspend fun deleteGame(id: Long) {
        savedGameDao.deleteById(id)
    }

    suspend fun getGameById(id: Long): SavedGameEntity? = savedGameDao.getById(id)

    suspend fun finalizeCompletedGame(
        existingId: Long?,
        bot: BotPersona,
        botIdOverride: String? = null,
        playerColor: PieceColor,
        currentFen: String,
        moves: List<RecordedMove>,
        result: GameResult,
        report: PostGameReport,
        currentElo: Int,
        solvedPuzzleCount: Int,
        gameMode: String = GameMode.VS_ENGINE.id,
        startFen: String = ChessBoardState.INITIAL_FEN,
        timeControlId: String = "unlimited",
        whiteTimeSec: Long = 0L,
        blackTimeSec: Long = 0L,
        hintsUsed: Int = 0,
        assisted: Boolean = false,
        strengthMixed: Boolean = false,
        edited: Boolean = false,
        flipped: Boolean = false,
        reviewCacheKey: String = "",
        reviewCompleted: Boolean = false
    ): List<AchievementDef> {
        val now = System.currentTimeMillis()
        val effectiveBotId = botIdOverride ?: bot.id
        val gameEntity = SavedGameEntity(
            id = existingId ?: 0L,
            botId = effectiveBotId,
            playerColor = playerColor.name,
            currentFen = currentFen,
            movesEncoded = MoveCodec.encode(moves),
            moveCount = moves.size,
            resultName = result.name,
            isCompleted = true,
            playerAccuracy = report.playerAccuracy,
            opponentAccuracy = report.opponentAccuracy,
            acpl = report.acpl,
            openingNameAr = report.openingNameAr,
            openingAccuracy = report.openingAccuracy,
            middlegameAccuracy = report.middlegameAccuracy,
            endgameAccuracy = report.endgameAccuracy,
            eloDelta = if (gameMode == GameMode.LOCAL_TWO_PLAYER.id) 0 else report.eloDelta,
            updatedAt = now,
            gameMode = gameMode,
            startFen = startFen,
            timeControlId = timeControlId,
            whiteTimeSec = whiteTimeSec,
            blackTimeSec = blackTimeSec,
            hintsUsed = hintsUsed,
            assisted = assisted,
            strengthMixed = strengthMixed,
            edited = edited,
            flipped = flipped,
            reviewCacheKey = reviewCacheKey,
            reviewCompleted = reviewCompleted
        )
        savedGameDao.upsert(gameEntity)

        if (gameMode == GameMode.LOCAL_TWO_PLAYER.id) {
            return emptyList()
        }

        val won = didPlayerWin(result, playerColor)
        val newElo = (currentElo + report.eloDelta).coerceIn(400, 2800)
        val xpGain = (if (won) 45 else 20) + (report.playerAccuracy / 4)
        prefs.updateRatingAndXp(newElo, xpGain)

        val blunders = report.qualityCounts[MoveQuality.BLUNDER] ?: 0
        val mistakes = report.qualityCounts[MoveQuality.MISTAKE] ?: 0
        ratingPointDao.insert(
            RatingPointEntity(
                timestamp = now,
                elo = newElo,
                accuracy = report.playerAccuracy,
                openingAcc = report.openingAccuracy,
                middlegameAcc = report.middlegameAccuracy,
                endgameAcc = report.endgameAccuracy,
                blunders = blunders,
                mistakes = mistakes,
                won = won,
                botId = effectiveBotId
            )
        )

        val unlockedNow = mutableListOf<AchievementDef>()
        suspend fun unlockIfEligible(id: String, condition: Boolean) {
            if (!condition) return
            achievementDao.insert(AchievementEntity(id = id, unlockedAt = now))
            AchievementCatalog.all.find { it.id == id }?.let { unlockedNow.add(it) }
        }

        val playerMoveCount = moves.count { it.color == playerColor }
        unlockIfEligible("first_win", won)
        unlockIfEligible("clean_game", playerMoveCount >= 10 && blunders == 0)
        unlockIfEligible("sharp_accuracy", playerMoveCount >= 8 && report.playerAccuracy >= 88)
        unlockIfEligible("beat_tal", won && bot == BotPersona.TAL)
        unlockIfEligible("beat_polgar", won && bot == BotPersona.POLGAR)
        unlockIfEligible("beat_kasparov", won && bot == BotPersona.KASPAROV)
        unlockIfEligible("beat_anand", won && bot == BotPersona.ANAND)
        unlockIfEligible("beat_carlsen", won && bot == BotPersona.CARLSEN)
        unlockIfEligible("strategist_1200", newElo >= 1200)
        unlockIfEligible("puzzle_hunter", solvedPuzzleCount >= 5)

        return unlockedNow
    }

    suspend fun updateCompletedReview(
        gameId: Long,
        moves: List<RecordedMove>,
        report: PostGameReport,
        cacheKey: String,
        completed: Boolean
    ) {
        val existing = savedGameDao.getById(gameId) ?: return
        savedGameDao.upsert(
            existing.copy(
                movesEncoded = MoveCodec.encode(moves),
                playerAccuracy = report.playerAccuracy,
                opponentAccuracy = report.opponentAccuracy,
                acpl = report.acpl,
                openingAccuracy = report.openingAccuracy,
                middlegameAccuracy = report.middlegameAccuracy,
                endgameAccuracy = report.endgameAccuracy,
                reviewCacheKey = cacheKey,
                reviewCompleted = completed
            )
        )
    }

    /**
     * Ruqaa Section 10.3: Counts only finished engine games, supports filtering out
     * assisted/hintsUsed, custom/edited positions, and strengthMixed games.
     */
    fun buildEngineStatsSummary(
        games: List<SavedGameEntity>,
        unassistedOnly: Boolean
    ): RuqaaEngineStatsSummary {
        val finishedEngine = games.filter {
            it.isCompleted && it.gameMode != GameMode.LOCAL_TWO_PLAYER.id && it.botId != "local_2p"
        }
        val assistedCount = finishedEngine.count { it.assisted || it.hintsUsed > 0 }
        val customPosCount = finishedEngine.count { it.edited || it.startFen != ChessBoardState.INITIAL_FEN }
        val strengthMixedCount = finishedEngine.count { it.strengthMixed }
        val unassistedGames = finishedEngine.filter {
            !it.assisted && it.hintsUsed == 0 &&
                !it.edited && it.startFen == ChessBoardState.INITIAL_FEN &&
                !it.strengthMixed
        }

        val target = if (unassistedOnly) unassistedGames else finishedEngine
        val byStrength = linkedMapOf<String, WdlRecord>()
        val byPersonality = linkedMapOf<String, WdlRecord>()

        for (g in target) {
            val res = runCatching { GameResult.valueOf(g.resultName) }.getOrDefault(GameResult.ONGOING)
            val color = runCatching { PieceColor.valueOf(g.playerColor) }.getOrDefault(PieceColor.WHITE)
            val won = didPlayerWin(res, color)
            val draw = res.isDraw

            val strengthKey = when {
                g.strengthMixed -> "قوة متغيّرة"
                g.botId.startsWith("custom:") -> {
                    val elo = g.botId.split(":").getOrNull(2)?.toIntOrNull() ?: -1
                    if (elo <= 0) "أقصى (بلا حد)" else "$elo إيلو"
                }
                else -> {
                    val bot = BotPersona.byId(g.botId)
                    "${bot.elo} إيلو"
                }
            }
            val prevS = byStrength[strengthKey] ?: WdlRecord()
            byStrength[strengthKey] = WdlRecord(
                wins = prevS.wins + if (won) 1 else 0,
                draws = prevS.draws + if (draw) 1 else 0,
                losses = prevS.losses + if (!won && !draw) 1 else 0
            )

            val persKey = if (g.botId.startsWith("custom:")) {
                val pId = g.botId.split(":").getOrNull(1).orEmpty()
                EnginePersonality.byId(pId).nameAr
            } else {
                BotPersona.byId(g.botId).nameAr
            }
            val prevP = byPersonality[persKey] ?: WdlRecord()
            byPersonality[persKey] = WdlRecord(
                wins = prevP.wins + if (won) 1 else 0,
                draws = prevP.draws + if (draw) 1 else 0,
                losses = prevP.losses + if (!won && !draw) 1 else 0
            )
        }

        return RuqaaEngineStatsSummary(
            totalEngineGames = finishedEngine.size,
            unassistedCount = unassistedGames.size,
            assistedCount = assistedCount,
            customPositionCount = customPosCount,
            strengthMixedCount = strengthMixedCount,
            byStrength = byStrength,
            byPersonality = byPersonality
        )
    }

    private fun didPlayerWin(result: GameResult, playerColor: PieceColor): Boolean {
        return (result == GameResult.WHITE_WINS_CHECKMATE || result == GameResult.BLACK_RESIGNED || result == GameResult.BLACK_TIMEOUT) && playerColor == PieceColor.WHITE ||
            (result == GameResult.BLACK_WINS_CHECKMATE || result == GameResult.WHITE_RESIGNED || result == GameResult.WHITE_TIMEOUT) && playerColor == PieceColor.BLACK
    }

    suspend fun recordPuzzleSolved(
        puzzleId: String,
        usedHint: Boolean,
        attempts: Int,
        isDaily: Boolean,
        todayKey: String,
        totalSolvedSoFar: Int,
        currentStreak: Int
    ) {
        val now = System.currentTimeMillis()
        puzzleRecordDao.upsert(
            PuzzleRecordEntity(
                puzzleId = puzzleId,
                solvedAt = now,
                usedHint = usedHint,
                attempts = attempts
            )
        )
        if (isDaily) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yKey = String.format(
                java.util.Locale.US,
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
            prefs.markDailyPuzzleSolved(todayKey, yKey, bonusXp = 40)
            if (currentStreak + 1 >= 3) {
                achievementDao.insert(AchievementEntity("daily_streak_3", now))
            }
        } else {
            prefs.addXp(if (usedHint) 15 else 25)
        }
        if (totalSolvedSoFar + 1 >= 5) {
            achievementDao.insert(AchievementEntity("puzzle_hunter", now))
        }
    }

    fun buildPeriodicReport(points: List<RatingPointEntity>, period: ReportPeriod): PeriodicReport {
        val cutoff = period.daysBack?.let {
            System.currentTimeMillis() - it * 24L * 60L * 60L * 1000L
        } ?: 0L
        val filtered = points.filter { it.timestamp >= cutoff }
        if (filtered.isEmpty()) {
            return PeriodicReport(
                period = period,
                totalGames = 0,
                wins = 0,
                avgAccuracy = 0,
                avgOpeningAcc = 0,
                avgMiddlegameAcc = 0,
                avgEndgameAcc = 0,
                totalBlunders = 0,
                totalMistakes = 0,
                weakestPhaseAr = "غير محدد بعد",
                strongestPhaseAr = "غير محدد بعد",
                diagnosisSummaryAr = "العب مباراتك الأولى ليقوم المحرك بتحليل أسلوبك ورسم مسار تطورك.",
                nextActionAr = "ابدأ مباراة جديدة أو حلّ لغز اليوم لبناء ملفك الإحصائي."
            )
        }

        val total = filtered.size
        val wins = filtered.count { it.won }
        val avgAcc = filtered.map { it.accuracy }.average().roundToInt()
        val avgOpen = filtered.map { it.openingAcc }.average().roundToInt()
        val avgMid = filtered.map { it.middlegameAcc }.average().roundToInt()
        val avgEnd = filtered.map { it.endgameAcc }.average().roundToInt()
        val blunders = filtered.sumOf { it.blunders }
        val mistakes = filtered.sumOf { it.mistakes }

        val phases = listOf(
            "الافتتاح" to avgOpen,
            "وسط اللعب" to avgMid,
            "نهاية اللعب" to avgEnd
        )
        val strongest = phases.maxByOrNull { it.second }?.first ?: "الافتتاح"
        val weakest = phases.minByOrNull { it.second }?.first ?: "وسط اللعب"

        val blundersPerGame = blunders.toFloat() / total.toFloat()
        val diagnosis = when {
            blundersPerGame >= 1.5f ->
                "تشخيص الفترة (${period.labelAr}): نقطة الضعف الأبرز هي الأخطاء الكبيرة المفاجئة (${blunders} خطأً في $total مباريات)."
            avgOpen < avgMid && avgOpen < 75 ->
                "تشخيص الفترة (${period.labelAr}): مرحلة الافتتاح هي الأقل دقة ($avgOpen%)؛ تحتاج إلى تفعيل القطع الخفيفة والتبييت مبكراً."
            avgEnd < avgMid && avgEnd < 78 ->
                "تشخيص الفترة (${period.labelAr}): تتفوق في بداية ووسط المباراة، لكنك تفقد الأفضلية في النهايات ($avgEnd%)."
            else ->
                "تشخيص الفترة (${period.labelAr}): تطور متوازن بدقة $avgAcc% وقوة واضحة في مرحلة $strongest."
        }

        val action = when {
            blundersPerGame >= 1.5f ->
                "الحل المقترح: طبّق قاعدة فحص التهديدات قبل كل نقلة، وخصّص ٥ دقائق يومياً للألغاز التكتيكية."
            weakest == "الافتتاح" ->
                "الحل المقترح: سيطر على الوسط، أخرج الأحصنة والأفيال، وبيّت مبكراً."
            weakest == "نهاية اللعب" ->
                "الحل المقترح: تدرّب على ترقية البيادق الحرة وتفعيل الملك نحو الوسط."
            else ->
                "الحل المقترح: انتقل إلى مستوى قوة أعلى أو جرّب شخصيات محرك متنوعة."
        }

        return PeriodicReport(
            period = period,
            totalGames = total,
            wins = wins,
            avgAccuracy = avgAcc,
            avgOpeningAcc = avgOpen,
            avgMiddlegameAcc = avgMid,
            avgEndgameAcc = avgEnd,
            totalBlunders = blunders,
            totalMistakes = mistakes,
            weakestPhaseAr = weakest,
            strongestPhaseAr = strongest,
            diagnosisSummaryAr = diagnosis,
            nextActionAr = action
        )
    }
}
