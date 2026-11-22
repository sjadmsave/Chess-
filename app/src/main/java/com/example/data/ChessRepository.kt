package com.example.data

import com.example.chess.BotPersona
import com.example.chess.GameResult
import com.example.chess.MoveQuality
import com.example.chess.PieceColor
import com.example.chess.PostGameReport
import com.example.chess.RecordedMove
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import kotlin.math.roundToInt

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
    val ratingHistory: Flow<List<RatingPointEntity>> = ratingPointDao.observeAll()
    val puzzleRecords: Flow<List<PuzzleRecordEntity>> = puzzleRecordDao.observeAll()
    val achievements: Flow<List<AchievementEntity>> = achievementDao.observeAll()

    suspend fun saveOngoingGame(
        existingId: Long?,
        botId: String,
        playerColor: PieceColor,
        currentFen: String,
        moves: List<RecordedMove>,
        openingNameAr: String
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
            updatedAt = System.currentTimeMillis()
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
        playerColor: PieceColor,
        currentFen: String,
        moves: List<RecordedMove>,
        result: GameResult,
        report: PostGameReport,
        currentElo: Int,
        solvedPuzzleCount: Int
    ): List<AchievementDef> {
        val now = System.currentTimeMillis()
        val gameEntity = SavedGameEntity(
            id = existingId ?: 0L,
            botId = bot.id,
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
            eloDelta = report.eloDelta,
            updatedAt = now
        )
        savedGameDao.upsert(gameEntity)

        val won = (result == GameResult.WHITE_WINS_CHECKMATE || result == GameResult.BLACK_RESIGNED) && playerColor == PieceColor.WHITE ||
            (result == GameResult.BLACK_WINS_CHECKMATE || result == GameResult.WHITE_RESIGNED) && playerColor == PieceColor.BLACK

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
                botId = bot.id
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
                diagnosisSummaryAr = "العب مباراتك الأولى ليقوم المحرك الذكي بتحليل أسلوبك ورسم مسار تطورك.",
                nextActionAr = "ابدأ مباراة أمام بوبي فيشر أو حلّ لغز اليوم لبناء ملفك الإحصائي."
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
                "تشخيص الفترة (${period.labelAr}): نقطة الضعف الأبرز هي الأخطاء الفادحة المفاجئة (${blunders} خطأ فادح في $total مباريات)، غالباً بسبب ترك قطع غير محمية تحت الضغط."
            avgOpen < avgMid && avgOpen < 75 ->
                "تشخيص الفترة (${period.labelAr}): مرحلة الافتتاح هي الأقل دقة ($avgOpen%)؛ تحتاج إلى تفعيل القطع الخفيفة والتبييت بصورة أسرع."
            avgEnd < avgMid && avgEnd < 78 ->
                "تشخيص الفترة (${period.labelAr}): تتفوق في بداية ووسط المباراة، لكنك تفقد الأفضلية في النهايات ($avgEnd%)."
            else ->
                "تشخيص الفترة (${period.labelAr}): تطور متوازن بدقة $avgAcc% وقوة واضحة في مرحلة $strongest."
        }

        val action = when {
            blundersPerGame >= 1.5f ->
                "الحل المقترح: طبّق قاعدة (فحص التهديدات) قبل كل نقلة، وخصّص 5 دقائق يومياً لألغاز التكتيك."
            weakest == "الافتتاح" ->
                "الحل المقترح: سيطر على e4/d4، أخرج الأحصنة قبل الفيلة، وبيّت مبكراً قبل النقلة الثامنة."
            weakest == "نهاية اللعب" ->
                "الحل المقترح: درّب نفسك في قسم (مهارات النهايات) على ترقية البيادق وتنشيط الملك نحو الوسط."
            else ->
                "الحل المقترح: انتقل لمواجهة الأستاذ التالي أو فعّل التحدي التكيفي لرفع سقف حساباتك التكتيكية."
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
