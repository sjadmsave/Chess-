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
    val badge: String,
    val colorHex: Long
) {
    BRILLIANT("نقلة عبقرية", "!!", 0xFF22D3EE),
    BEST("أفضل نقلة", "★", 0xFF34D399),
    EXCELLENT("نقلة ممتازة", "✓", 0xFF4ADE80),
    GOOD("نقلة جيدة", "•", 0xFF60A5FA),
    INACCURACY("عدم دقة", "?!", 0xFFFBBF24),
    MISTAKE("خطأ", "?", 0xFFFB923C),
    BLUNDER("خطأ فادح", "??", 0xFFF87171)
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
    WHITE_WINS_CHECKMATE("فوز الأبيض (كش مات)"),
    BLACK_WINS_CHECKMATE("فوز الأسود (كش مات)"),
    DRAW_STALEMATE("تعادل (ستالميت)"),
    DRAW_INSUFFICIENT_MATERIAL("تعادل (نقص القطع)"),
    DRAW_FIFTY_MOVE("تعادل (قاعدة 50 نقلة)"),
    DRAW_REPETITION("تعادل (تكرار ثلاثي)"),
    WHITE_RESIGNED("استسلام الأبيض"),
    BLACK_RESIGNED("استسلام الأسود")
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

enum class PieceStyle(
    val id: String,
    val nameAr: String,
    val subtitleAr: String
) {
    CLASSIC_STAUNTON("classic", "كلاسيكي ستونتون", "عاج دافئ وأوبسيديان أصيل"),
    MODERN_GEOMETRIC("geometric", "هندسي عصري", "أشكال حادة بلمسة معاصرة"),
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
    ROYAL_WALNUT(
        id = "walnut",
        nameAr = "خشب الجوز الملكي",
        lightSquare = Color(0xFFEDD6B0),
        darkSquare = Color(0xFFB57C4C),
        selectedHighlight = Color(0x88FBBF24),
        lastMoveHighlight = Color(0x55F59E0B),
        boardBorder = Color(0xFF5C3A21)
    ),
    TOURNAMENT_EMERALD(
        id = "emerald",
        nameAr = "زمرد الملوك",
        lightSquare = Color(0xFFEEEED2),
        darkSquare = Color(0xFF4F8A5B),
        selectedHighlight = Color(0x8838BDF8),
        lastMoveHighlight = Color(0x66FACC15),
        boardBorder = Color(0xFF214A31)
    ),
    OBSIDIAN_GOLD(
        id = "obsidian",
        nameAr = "رخام الليل الذهبي",
        lightSquare = Color(0xFFCBD5E1),
        darkSquare = Color(0xFF334155),
        selectedHighlight = Color(0x88FBBF24),
        lastMoveHighlight = Color(0x55EAB308),
        boardBorder = Color(0xFF1E293B)
    ),
    DEEP_OCEAN(
        id = "ocean",
        nameAr = "أزرق المحيط العميق",
        lightSquare = Color(0xFFE0F2FE),
        darkSquare = Color(0xFF2E6F95),
        selectedHighlight = Color(0x8834D399),
        lastMoveHighlight = Color(0x6638BDF8),
        boardBorder = Color(0xFF0F3A52)
    ),
    CHERRY_MAPLE(
        id = "cherry",
        nameAr = "خشب الكرز والقيقب",
        lightSquare = Color(0xFFF6E3C5),
        darkSquare = Color(0xFF9F4234),
        selectedHighlight = Color(0x88FBBF24),
        lastMoveHighlight = Color(0x55FB923C),
        boardBorder = Color(0xFF5C1E16)
    ),
    ROYAL_AMETHYST(
        id = "amethyst",
        nameAr = "أرجواني السديم",
        lightSquare = Color(0xFFEDE7F6),
        darkSquare = Color(0xFF6B4E90),
        selectedHighlight = Color(0x8838BDF8),
        lastMoveHighlight = Color(0x66C084FC),
        boardBorder = Color(0xFF3B2457)
    ),
    DESERT_BRONZE(
        id = "bronze",
        nameAr = "الرمال والبرونز",
        lightSquare = Color(0xFFF5EAD4),
        darkSquare = Color(0xFFA28354),
        selectedHighlight = Color(0x8810B981),
        lastMoveHighlight = Color(0x66D97706),
        boardBorder = Color(0xFF594426)
    ),
    CYBER_CARBON(
        id = "cyber",
        nameAr = "كربون سيبراني",
        lightSquare = Color(0xFF94A3B8),
        darkSquare = Color(0xFF1E293B),
        selectedHighlight = Color(0x8822D3EE),
        lastMoveHighlight = Color(0x5534D399),
        boardBorder = Color(0xFF065F46)
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
