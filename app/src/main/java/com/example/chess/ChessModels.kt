package com.example.chess

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(
    val valueCp: Int,
    val fenChar: Char,
    val sanLetter: String,
    val nameAr: String
) {
    PAWN(100, 'P', "", "بيدق"),
    KNIGHT(320, 'N', "N", "حصان"),
    BISHOP(330, 'B', "B", "فيل"),
    ROOK(500, 'R', "R", "قلعة"),
    QUEEN(900, 'Q', "Q", "وزير"),
    KING(20000, 'K', "K", "ملك")
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor
) {
    fun toFenChar(): Char = if (color == PieceColor.WHITE) type.fenChar else type.fenChar.lowercaseChar()

    companion object {
        fun fromFenChar(c: Char): ChessPiece? {
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = when (c.uppercaseChar()) {
                'P' -> PieceType.PAWN
                'N' -> PieceType.KNIGHT
                'B' -> PieceType.BISHOP
                'R' -> PieceType.ROOK
                'Q' -> PieceType.QUEEN
                'K' -> PieceType.KING
                else -> return null
            }
            return ChessPiece(type, color)
        }
    }
}

data class Square(val row: Int, val col: Int) {
    init {
        require(row in 0..7 && col in 0..7)
    }

    // row 0 = rank 8 (top), row 7 = rank 1 (bottom)
    fun toAlgebraic(): String {
        val file = ('a' + col)
        val rank = (8 - row)
        return "$file$rank"
    }

    val index: Int get() = row * 8 + col

    companion object {
        fun fromAlgebraic(s: String): Square? {
            if (s.length != 2) return null
            val col = s[0] - 'a'
            val row = 8 - (s[1] - '0')
            return if (row in 0..7 && col in 0..7) Square(row, col) else null
        }

        fun fromIndex(idx: Int): Square = Square(idx / 8, idx % 8)
    }
}

data class ChessMove(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
    val isCastleKingside: Boolean = false,
    val isCastleQueenside: Boolean = false,
    val isEnPassant: Boolean = false,
    val capturedPiece: ChessPiece? = null
) {
    fun toUci(): String = buildString {
        append(from.toAlgebraic())
        append(to.toAlgebraic())
        if (promotion != null) append(promotion.fenChar.lowercaseChar())
    }
}

enum class MoveQuality(
    val labelAr: String,
    val reviewLabelAr: String,
    val badge: String,
    val colorHex: Long
) {
    BRILLIANT("نقلة عبقرية", "جيدة", "!!", 0xFF72D9EF),
    BEST("أفضل نقلة", "جيدة", "★", 0xFF22A06B),
    EXCELLENT("نقلة ممتازة", "جيدة", "✓", 0xFF4CAF7A),
    GOOD("نقلة جيدة", "جيدة", "•", 0xFF69A84F),
    INACCURACY("عدم دقة", "عدم دقة", "?!", 0xFFF7C631),
    MISTAKE("خطأ", "خطأ", "?", 0xFFE58F2A),
    BLUNDER("خطأ كبير", "خطأ كبير", "??", 0xFFCA3431)
}

enum class GamePhase(val labelAr: String) {
    OPENING("الافتتاح"),
    MIDDLEGAME("وسط اللعب"),
    ENDGAME("نهاية اللعب")
}

data class RecordedMove(
    val moveNumber: Int,
    val color: PieceColor,
    val move: ChessMove,
    val san: String,
    val fenAfter: String,
    val evalWhiteCp: Int,
    val evalBeforeWhiteCp: Int,
    val centipawnLoss: Int,
    val quality: MoveQuality,
    val phase: GamePhase,
    val coachNoteAr: String = "",
    val bestMoveSan: String = ""
)

