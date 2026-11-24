package com.example.chess

import java.util.Calendar

enum class PuzzleCategory(val labelAr: String) {
    MATE_IN_1("كش مات في نقلة"),
    MATE_IN_2("كش مات في نقلتين"),
    TACTICS("تكتيك وشوكة وتثبيت"),
    ENDGAME("مهارات النهايات")
}

data class ChessPuzzle(
    val id: String,
    val titleAr: String,
    val category: PuzzleCategory,
    val rating: Int,
    val fen: String,
    val solutionUci: List<String>,
    val hintAr: String,
    val explanationAr: String
)

object PuzzleRepository {

    private val originalPuzzles: List<ChessPuzzle> = listOf(
        // Mate in 1
        ChessPuzzle(
            id = "p_m1_1",
            titleAr = "مات الصف الخلفي الكلاسيكي",
            category = PuzzleCategory.MATE_IN_1,
            rating = 800,
            fen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            solutionUci = listOf("d1d8"),
            hintAr = "انظر إلى الصف الثامن حيث الملك الأسود محاصر خلف بيادقه.",
            explanationAr = "القلعة إلى d8 تعطي كش مات لأن بيادق الأسود تمنع الملك من الهروب للأمام."
        ),
        ChessPuzzle(
            id = "p_m1_2",
            titleAr = "مات الباحث السريعة",
            category = PuzzleCategory.MATE_IN_1,
            rating = 850,
            fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
            solutionUci = listOf("h5f7"),
            hintAr = "بيدق f7 محمي فقط بالملك، والفيل على c4 يدعم الهجوم.",
            explanationAr = "الوزير يأسر بيدق f7 بدعم مباشر من الفيل، ليحسم المباراة فوراً."
        ),
        ChessPuzzle(
            id = "p_m1_3",
            titleAr = "المات المختنقة بالحصان",
            category = PuzzleCategory.MATE_IN_1,
            rating = 950,
            fen = "6rk/6pp/8/6N1/8/8/6PP/6K1 w - - 0 1",
            solutionUci = listOf("g5f7"),
            hintAr = "الملك الأسود محاصر بالكامل بقطعه؛ أي مربع يمكن للحصان الهجوم منه؟",
            explanationAr = "الحصان على f7 يغلق كل المربعات لأن قطع الأسود نفسها تسد طريق الهروب."
        ),
        ChessPuzzle(
            id = "p_m1_4",
            titleAr = "تناغم الوزير والفيل",
            category = PuzzleCategory.MATE_IN_1,
            rating = 920,
            fen = "6k1/5p1p/8/8/6Q1/2B5/6PP/6K1 w - - 0 1",
            solutionUci = listOf("g4g7"),
            hintAr = "الفيل يسيطر على القطر الطويل نحو g7، فاستغل ذلك بالوزير.",
            explanationAr = "الوزير يندفع إلى g7 محمياً بالفيل ليحرم الملك من أي مربع للهروب."
        ),
        ChessPuzzle(
            id = "p_m1_5",
            titleAr = "المات العربية الخالدة",
            category = PuzzleCategory.MATE_IN_1,
            rating = 1050,
            fen = "7k/4R3/5N2/8/8/8/8/6K1 w - - 0 1",
            solutionUci = listOf("e7h7"),
            hintAr = "الحصان يراقب g8 ويحمي مربع h7، فأين تتحرك القلعة؟",
            explanationAr = "القلعة إلى h7 بدعم الحصان على f6 تشكل المات العربية التراثية الشهيرة."
        ),

        // Tactics
        ChessPuzzle(
            id = "p_tac_1",
            titleAr = "الشوكة الملكية بالحصان",
            category = PuzzleCategory.TACTICS,
            rating = 1100,
            fen = "2q2nk1/5ppp/8/3N4/8/8/5PPP/6K1 w - - 0 1",
            solutionUci = listOf("d5e7", "g8h8", "e7c8"),
            hintAr = "ابحث عن قفزة للحصان تهاجم الملك والوزير في اللحظة نفسها!",
            explanationAr = "الحصان إلى e7 يعطي كشاً للملك ويهدد الوزير على c8 ليحسم التفوق المادي."
        ),
        ChessPuzzle(
            id = "p_tac_2",
            titleAr = "التثبيت القاتل على القطر",
            category = PuzzleCategory.TACTICS,
            rating = 1150,
            fen = "6k1/3r1qpp/8/8/8/1P6/4BPPP/6K1 w - - 0 1",
            solutionUci = listOf("e2c4"),
            hintAr = "كيف يمكنك استغلال اصطفاف الملك والوزير الأسود على نفس القطر؟",
            explanationAr = "الفيل إلى c4 يثبّت الوزير تماماً أمام الملك وهو محمي ببيدق b3."
        ),
        ChessPuzzle(
            id = "p_tac_3",
            titleAr = "الهجوم الكاشف المزدوج",
            category = PuzzleCategory.TACTICS,
            rating = 1250,
            fen = "4q1k1/7p/8/8/4B3/8/6PP/4R1K1 w - - 0 1",
            solutionUci = listOf("e4h7"),
            hintAr = "حرّك الفيل في نقلة تعطي كشاً لتكشف هجوم القلعة على الوزير!",
            explanationAr = "أسر البيدق على h7 بكش يكشف عمود e للقلعة لتربح الوزير الأسود مجاناً."
        ),
        ChessPuzzle(
            id = "p_tac_4",
            titleAr = "ضربة السيخ (Skewer)",
            category = PuzzleCategory.TACTICS,
            rating = 1300,
            fen = "4q3/8/4k3/8/8/8/8/R5K1 w - - 0 1",
            solutionUci = listOf("a1e1"),
            hintAr = "الملك الأسود يحجب وزيره على عمود e؛ هاجم الملك على طول العمود.",
            explanationAr = "القلعة إلى e1 تجبر الملك على الابتعاد وتترك الوزير مكشوفاً للأسر."
        ),

        // Mate in 2
        ChessPuzzle(
            id = "p_m2_1",
            titleAr = "تضحية الوزير في الصف الثامن",
            category = PuzzleCategory.MATE_IN_2,
            rating = 1350,
            fen = "3r2k1/5ppp/2Q5/8/8/8/5PPP/4R1K1 w - - 0 1",
            solutionUci = listOf("c6e8", "d8e8", "e1e8"),
            hintAr = "ضحِّ بوزيرك مباشرة في مربع e8 لسحب القلعة المدافعة!",
            explanationAr = "بعد Qe8+ ثم Rxe8 تقتحم قلعة الأبيض مربع e8 لتحقق كش مات حتمي."
        ),
        ChessPuzzle(
            id = "p_m2_2",
            titleAr = "تضحية الساحر للمات المختنقة",
            category = PuzzleCategory.MATE_IN_2,
            rating = 1500,
            fen = "r6k/6pp/7N/8/8/1Q6/6PP/6K1 w - - 0 1",
            solutionUci = listOf("b3g8", "a8g8", "h6f7"),
            hintAr = "أجبر القلعة السوداء على إغلاق مربع الهروب الوحيد للملك!",
            explanationAr = "Qg8+ تجبر القلعة على الأسر Rxg8، فيخنق الحصان الملك في f7."
        ),
        ChessPuzzle(
            id = "p_m2_3",
            titleAr = "مات أنستازيا المذهلة",
            category = PuzzleCategory.MATE_IN_2,
            rating = 1600,
            fen = "r6k/4N1pp/8/8/7Q/3R4/6PP/6K1 w - - 0 1",
            solutionUci = listOf("h4h7", "h8h7", "d3h3"),
            hintAr = "اسحب الملك نحو عمود h بتضحية جريئة بالوزير!",
            explanationAr = "Qxh7+ Kxh7 ثم Rh3#؛ الحصان يقطع مربعي g8 و g6 عن الملك."
        ),
        ChessPuzzle(
            id = "p_m2_4",
            titleAr = "اقتحام القطر الخلفي",
            category = PuzzleCategory.MATE_IN_2,
            rating = 1450,
            fen = "5qk1/5ppp/5P2/2Q5/B7/8/6PP/4R1K1 w - - 0 1",
            solutionUci = listOf("c5f8", "g8f8", "e1e8"),
            hintAr = "تخلص من المدافع الأساسي في الصف الثامن بتبادل إجباري!",
            explanationAr = "Qxf8+ تجبر الملك على الأسر Kxf8، فتدخل القلعة e8 بدعم الفيل معلنة المات."
        ),

        // Endgame
        ChessPuzzle(
            id = "p_end_1",
            titleAr = "تضحية النهاية لترقية البيدق",
            category = PuzzleCategory.ENDGAME,
            rating = 1400,
            fen = "2r5/2P4k/3K4/8/8/8/8/1R6 w - - 0 1",
            solutionUci = listOf("b1b8"),
            hintAr = "امنح القلعة دعماً لتهديد الترقية الإجباري في c8!",
            explanationAr = "Rb8 تجبر الأسود على تبادل القلاع فيرقى البيدق إلى وزير مع كش."
        ),
        ChessPuzzle(
            id = "p_end_2",
            titleAr = "اختراق البيادق الثلاثي",
            category = PuzzleCategory.ENDGAME,
            rating = 1550,
            fen = "7k/ppp5/8/PPP5/8/8/8/7K w - - 0 1",
            solutionUci = listOf("b5b6"),
            hintAr = "ادفع البيدق الأوسط لفتح الطريق أمام أحد البيدقين الجانبيين نحو الترقية!",
            explanationAr = "النقلة b6 تخلق اختراقاً لا يمكن إيقافه؛ فأي أسر يسمح للبيدق المقابل بالعبور."
        )
    )

    val puzzles: List<ChessPuzzle> = originalPuzzles +
        mateInOnePuzzlePack +
        mateInTwoPuzzlePack +
        tacticsPuzzlePack +
        endgamesPuzzlePack

    fun dailyPuzzleForToday(playerElo: Int): ChessPuzzle {
        val cal = Calendar.getInstance()
        val daySeed = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        // Filter puzzles appropriate around player's skill level, or pick from full pool
        val tiered = puzzles.filter {
            when {
                playerElo < 1050 -> it.rating <= 1250
                playerElo < 1450 -> it.rating in 1050..1500
                else -> it.rating >= 1250
            }
        }.ifEmpty { puzzles }
        return tiered[daySeed % tiered.size]
    }

    fun todayDateKey(): String {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m, d)
    }
}