enum class GameResult(val labelAr: String) {
    ONGOING("جارية"),
    WHITE_WINS_CHECKMATE("كش مات • فوز الأبيض"),
    BLACK_WINS_CHECKMATE("كش مات • فوز الأسود"),
    WHITE_TIMEOUT("نفد وقت الأبيض"),
    BLACK_TIMEOUT("نفد وقت الأسود"),
    DRAW_TIMEOUT_INSUFFICIENT("تعادل بعد نفاد الوقت لعدم إمكانية الإماتة"),
    DRAW_STALEMATE("تعادل بالردب"),
    DRAW_INSUFFICIENT_MATERIAL("تعادل لعدم كفاية القطع"),
    DRAW_FIVE_FOLD("تعادل بتكرار الوضعية خمس مرات"),
    DRAW_SEVENTY_FIVE_MOVE("تعادل بقاعدة 75 نقلة"),
    DRAW_FIFTY_MOVE("تعادل بقاعدة 50 نقلة"),
    DRAW_REPETITION("تعادل بالتكرار الثلاثي"),
    DRAW_CLAIMED("تعادل بطلب اللاعب"),
    WHITE_RESIGNED("استسلام الأبيض"),
    BLACK_RESIGNED("استسلام الأسود");

    val isDraw: Boolean
        get() = this == DRAW_STALEMATE ||
            this == DRAW_INSUFFICIENT_MATERIAL ||
            this == DRAW_FIVE_FOLD ||
            this == DRAW_SEVENTY_FIVE_MOVE ||
            this == DRAW_FIFTY_MOVE ||
            this == DRAW_REPETITION ||
            this == DRAW_CLAIMED ||
            this == DRAW_TIMEOUT_INSUFFICIENT

    val isOngoing: Boolean
        get() = this == ONGOING
}

enum class GameMode(val id: String, val nameAr: String) {
    VS_ENGINE("vs_engine", "ضد المحرك"),
    VS_BOT_PERSONA("vs_bot", "تحدي الأساتذة"),
    LOCAL_TWO_PLAYER("local_2p", "لاعبان محليان");

    companion object {
        fun byId(id: String): GameMode = entries.find { it.id == id } ?: VS_ENGINE
    }
}

enum class StrengthTier(
    val id: String,
    val nameAr: String,
    val targetElo: Int,
    val eloRange: IntRange,
    val humanToleranceCp: Int,
    val heuristicCapCp: Int,
    val humanTemperature: Int,
    val personalityBaseCapCp: Int
) {
    BEGINNER("beginner", "مبتدئ", 300, 200..500, 100, 290, 170, 108),
    EASY("easy", "سهل", 700, 550..950, 100, 160, 90, 82),
    MEDIUM("medium", "متوسط", 1200, 1000..1500, 78, 78, 38, 57),
    STRONG("strong", "قوي", 1800, 1550..2300, 38, 38, 19, 37),
    MAX("max", "أقصى", -1, 2350..3000, 18, 18, 10, 23);

    companion object {
        fun byId(id: String): StrengthTier = entries.find { it.id == id } ?: EASY

        fun snapElo(rawElo: Int): Int {
            if (rawElo <= 0) return -1
            return (((rawElo + 25) / 50) * 50).coerceIn(200, 3000)
        }

        fun fromElo(elo: Int): StrengthTier {
            if (elo <= 0) return MAX
            val snapped = snapElo(elo)
            return when {
                snapped <= 500 -> BEGINNER
                snapped <= 950 -> EASY
                snapped <= 1500 -> MEDIUM
                snapped <= 2300 -> STRONG
                else -> MAX
            }
        }
    }
}

enum class PersonalityRisk(
    val id: String,
    val nameAr: String,
    val multiplier: Float
) {
    CONSERVATIVE("conservative", "محافظ ×٠.٦", 0.6f),
    BALANCED("balanced", "متوازن ×١.٠", 1.0f),
    AGGRESSIVE("aggressive", "مغامر ×١.٤٥", 1.45f);

    companion object {
        fun byId(id: String): PersonalityRisk = entries.find { it.id == id } ?: BALANCED
    }
}

enum class SoundProfile(val id: String, val nameAr: String) {
    SOFT("soft", "ناعم"),
    WOOD("wood", "خشبي"),
    DIGITAL("digital", "رقمي");

    companion object {
        fun byId(id: String): SoundProfile = entries.find { it.id == id } ?: SOFT
    }
}

enum class VibrationStyle(val level: Int, val nameAr: String, val amplitude: Int) {
    LIGHT(0, "خفيف", 65),
    MEDIUM(1, "متوسط", 125),
    STRONG(2, "قوي", 200);

    companion object {
        fun byLevel(level: Int): VibrationStyle = entries.find { it.level == level } ?: LIGHT
    }
}

enum class HintStage(val step: Int, val titleAr: String) {
    CONCEPT(1, "فكرة عامة"),
    PIECE(2, "تحديد القطعة"),
    EXACT(3, "النقلة الكاملة")
}

data class ProgressiveHint(
    val fen: String,
    val move: ChessMove,
    val san: String,
    val conceptAr: String,
    val pieceHintAr: String,
    val exactMoveAr: String
)

data class MultiPvLine(
    val rank: Int,
    val move: ChessMove,
    val san: String,
    val evalWhiteCp: Int,
    val mateIn: Int? = null,
    val depth: Int = 1,
    val pvSan: List<String> = emptyList(),
    val pvUci: List<String> = emptyList(),
    val nodes: Long = 0L,
    val tbHits: Long = 0L
)

enum class TimeControl(
    val id: String,
    val labelAr: String,
    val categoryAr: String,
    val initialSeconds: Long,
    val incrementSeconds: Long
) {
    UNLIMITED("unlimited", "بلا ساعة", "مفتوح", 0L, 0L),
    MIN_5("min5", "5 د", "سريع", 300L, 0L),
    MIN_10("min10", "10 د", "سريع", 600L, 0L),
    MIN_15("min15", "15 د", "كلاسيكي", 900L, 0L);

    companion object {
        fun byId(id: String): TimeControl = when (id) {
            "unlimited" -> UNLIMITED
            "min5", "blitz5_3", "bullet3_2" -> MIN_5
            "min10", "rapid10" -> MIN_10
            "min15" -> MIN_15
            else -> UNLIMITED
        }
    }
}

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun toFen(): String {
        val sb = StringBuilder()
        if (whiteKingside) sb.append('K')
        if (whiteQueenside) sb.append('Q')
        if (blackKingside) sb.append('k')
        if (blackQueenside) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    companion object {
        fun fromFen(s: String): CastlingRights = CastlingRights(
            whiteKingside = 'K' in s,
            whiteQueenside = 'Q' in s,
            blackKingside = 'k' in s,
            blackQueenside = 'q' in s
        )
    }
}

enum class BotPersona(
    val id: String,
    val nameAr: String,
    val titleAr: String,
    val elo: Int,
    val searchDepth: Int,
    val noiseCp: Int,
    val aggressionWeight: Float,
    val styleTagAr: String,
    val descriptionAr: String,
    @DrawableRes val avatarRes: Int,
    val accentColor: Color,
    val greetingsAr: List<String>,
    val goodMoveReactionsAr: List<String>,
    val blunderReactionsAr: List<String>
) {
    FISCHER(
        id = "fischer",
        nameAr = "بوبي فيشر",
        titleAr = "العبقري الصاعد • مستوى مبتدئ",
        elo = 900,
        searchDepth = 2,
        noiseCp = 95,
        aggressionWeight = 1.0f,
        styleTagAr = "افتتاحيات كلاسيكية",
        descriptionAr = "يلعب افتتاحيات واضحة ومباشرة مع أخطاء تعليمية تمنحك فرصة لاكتشاف التكتيكات.",
        avatarRes = R.drawable.img_gm_fischer,
        accentColor = Color(0xFFF59E0B),
        greetingsAr = listOf(
            "أهلاً بك على الرقعة! لنبدأ بمباراة كلاسيكية نظيفة.",
            "السيطرة على الوسط هي مفتاح النجاح، أرني خطتك!"
        ),
        goodMoveReactionsAr = listOf(
            "نقلة منضبطة! تحافظ على توازنك.",
            "أحسنت، انتشار جميل للقطع."
        ),
        blunderReactionsAr = listOf(
            "انتبه! هذه النقلة تترك ثغرة واضحة.",
            "تأكد من حماية قطعك قبل الهجوم!"
        )
    ),
    CAPABLANCA(
        id = "capablanca",
        nameAr = "خوسيه كابابلانكا",
        titleAr = "آلة الشطرنج • مستوى هاوٍ متطور",
        elo = 1100,
        searchDepth = 2,
        noiseCp = 58,
        aggressionWeight = 0.92f,
        styleTagAr = "بساطة ونقاء استراتيجي",
        descriptionAr = "يفضل المواقف الواضحة وتبسيط اللعب نحو نهايات متوازنة دون تعقيد عشوائي.",
        avatarRes = R.drawable.img_gm_capablanca,
        accentColor = Color(0xFFD97706),
        greetingsAr = listOf(
            "الأناقة في الشطرنج تكمن في بساطة الخطة ووضوحها.",
            "لنتنافس بهدوء؛ التمركز السليم يسبق الهجوم."
        ),
        goodMoveReactionsAr = listOf(
            "نقلة طبيعية ومريحة، تفكير سليم.",
            "توزيع متوازن جداً للبيادق والقطع."
        ),
        blunderReactionsAr = listOf(
            "تنازلت عن توازن الموقف بسهولة هنا.",
            "هذه الخسارة المادية ستظهر في النهاية."
        )
    ),
    TAL(
        id = "tal",
        nameAr = "ميخائيل تال",
        titleAr = "ساحر ريغا • مستوى متوسط",
        elo = 1300,
        searchDepth = 2,
        noiseCp = 32,
        aggressionWeight = 1.35f,
        styleTagAr = "هجوم وتضحيات",
        descriptionAr = "يعشق التضحيات الجريئة والمواقف المعقدة ضد ملكك. يختبر يقظتك التكتيكية باستمرار.",
        avatarRes = R.drawable.img_gm_tal,
        accentColor = Color(0xFF10B981),
        greetingsAr = listOf(
            "هل أنت مستعد لمغامرة تكتيكية؟ القطع خلقت لتتحرك!",
            "سأبحث عن أي ثغرة قرب ملكك، كن حذراً."
        ),
        goodMoveReactionsAr = listOf(
            "دفاع صلب! لم تنخدع بالهجوم.",
            "رؤية تكتيكية ذكية جداً!"
        ),
        blunderReactionsAr = listOf(
            "همم... أشم رائحة تضحية رابحة هنا!",
            "حساباتك أغفلت هذا الرد التكتيكي."
        )
    ),
    POLGAR(
        id = "polgar",
        nameAr = "جوديت بولغار",
        titleAr = "ملكة الهجوم • مستوى فوق المتوسط",
        elo = 1500,
        searchDepth = 3,
        noiseCp = 24,
        aggressionWeight = 1.28f,
        styleTagAr = "مبادرة وهجوم ديناميكي",
        descriptionAr = "تهاجم بلا تردد وتفتح الأوتار والأعمدة لانتزاع المبادرة منذ النقلة العاشرة.",
        avatarRes = R.drawable.img_gm_polgar,
        accentColor = Color(0xFFA855F7),
        greetingsAr = listOf(
            "الهجوم هو أفضل وسيلة للدفاع! استعد لمعركة حامية.",
            "لن أنتظر طويلاً، المبادرة هي هدفي الأول."
        ),
        goodMoveReactionsAr = listOf(
            "رد جريء! تحافظ على نشاط قطعك.",
            "قراءة ممتازة لتهديداتي على الجناح."
        ),
        blunderReactionsAr = listOf(
            "الآن انفتح الطريق أمام قطعي نحو ملكك!",
            "تراخيت في الدفاع عن هذا المربع الحساس."
        )
    ),
    KASPAROV(
        id = "kasparov",
        nameAr = "غاري كاسباروف",
        titleAr = "بطل باكو • مستوى متقدم",
        elo = 1700,
        searchDepth = 3,
        noiseCp = 12,
        aggressionWeight = 1.2f,
        styleTagAr = "ضغط استراتيجي عميق",
        descriptionAr = "يفرض ضغطاً متواصلاً على المربعات المركزية ويستغل أدق الأخطاء الاستراتيجية.",
        avatarRes = R.drawable.img_gm_kasparov,
        accentColor = Color(0xFFEF4444),
        greetingsAr = listOf(
            "الشطرنج اختبار للإرادة والتركيز. أرني أفضل ما لديك.",
            "كل تمبو وكل مربع في الوسط له ثمن اليوم."
        ),
        goodMoveReactionsAr = listOf(
            "نقلة بمستوى الأساتذة! ضغط متوازن.",
            "مثير للإعجاب، تقرأ الموقف بعمق."
        ),
        blunderReactionsAr = listOf(
            "خطأ استراتيجي مكلف، سأعاقبه فوراً.",
            "لقد تراجعت مبادرتك الآن."
        )
    ),
    ALEKHINE(
        id = "alekhine",
        nameAr = "ألكسندر أليخين",
        titleAr = "سيد التركيبات • خبير تكتيكي",
        elo = 1850,
        searchDepth = 3,
        noiseCp = 6,
        aggressionWeight = 1.30f,
        styleTagAr = "تركيبات وصيد الملك",
        descriptionAr = "يصنع هجمات مركبة من عدة جهات ويستغل أضعف نقطة في دفاع الخصم بدقة قاتلة.",
        avatarRes = R.drawable.img_gm_alekhine,
        accentColor = Color(0xFFF43F5E),
        greetingsAr = listOf(
            "أعشق التركيبات الخفية التي تقلب الموازين فجأة.",
            "راقب كل الأقطار؛ الخطر يأتي من حيث لا تتوقع."
        ),
        goodMoveReactionsAr = listOf(
            "تنبأت بالفخ في اللحظة المناسبة!",
            "دفاع عميق يحبط التركيبة الهجومية."
        ),
        blunderReactionsAr = listOf(
            "تركيبة تكتيكية قادمة لا مفر منها!",
            "سمحت لقطعي بالتنسيق المثالي ضد ملكك."
        )
    ),
    ANAND(
        id = "anand",
        nameAr = "فيسواناثان أناند",
        titleAr = "نمر مدراس • أستاذ دولي كبير",
        elo = 1950,
        searchDepth = 4,
        noiseCp = 4,
        aggressionWeight = 1.12f,
        styleTagAr = "سرعة حسابية ومرونة",
        descriptionAr = "متوازن في جميع الافتتاحيات، سريع الاقتناص للأخطاء، ويجيد الانتقال السلس للنهايات.",
        avatarRes = R.drawable.img_gm_anand,
        accentColor = Color(0xFFFB923C),
        greetingsAr = listOf(
            "المرونة والسرعة في تقييم الموقف هما سر التفوق.",
            "أرحب بتحديك؛ لنرَ من يحسب أدق اليوم."
        ),
        goodMoveReactionsAr = listOf(
            "رد سريع ودقيق للغاية!",
            "إدارة ممتازة للتوقيت والمساحات."
        ),
        blunderReactionsAr = listOf(
            "التقطتُ الثغرة فوراً، التفوق يتحول لي الآن.",
            "نقلة متسرعة كلفتك أفضلية الموقف."
        )
    ),
    CARLSEN(
        id = "carlsen",
        nameAr = "ماغنوس كارلسن",
        titleAr = "ملك النهايات • أستاذ كبير",
        elo = 2100,
        searchDepth = 4,
        noiseCp = 0,
        aggressionWeight = 1.05f,
        styleTagAr = "دقة قاتلة ونهايات",
        descriptionAr = "دقة تحليل عالية في كل مراحل اللعب، يضغط بلا كلل في وسط اللعب ونهايته.",
        avatarRes = R.drawable.img_gm_carlsen,
        accentColor = Color(0xFF38BDF8),
        greetingsAr = listOf(
            "لنرَ كم يمكنك الصمود في المعركة الاستراتيجية الطويلة.",
            "أدق التفاصيل في بنية البيادق تصنع الفارق."
        ),
        goodMoveReactionsAr = listOf(
            "دقيقة جداً. تلعب بهدوء المحترفين.",
            "تعامل ممتاز مع تعقيدات الموقف."
        ),
        blunderReactionsAr = listOf(
            "هذا التنازل الصغير يكفي لحسم النهاية.",
            "انتبه، لقد سمحت لي بأفضلية دائمة."
        )
    );

    companion object {
        fun byId(id: String): BotPersona = entries.find { it.id == id } ?: FISCHER
    }
}

/**
 * Ruqaa 13 Engine Personalities (12 active styles + CLASSIC baseline).
 */
enum class EnginePersonality(
    val id: String,
    val nameAr: String,
    val styleBadgeAr: String,
    val descriptionAr: String,
    val aggressionWeight: Float,
    val tacticalWeight: Float,
    val safetyWeight: Float
) {
    CLASSIC(
        id = "classic",
        nameAr = "كلاسيكي",
        styleBadgeAr = "بلا شخصية",
        descriptionAr = "Stockfish وفق القوة المختارة والأسلوب البشري فقط.",
        aggressionWeight = 1.0f,
        tacticalWeight = 1.0f,
        safetyWeight = 1.0f
    ),
    FIERCE_WARRIOR(
        id = "warrior",
        nameAr = "المحارب الشرس",
        styleBadgeAr = "هجومي",
        descriptionAr = "الضغط على الخصم، الكشّات، التكتيك، وتقدّم البيادق المبكر.",
        aggressionWeight = 1.45f,
        tacticalWeight = 1.22f,
        safetyWeight = 0.80f
    ),
    IRON_FORTRESS(
        id = "fortress",
        nameAr = "القلعة الحديدية",
        styleBadgeAr = "دفاعي",
        descriptionAr = "حماية القطع، التبييت السريع، بنية البيادق، والبيادق الحرة.",
        aggressionWeight = 0.82f,
        tacticalWeight = 1.0f,
        safetyWeight = 1.45f
    ),
    STRATEGIC_GENIUS(
        id = "strategist",
        nameAr = "العبقري الاستراتيجي",
        styleBadgeAr = "موضعي",
        descriptionAr = "تمركز القطع، بنية البيادق بوزن مرتفع، والأهداف الاستراتيجية.",
        aggressionWeight = 1.0f,
        tacticalWeight = 1.06f,
        safetyWeight = 1.25f
    ),
    PATIENT_SERPENT(
        id = "serpent",
        nameAr = "الثعبان الصبور",
        styleBadgeAr = "ضاغط بهدوء",
        descriptionAr = "تقييد الخصم والتمركز والبنية؛ يتجنب الأخذ والكش المبكر والتكرار.",
        aggressionWeight = 0.90f,
        tacticalWeight = 1.05f,
        safetyWeight = 1.32f
    ),
    ENDGAME_MASTER(
        id = "endgame_master",
        nameAr = "سيد النهايات",
        styleBadgeAr = "نهايات",
        descriptionAr = "البيادق الحرة، نشاط الملك، الأخذ عند التفوق، والبنية والحماية.",
        aggressionWeight = 0.96f,
        tacticalWeight = 1.12f,
        safetyWeight = 1.30f
    ),
    SACRIFICE_KING(
        id = "sacrifice_king",
        nameAr = "ملك التضحيات",
        styleBadgeAr = "مغامر",
        descriptionAr = "التضحية بتعويض، الضغط المتواصل، التكتيك، ومتابعة الكشّات.",
        aggressionWeight = 1.55f,
        tacticalWeight = 1.35f,
        safetyWeight = 0.70f
    ),
    BRILLIANCY_CREATOR(
        id = "brilliancy",
        nameAr = "صانع البريليانت",
        styleBadgeAr = "إبداعي",
        descriptionAr = "الأفكار الهادئة، التضحيات المدروسة، والتمركز؛ يتجنب تعريض القطع.",
        aggressionWeight = 1.32f,
        tacticalWeight = 1.32f,
        safetyWeight = 0.92f
    ),
    MISTAKE_CATCHER(
        id = "catcher",
        nameAr = "صياد الأخطاء",
        styleBadgeAr = "عملي",
        descriptionAr = "التكتيك وتقييد الخصم مع تحقق عملي من استثمار الأخطاء.",
        aggressionWeight = 1.14f,
        tacticalWeight = 1.28f,
        safetyWeight = 1.12f
    ),
    TRICKY_GENIUS(
        id = "tricky_genius",
        nameAr = "العبقري المتلاعب",
        styleBadgeAr = "خادع",
        descriptionAr = "التكتيك، الأفكار الهادئة، عروض الطُّعم، وتقييد الخصم بتحقق عملي.",
        aggressionWeight = 1.22f,
        tacticalWeight = 1.30f,
        safetyWeight = 0.95f
    ),
    OPPORTUNITY_HUNTER(
        id = "hunter",
        nameAr = "صائد الفرص",
        styleBadgeAr = "انتهازي",
        descriptionAr = "المكاسب المادية والتكتيك؛ يفحص المات القصير (حتى ٣ نقلات) أولاً.",
        aggressionWeight = 1.24f,
        tacticalWeight = 1.38f,
        safetyWeight = 1.0f
    ),
    LONG_GAME_MAKER(
        id = "long_game",
        nameAr = "صانع المباراة الطويلة",
        styleBadgeAr = "مؤجِّل للحسم",
        descriptionAr = "عند التفوق يتجنب الأخذ والكش والتكرار ويؤخّر الحسم للحفاظ على المعركة.",
        aggressionWeight = 0.92f,
        tacticalWeight = 1.0f,
        safetyWeight = 1.24f
    ),
    MYSTERY_PLAYER(
        id = "mystery",
        nameAr = "اللاعب الغامض",
        styleBadgeAr = "غير متوقع",
        descriptionAr = "تنويع شبه عشوائي مثبّت بمعرّف المباراة يتجنب تكرار الجناح والقطعة نفسها.",
        aggressionWeight = 1.10f,
        tacticalWeight = 1.12f,
        safetyWeight = 1.0f
    );

    companion object {
        fun byId(id: String): EnginePersonality {
            if (id.isBlank() || id == "traditional" || id == "classic") return CLASSIC
            return entries.find { it.id == id } ?: CLASSIC
        }
    }
}

enum class BoardMaterial(val id: String, val nameAr: String) {
    FLAT("flat", "سادة"),
    WOOD("wood", "خشب"),
    IVORY("ivory", "عاج"),
    MARBLE("marble", "رخام");

    companion object {
        fun byId(id: String): BoardMaterial = entries.find { it.id == id } ?: FLAT
    }
}

enum class FloatingFrameShape(val id: String, val nameAr: String, val cornerDp: Int) {
    ROUNDED("rounded", "مستديرة", 12),
    COMPACT("compact", "مدمجة", 7),
    SQUARE("square", "مربّعة", 0);

    companion object {
        fun byId(id: String): FloatingFrameShape = entries.find { it.id == id } ?: ROUNDED
    }
}

enum class FloatingBubbleSize(val id: String, val nameAr: String, val sizeDp: Int) {
    SMALL("small", "صغيرة", 44),
    MEDIUM("medium", "متوسطة", 52),
    LARGE("large", "كبيرة", 64);

    companion object {
        fun byId(id: String): FloatingBubbleSize = entries.find { it.id == id } ?: MEDIUM
    }
}

enum class EngineBackendPreference(val id: String, val nameAr: String, val subtitleAr: String) {
    STOCKFISH_AUTO(
        id = "stockfish_auto",
        nameAr = "ستوك فيش (Stockfish UCI)",
        subtitleAr = "يستخدم ملف libstockfish.so أو محرك OEX عند توفره، أو المحرك المدمج المتطور تلقائياً"
    ),
    BUILTIN_ENGINE(
        id = "builtin",
        nameAr = "المحرك المدمج المتطور",
        subtitleAr = "بحث فوري بجدول التحويلات Zobrist وتقليم المسارات"
    );

    companion object {
        fun byId(id: String): EngineBackendPreference = entries.find { it.id == id } ?: STOCKFISH_AUTO
    }
}

enum class PieceStyle(
    val id: String,
    val nameAr: String,
    val subtitleAr: String
) {
    CLASSIC_STAUNTON("classic", "كلاسيكي", "عاج دافئ وأوبسيديان أصيل"),
    MODERN_GEOMETRIC("geometric", "هندسي", "أشكال حادة بلمسة معاصرة"),
    HIGH_CONTRAST("high_contrast", "عالي الوضوح", "خطوط عريضة وتباين حاد للرؤية السريعة"),
    ROYAL_HERITAGE("royal", "تراثي ملكي", "تاج ذهبي وقواعد فاخرة"),
    WOOD_CARVED("wood", "خشب محفور", "قيقب فاتح وماهوغني دافئ"),
    NEON_CYBER("neon", "نيون سيبراني", "توهج تركوازي وذهبي متقد");

    companion object {
        fun byId(id: String): PieceStyle = entries.find { it.id == id } ?: CLASSIC_STAUNTON
    }
}

enum class BoardTheme(
    val id: String,
    val nameAr: String,
    val lightSquare: Color,
    val darkSquare: Color,
    val selectedHighlight: Color,
    val lastMoveHighlight: Color,
    val boardBorder: Color
) {
    DEEP_OCEAN(
        id = "ocean",
        nameAr = "أزرق",
        lightSquare = Color(0xFFE3EEF8),
        darkSquare = Color(0xFF42739A),
        selectedHighlight = Color(0x8872D9EF),
        lastMoveHighlight = Color(0x66006A87),
        boardBorder = Color(0xFF173652)
    ),
    TOURNAMENT_EMERALD(
        id = "emerald",
        nameAr = "زيتوني",
        lightSquare = Color(0xFFEEEED2),
        darkSquare = Color(0xFF769656),
        selectedHighlight = Color(0x99F7C631),
        lastMoveHighlight = Color(0x88BACA44),
        boardBorder = Color(0xFF3A4A29)
    ),
    ROYAL_WALNUT(
        id = "walnut",
        nameAr = "خشبي",
        lightSquare = Color(0xFFEDD6B0),
        darkSquare = Color(0xFFB57C4C),
        selectedHighlight = Color(0x88FBBF24),
        lastMoveHighlight = Color(0x55F59E0B),
        boardBorder = Color(0xFF5C3A21)
    ),
    OBSIDIAN_GOLD(
        id = "obsidian",
        nameAr = "رمادي",
        lightSquare = Color(0xFFD8DEE8),
        darkSquare = Color(0xFF526277),
        selectedHighlight = Color(0x8872D9EF),
        lastMoveHighlight = Color(0x5594A3B8),
        boardBorder = Color(0xFF1E293B)
    ),
    CHERRY_MAPLE(
        id = "cherry",
        nameAr = "كرز وقيقب",
        lightSquare = Color(0xFFF6E3C5),
        darkSquare = Color(0xFF9F4234),
        selectedHighlight = Color(0x88FBBF24),
        lastMoveHighlight = Color(0x55FB923C),
        boardBorder = Color(0xFF5C1E16)
    ),
    ROYAL_AMETHYST(
        id = "amethyst",
        nameAr = "أرجواني",
        lightSquare = Color(0xFFEDE7F6),
        darkSquare = Color(0xFF6B4E90),
        selectedHighlight = Color(0x8872D9EF),
        lastMoveHighlight = Color(0x66C084FC),
        boardBorder = Color(0xFF3B2457)
    ),
    DESERT_BRONZE(
        id = "bronze",
        nameAr = "رمال وبرونز",
        lightSquare = Color(0xFFF5EAD4),
        darkSquare = Color(0xFFA28354),
        selectedHighlight = Color(0x8810B981),
        lastMoveHighlight = Color(0x66D97706),
        boardBorder = Color(0xFF594426)
    ),
    CYBER_CARBON(
        id = "cyber",
        nameAr = "كربون",
        lightSquare = Color(0xFF94A3B8),
        darkSquare = Color(0xFF1E293B),
        selectedHighlight = Color(0x8872D9EF),
        lastMoveHighlight = Color(0x55006A87),
        boardBorder = Color(0xFF0B3B4B)
    );

    companion object {
        fun byId(id: String): BoardTheme = entries.find { it.id == id } ?: TOURNAMENT_EMERALD
    }
}

data class MoveHint(
    val move: ChessMove,
    val san: String,
    val explanationAr: String
)

data class PostGameReport(
    val playerAccuracy: Int,
    val opponentAccuracy: Int,
    val acpl: Int,
    val openingNameAr: String,
    val openingAccuracy: Int,
    val middlegameAccuracy: Int,
    val endgameAccuracy: Int,
    val qualityCounts: Map<MoveQuality, Int>,
    val strengthsAr: List<String>,
    val weaknessesAr: List<String>,
    val diagnosisAr: String,
    val actionPlanAr: List<String>,
    val eloDelta: Int
)
