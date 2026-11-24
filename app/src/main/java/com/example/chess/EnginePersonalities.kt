package com.example.chess

enum class PersonalityCategory(val id: String, val nameAr: String) {
    ALL("all", "الكل"),
    BASELINE("baseline", "الأساس"),
    ATTACKING("attacking", "هجومية"),
    DEFENSIVE("defensive", "دفاعية"),
    POSITIONAL("positional", "موضعية"),
    TACTICAL("tactical", "تكتيكية"),
    ENDGAME("endgame", "نهايات"),
    PSYCHOLOGICAL("psychological", "متنوعة ونفسية")
}

data class PersonalityTraits(
    val centerWeight: Float = 0f,
    val checkWeight: Float = 0f,
    val captureWeight: Float = 0f,
    val sacWeight: Float = 0f,
    val pawnStormWeight: Float = 0f,
    val kingSafetyWeight: Float = 0f,
    val mobilityRestrictionWeight: Float = 0f,
    val simplifyAdvantageWeight: Float = 0f,
    val flankWeight: Float = 0f,
    val quietTrapWeight: Float = 0f,
    val openFileWeight: Float = 0f,
    val endgameKingWeight: Float = 0f,
    val promotionWeight: Float = 0f,
    val avoidEarlyCapture: Boolean = false,
    val delayWinningTrade: Boolean = false,
    val mysteryDiversity: Boolean = false
)

/**
 * 50 distinct chess engine personalities covering attacking, defensive,
 * positional, tactical, endgame, classical, and psychological archetypes.
 */
enum class EnginePersonality(
    val id: String,
    val nameAr: String,
    val styleBadgeAr: String,
    val category: PersonalityCategory,
    val descriptionAr: String,
    val aggressionWeight: Float,
    val tacticalWeight: Float,
    val safetyWeight: Float,
    val traits: PersonalityTraits
) {
    // 1..13 Preserved Core Personalities
    CLASSIC(
        id = "classic",
        nameAr = "كلاسيكي",
        styleBadgeAr = "بلا شخصية",
        category = PersonalityCategory.BASELINE,
        descriptionAr = "Stockfish وفق القوة المختارة والأسلوب البشري فقط.",
        aggressionWeight = 1.0f, tacticalWeight = 1.0f, safetyWeight = 1.0f,
        traits = PersonalityTraits()
    ),
    FIERCE_WARRIOR(
        id = "warrior",
        nameAr = "المحارب الشرس",
        styleBadgeAr = "هجومي",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "الضغط على الخصم، الكشّات، التكتيك، وتقدّم البيادق المبكر.",
        aggressionWeight = 1.45f, tacticalWeight = 1.22f, safetyWeight = 0.80f,
        traits = PersonalityTraits(checkWeight = 18f, sacWeight = 6f, pawnStormWeight = 12f, captureWeight = 6f)
    ),
    IRON_FORTRESS(
        id = "fortress",
        nameAr = "القلعة الحديدية",
        styleBadgeAr = "دفاعي",
        category = PersonalityCategory.DEFENSIVE,
        descriptionAr = "حماية القطع، التبييت السريع، بنية البيادق، والبيادق الحرة.",
        aggressionWeight = 0.82f, tacticalWeight = 1.0f, safetyWeight = 1.45f,
        traits = PersonalityTraits(kingSafetyWeight = 24f, simplifyAdvantageWeight = 8f, sacWeight = -18f)
    ),
    STRATEGIC_GENIUS(
        id = "strategist",
        nameAr = "العبقري الاستراتيجي",
        styleBadgeAr = "موضعي",
        category = PersonalityCategory.POSITIONAL,
        descriptionAr = "تمركز القطع، بنية البيادق بوزن مرتفع، والأهداف الاستراتيجية.",
        aggressionWeight = 1.0f, tacticalWeight = 1.06f, safetyWeight = 1.25f,
        traits = PersonalityTraits(centerWeight = 15f, kingSafetyWeight = 10f, openFileWeight = 8f)
    ),
    PATIENT_SERPENT(
        id = "serpent",
        nameAr = "الثعبان الصبور",
        styleBadgeAr = "ضاغط بهدوء",
        category = PersonalityCategory.POSITIONAL,
        descriptionAr = "تقييد الخصم والتمركز والبنية؛ يتجنب الأخذ والكش المبكر والتكرار.",
        aggressionWeight = 0.90f, tacticalWeight = 1.05f, safetyWeight = 1.32f,
        traits = PersonalityTraits(centerWeight = 10f, mobilityRestrictionWeight = 14f, avoidEarlyCapture = true)
    ),
    ENDGAME_MASTER(
        id = "endgame_master",
        nameAr = "سيد النهايات",
        styleBadgeAr = "نهايات",
        category = PersonalityCategory.ENDGAME,
        descriptionAr = "البيادق الحرة، نشاط الملك، الأخذ عند التفوق، والبنية والحماية.",
        aggressionWeight = 0.96f, tacticalWeight = 1.12f, safetyWeight = 1.30f,
        traits = PersonalityTraits(endgameKingWeight = 18f, simplifyAdvantageWeight = 15f, promotionWeight = 14f, kingSafetyWeight = 8f)
    ),
    SACRIFICE_KING(
        id = "sacrifice_king",
        nameAr = "ملك التضحيات",
        styleBadgeAr = "مغامر",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "التضحية بتعويض، الضغط المتواصل، التكتيك، ومتابعة الكشّات.",
        aggressionWeight = 1.55f, tacticalWeight = 1.35f, safetyWeight = 0.70f,
        traits = PersonalityTraits(sacWeight = 18f, checkWeight = 16f, pawnStormWeight = 10f)
    ),
    BRILLIANCY_CREATOR(
        id = "brilliancy",
        nameAr = "صانع البريليانت",
        styleBadgeAr = "إبداعي",
        category = PersonalityCategory.TACTICAL,
        descriptionAr = "الأفكار الهادئة، التضحيات المدروسة، والتمركز؛ يتجنب تعريض القطع.",
        aggressionWeight = 1.32f, tacticalWeight = 1.32f, safetyWeight = 0.92f,
        traits = PersonalityTraits(quietTrapWeight = 14f, centerWeight = 8f, sacWeight = 8f)
    ),
    MISTAKE_CATCHER(
        id = "catcher",
        nameAr = "صياد الأخطاء",
        styleBadgeAr = "عملي",
        category = PersonalityCategory.TACTICAL,
        descriptionAr = "التكتيك وتقييد الخصم مع تحقق عملي من استثمار الأخطاء.",
        aggressionWeight = 1.14f, tacticalWeight = 1.28f, safetyWeight = 1.12f,
        traits = PersonalityTraits(mobilityRestrictionWeight = 13f, captureWeight = 10f, checkWeight = 10f)
    ),
    TRICKY_GENIUS(
        id = "tricky_genius",
        nameAr = "العبقري المتلاعب",
        styleBadgeAr = "خادع",
        category = PersonalityCategory.PSYCHOLOGICAL,
        descriptionAr = "التكتيك، الأفكار الهادئة، عروض الطُّعم، وتقييد الخصم بتحقق عملي.",
        aggressionWeight = 1.22f, tacticalWeight = 1.30f, safetyWeight = 0.95f,
        traits = PersonalityTraits(quietTrapWeight = 12f, mobilityRestrictionWeight = 12f, flankWeight = 7f)
    ),
    OPPORTUNITY_HUNTER(
        id = "hunter",
        nameAr = "صائد الفرص",
        styleBadgeAr = "انتهازي",
        category = PersonalityCategory.TACTICAL,
        descriptionAr = "المكاسب المادية والتكتيك؛ يفحص المات القصير (حتى ٣ نقلات) أولاً.",
        aggressionWeight = 1.24f, tacticalWeight = 1.38f, safetyWeight = 1.0f,
        traits = PersonalityTraits(captureWeight = 15f, promotionWeight = 16f, checkWeight = 12f)
    ),
    LONG_GAME_MAKER(
        id = "long_game",
        nameAr = "صانع المباراة الطويلة",
        styleBadgeAr = "مؤجِّل للحسم",
        category = PersonalityCategory.PSYCHOLOGICAL,
        descriptionAr = "عند التفوق يتجنب الأخذ والكش والتكرار ويؤخّر الحسم للحفاظ على المعركة.",
        aggressionWeight = 0.92f, tacticalWeight = 1.0f, safetyWeight = 1.24f,
        traits = PersonalityTraits(centerWeight = 9f, kingSafetyWeight = 8f, delayWinningTrade = true)
    ),
    MYSTERY_PLAYER(
        id = "mystery",
        nameAr = "اللاعب الغامض",
        styleBadgeAr = "غير متوقع",
        category = PersonalityCategory.PSYCHOLOGICAL,
        descriptionAr = "تنويع شبه عشوائي مثبّت بمعرّف المباراة يتجنب تكرار الجناح والقطعة نفسها.",
        aggressionWeight = 1.10f, tacticalWeight = 1.12f, safetyWeight = 1.0f,
        traits = PersonalityTraits(flankWeight = 6f, quietTrapWeight = 6f, mysteryDiversity = true)
    ),

    // 14..23 Attacking Archetypes (10 new)
    BLITZ_ASSASSIN(
        id = "blitz_assassin",
        nameAr = "مهاجم الخاطفة",
        styleBadgeAr = "هجوم سريع",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "يفضّل النقلات الخاطفة نحو الملك، الكشّات المستمرة وفتح الخطوط سريعاً.",
        aggressionWeight = 1.50f, tacticalWeight = 1.28f, safetyWeight = 0.78f,
        traits = PersonalityTraits(checkWeight = 20f, openFileWeight = 10f, pawnStormWeight = 8f)
    ),
    KINGSIDE_STORM(
        id = "kingside_storm",
        nameAr = "عاصفة جناح الملك",
        styleBadgeAr = "اقتحام الجناح",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "يدفع بيادق g و h و f لتمزيق تحصينات قلعة الخصم.",
        aggressionWeight = 1.48f, tacticalWeight = 1.20f, safetyWeight = 0.75f,
        traits = PersonalityTraits(pawnStormWeight = 20f, flankWeight = 12f, checkWeight = 10f)
    ),
    ROMANTIC_GAMBITEER(
        id = "romantic_gambit",
        nameAr = "بطل المناورات الرومانسي",
        styleBadgeAr = "مناورات",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "يضحي بيدقاً أو اثنين مقابل تفوق في الانتشار وفتح الأقطار.",
        aggressionWeight = 1.52f, tacticalWeight = 1.30f, safetyWeight = 0.72f,
        traits = PersonalityTraits(sacWeight = 16f, centerWeight = 12f, openFileWeight = 10f)
    ),
    TAL_INCARNATE(
        id = "tal_chaos",
        nameAr = "تلميذ تال",
        styleBadgeAr = "فوضى عبقرية",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "يخلق تعقيدات متشابكة ويهاجم المربعات الحساسة بلا تردد.",
        aggressionWeight = 1.58f, tacticalWeight = 1.40f, safetyWeight = 0.68f,
        traits = PersonalityTraits(sacWeight = 20f, checkWeight = 14f, mobilityRestrictionWeight = 8f)
    ),
    QUEENSIDE_RAIDER(
        id = "queenside_raider",
        nameAr = "مغير جناح الوزير",
        styleBadgeAr = "ضغط جناح الوزير",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "يركّز قواه على أعمدة a و b و c ويطلق هجوماً جانبياً مدمراً.",
        aggressionWeight = 1.34f, tacticalWeight = 1.18f, safetyWeight = 0.90f,
        traits = PersonalityTraits(flankWeight = 16f, openFileWeight = 12f, pawnStormWeight = 9f)
    ),
    CHECKMATE_ARTIST(
        id = "checkmate_artist",
        nameAr = "رسّام الكش مات",
        styleBadgeAr = "إماتة مباشرة",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "يبحث دائماً عن شبكة الإماتة ويقرّب الملكة والأحصنة من الملك.",
        aggressionWeight = 1.42f, tacticalWeight = 1.32f, safetyWeight = 0.85f,
        traits = PersonalityTraits(checkWeight = 18f, mobilityRestrictionWeight = 12f, sacWeight = 9f)
    ),
    PAWN_AVALANCHE(
        id = "pawn_avalanche",
        nameAr = "انهيار البيادق",
        styleBadgeAr = "زحف بيادق",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "يدفع سلسلة بيادق مترابطة لخنق مساحة الخصم بالكامل.",
        aggressionWeight = 1.32f, tacticalWeight = 1.10f, safetyWeight = 0.92f,
        traits = PersonalityTraits(pawnStormWeight = 22f, centerWeight = 10f, promotionWeight = 10f)
    ),
    DOUBLE_EDGE_DUELIST(
        id = "double_edge",
        nameAr = "مبارز الحدّين",
        styleBadgeAr = "مبارزة حادة",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "يفضّل المواقف ذات التبييت المتعاكس والسباق الهجومي المتبادل.",
        aggressionWeight = 1.44f, tacticalWeight = 1.28f, safetyWeight = 0.78f,
        traits = PersonalityTraits(pawnStormWeight = 14f, captureWeight = 10f, checkWeight = 12f)
    ),
    DRAGON_SLAYER(
        id = "dragon_slayer",
        nameAr = "قاتل التنين",
        styleBadgeAr = "هجوم الأوتار",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "يفتح عمود h والأقطار الطويلة لهجوم مركّز ضد الملك المتحصن.",
        aggressionWeight = 1.40f, tacticalWeight = 1.25f, safetyWeight = 0.84f,
        traits = PersonalityTraits(openFileWeight = 15f, checkWeight = 13f, sacWeight = 10f)
    ),
    FEARLESS_CAVALRY(
        id = "fearless_cavalry",
        nameAr = "سلاح الفرسان الجريء",
        styleBadgeAr = "ثغرات الفرسان",
        category = PersonalityCategory.ATTACKING,
        descriptionAr = "يزرع الأحصنة في المربعات المتقدمة e5 و d5 و f5 لفرض ضغط خانق.",
        aggressionWeight = 1.36f, tacticalWeight = 1.24f, safetyWeight = 0.88f,
        traits = PersonalityTraits(centerWeight = 14f, checkWeight = 12f, quietTrapWeight = 8f)
    ),

    // 24..31 Defensive & Solid Archetypes (8 new)
    BASTION_GUARDIAN(
        id = "bastion",
        nameAr = "حارس الحصن",
        styleBadgeAr = "صلابة مطلقة",
        category = PersonalityCategory.DEFENSIVE,
        descriptionAr = "يغلق كل الثغرات حول الملك ويمنع التسلل عبر الخطوط.",
        aggressionWeight = 0.76f, tacticalWeight = 1.02f, safetyWeight = 1.50f,
        traits = PersonalityTraits(kingSafetyWeight = 24f, mobilityRestrictionWeight = 8f, sacWeight = -20f)
    ),
    STONEWALL_MASTER(
        id = "stonewall",
        nameAr = "خبير الجدار الحجري",
        styleBadgeAr = "جدار متين",
        category = PersonalityCategory.DEFENSIVE,
        descriptionAr = "يبني تشكيلاً محكماً من البيادق في الوسط ويصعب اختراقه.",
        aggressionWeight = 0.85f, tacticalWeight = 1.0f, safetyWeight = 1.42f,
        traits = PersonalityTraits(centerWeight = 14f, kingSafetyWeight = 18f, avoidEarlyCapture = true)
    ),
    EXCHANGE_DEFENDER(
        id = "exchange_defender",
        nameAr = "مدافع التبادلات",
        styleBadgeAr = "تفكيك الهجوم",
        category = PersonalityCategory.DEFENSIVE,
        descriptionAr = "يطفئ أي مبادرة هجومية عبر تبادل القطع النشطة بدقة.",
        aggressionWeight = 0.84f, tacticalWeight = 1.08f, safetyWeight = 1.38f,
        traits = PersonalityTraits(simplifyAdvantageWeight = 18f, captureWeight = 12f, kingSafetyWeight = 12f)
    ),
    PETROSIAN_SHIELD(
        id = "petrosian_shield",
        nameAr = "درع بتروسيان",
        styleBadgeAr = "وقاية استباقية",
        category = PersonalityCategory.DEFENSIVE,
        descriptionAr = "يتوقع خطط الخصم مبكراً ويمنعها قبل أن تبدأ (Prophylaxis).",
        aggressionWeight = 0.86f, tacticalWeight = 1.10f, safetyWeight = 1.46f,
        traits = PersonalityTraits(mobilityRestrictionWeight = 16f, kingSafetyWeight = 16f, sacWeight = -14f)
    ),
    COUNTERPUNCHER(
        id = "counterpuncher",
        nameAr = "بطل الهجوم المرتد",
        styleBadgeAr = "دفاع ثم ارتداد",
        category = PersonalityCategory.DEFENSIVE,
        descriptionAr = "يمتص الضغط أولاً ثم يرد بضربة تكتيكية خاطفة عند توسّع الخصم.",
        aggressionWeight = 1.04f, tacticalWeight = 1.24f, safetyWeight = 1.28f,
        traits = PersonalityTraits(kingSafetyWeight = 14f, captureWeight = 12f, quietTrapWeight = 10f)
    ),
    QUIET_ANCHOR(
        id = "quiet_anchor",
        nameAr = "المرساة الهادئة",
        styleBadgeAr = "اتزان دفاعي",
        category = PersonalityCategory.DEFENSIVE,
        descriptionAr = "يتجنب المجازفة غير المحسوبة ويحمي كل قطعة بمضاعفة الدعم.",
        aggressionWeight = 0.80f, tacticalWeight = 1.04f, safetyWeight = 1.40f,
        traits = PersonalityTraits(kingSafetyWeight = 20f, centerWeight = 8f, sacWeight = -16f)
    ),
    CARO_EXPERT(
        id = "caro_expert",
        nameAr = "متخصص الكارو-كان",
        styleBadgeAr = "هيكل بيادق صلب",
        category = PersonalityCategory.DEFENSIVE,
        descriptionAr = "يدافع عن المربعات الحساسة ببنية بيادق نظيفة دون أي نقاط ضعف.",
        aggressionWeight = 0.88f, tacticalWeight = 1.06f, safetyWeight = 1.36f,
        traits = PersonalityTraits(kingSafetyWeight = 15f, simplifyAdvantageWeight = 10f, centerWeight = 10f)
    ),
    SILENT_FORTRESS(
        id = "silent_fortress",
        nameAr = "الحصن الصامت",
        styleBadgeAr = "تحصين صبور",
        category = PersonalityCategory.DEFENSIVE,
        descriptionAr = "يبني قلاعاً يصعب كسرها ويجبر المهاجم على استنزاف وقته وقطعه.",
        aggressionWeight = 0.78f, tacticalWeight = 1.02f, safetyWeight = 1.48f,
        traits = PersonalityTraits(kingSafetyWeight = 22f, avoidEarlyCapture = true, mobilityRestrictionWeight = 10f)
    ),

    // 32..39 Positional Archetypes (8 new)
    CENTER_CONTROLLER(
        id = "center_controller",
        nameAr = "مسيطر الوسط",
        styleBadgeAr = "هيمنة مركزية",
        category = PersonalityCategory.POSITIONAL,
        descriptionAr = "يضع أقصى أولوية للتحكم الكامل بالمربعات الأربعة الوسطى.",
        aggressionWeight = 1.05f, tacticalWeight = 1.08f, safetyWeight = 1.18f,
        traits = PersonalityTraits(centerWeight = 24f, openFileWeight = 8f)
    ),
    BISHOP_PAIR_LOVER(
        id = "bishop_pair",
        nameAr = "عاشق ثنائي الأفيال",
        styleBadgeAr = "تفوق الأقطار",
        category = PersonalityCategory.POSITIONAL,
        descriptionAr = "يحافظ على زوج الأفيال ويفتح الرقعة لاستغلالهما على الأقطار الطويلة.",
        aggressionWeight = 1.02f, tacticalWeight = 1.10f, safetyWeight = 1.16f,
        traits = PersonalityTraits(openFileWeight = 14f, flankWeight = 10f, centerWeight = 10f)
    ),
    KNIGHT_OUTPOSTER(
        id = "knight_outposter",
        nameAr = "سيد مواقع الأحصنة",
        styleBadgeAr = "حصن متقدم",
        category = PersonalityCategory.POSITIONAL,
        descriptionAr = "يتفوق في المناورات بالأحصنة نحو الثقوب الموضعية المحمية.",
        aggressionWeight = 1.04f, tacticalWeight = 1.12f, safetyWeight = 1.14f,
        traits = PersonalityTraits(centerWeight = 16f, quietTrapWeight = 11f, kingSafetyWeight = 8f)
    ),
    ROOK_HIGHWAY_ENGINEER(
        id = "rook_engineer",
        nameAr = "مهندس الأعمدة المفتوحة",
        styleBadgeAr = "سيطرة الأعمدة",
        category = PersonalityCategory.POSITIONAL,
        descriptionAr = "يضاعف القلاع على الأعمدة المفتوحة ويغزو الصف السابع.",
        aggressionWeight = 1.08f, tacticalWeight = 1.14f, safetyWeight = 1.12f,
        traits = PersonalityTraits(openFileWeight = 22f, simplifyAdvantageWeight = 8f)
    ),
    SPACE_CLAIMER(
        id = "space_claimer",
        nameAr = "محتكر المساحة",
        styleBadgeAr = "توسيع المساحة",
        category = PersonalityCategory.POSITIONAL,
        descriptionAr = "يتقدم بجبهة واسعة ليقلّص حرية حركة خصمه تدريجياً.",
        aggressionWeight = 1.10f, tacticalWeight = 1.06f, safetyWeight = 1.14f,
        traits = PersonalityTraits(mobilityRestrictionWeight = 18f, pawnStormWeight = 10f, centerWeight = 12f)
    ),
    FLANK_ARCHITECT(
        id = "flank_architect",
        nameAr = "مهندس الأجنحة",
        styleBadgeAr = "تطويق جانبي",
        category = PersonalityCategory.POSITIONAL,
        descriptionAr = "يفضّل افتتاحيات الأجنحة والفياكنتو والالتفاف البعيد عن الاشتباك المبكر.",
        aggressionWeight = 0.98f, tacticalWeight = 1.06f, safetyWeight = 1.20f,
        traits = PersonalityTraits(flankWeight = 20f, quietTrapWeight = 8f, avoidEarlyCapture = true)
    ),
    HARMONY_MASTER(
        id = "harmony_master",
        nameAr = "أستاذ الانسجام",
        styleBadgeAr = "تناسق القطع",
        category = PersonalityCategory.POSITIONAL,
        descriptionAr = "ينسّق القلاع والأفيال والبيادق في منظومة مترابطة بلا قطع معلّقة.",
        aggressionWeight = 0.98f, tacticalWeight = 1.10f, safetyWeight = 1.26f,
        traits = PersonalityTraits(kingSafetyWeight = 14f, centerWeight = 12f, openFileWeight = 10f)
    ),
    HYPERMODERN_REBEL(
        id = "hypermodern",
        nameAr = "المدرسة الحداثية",
        styleBadgeAr = "ضغط عن بُعد",
        category = PersonalityCategory.POSITIONAL,
        descriptionAr = "يدعو الخصم لبناء وسط كبير ثم يفجّره من الأجنحة والفياكنتو.",
        aggressionWeight = 1.12f, tacticalWeight = 1.16f, safetyWeight = 1.05f,
        traits = PersonalityTraits(flankWeight = 16f, quietTrapWeight = 12f, openFileWeight = 10f)
    ),

    // 40..45 Tactical & Endgame Archetypes (6 new)
    FORK_SPECIALIST(
        id = "fork_specialist",
        nameAr = "قنّاص الشوكة",
        styleBadgeAr = "تكتيك مزدوج",
        category = PersonalityCategory.TACTICAL,
        descriptionAr = "يركّز على الهجمات المزدوجة بالحصان والوزير والبيادق.",
        aggressionWeight = 1.26f, tacticalWeight = 1.38f, safetyWeight = 0.96f,
        traits = PersonalityTraits(captureWeight = 14f, checkWeight = 14f, quietTrapWeight = 10f)
    ),
    PIN_AND_SKEWER(
        id = "pin_skewer",
        nameAr = "خبير التثبيت والسيخ",
        styleBadgeAr = "أشعة تكتيكية",
        category = PersonalityCategory.TACTICAL,
        descriptionAr = "يضع القلاع والأفيال على نفس خط الملك والوزير لشل حركة الخصم.",
        aggressionWeight = 1.18f, tacticalWeight = 1.34f, safetyWeight = 1.02f,
        traits = PersonalityTraits(openFileWeight = 14f, mobilityRestrictionWeight = 14f, captureWeight = 8f)
    ),
    DISCOVERED_THUNDER(
        id = "discovered_thunder",
        nameAr = "صاعقة الهجوم الكاشف",
        styleBadgeAr = "كش كاشف",
        category = PersonalityCategory.TACTICAL,
        descriptionAr = "يصطف بقطعه الثقيلة لإنشاء ضربات كاشفة مدمّرة.",
        aggressionWeight = 1.30f, tacticalWeight = 1.36f, safetyWeight = 0.90f,
        traits = PersonalityTraits(checkWeight = 16f, openFileWeight = 12f, sacWeight = 10f)
    ),
    PAWN_PROMOTER(
        id = "pawn_promoter",
        nameAr = "صانع البيدق السالك",
        styleBadgeAr = "ترقية حاسمة",
        category = PersonalityCategory.ENDGAME,
        descriptionAr = "يسعى لخلق بيدق سالك مدعوم ودفعه نحو مربع الترقية بأي ثمن.",
        aggressionWeight = 1.0f, tacticalWeight = 1.12f, safetyWeight = 1.22f,
        traits = PersonalityTraits(promotionWeight = 24f, endgameKingWeight = 12f, simplifyAdvantageWeight = 10f)
    ),
    ROOK_ENDGAME_SAGE(
        id = "rook_endgame_sage",
        nameAr = "حكيم نهايات القلاع",
        styleBadgeAr = "لوسينا وفليدور",
        category = PersonalityCategory.ENDGAME,
        descriptionAr = "يبقِي القلاع نشطة خلف البيادق السالكة ويقطع ملك الخصم.",
        aggressionWeight = 0.96f, tacticalWeight = 1.14f, safetyWeight = 1.28f,
        traits = PersonalityTraits(openFileWeight = 18f, endgameKingWeight = 14f, simplifyAdvantageWeight = 12f)
    ),
    KING_ACTIVATOR(
        id = "king_activator",
        nameAr = "المَلِك المقاتل",
        styleBadgeAr = "نشاط الملك",
        category = PersonalityCategory.ENDGAME,
        descriptionAr = "يدفع الملك نحو الوسط كقطعة هجومية قاتلة بمجرد تصفية الوزراء.",
        aggressionWeight = 1.02f, tacticalWeight = 1.10f, safetyWeight = 1.20f,
        traits = PersonalityTraits(endgameKingWeight = 26f, promotionWeight = 12f, simplifyAdvantageWeight = 12f)
    ),

    // 46..50 Psychological & Wildcard Archetypes (5 new)
    SWINDLE_WIZARD(
        id = "swindle_wizard",
        nameAr = "ساحر الإنقاذ والخداع",
        styleBadgeAr = "فخاخ معاكسة",
        category = PersonalityCategory.PSYCHOLOGICAL,
        descriptionAr = "لا يستسلم حتى في المواقف الصعبة ويضع أفخاخ ردب وخطف أبدي.",
        aggressionWeight = 1.20f, tacticalWeight = 1.26f, safetyWeight = 0.94f,
        traits = PersonalityTraits(quietTrapWeight = 16f, checkWeight = 12f, mobilityRestrictionWeight = 8f)
    ),
    TIME_PRESSURE_SHARK(
        id = "time_shark",
        nameAr = "قرش ضغط الوقت",
        styleBadgeAr = "إرباك الخصم",
        category = PersonalityCategory.PSYCHOLOGICAL,
        descriptionAr = "يختار التفرعات غير الاعتيادية التي تجبر الخصم على استهلاك وقته.",
        aggressionWeight = 1.22f, tacticalWeight = 1.24f, safetyWeight = 0.92f,
        traits = PersonalityTraits(quietTrapWeight = 14f, flankWeight = 12f, mysteryDiversity = true)
    ),
    MINIMALIST_SNIPER(
        id = "minimalist_sniper",
        nameAr = "القنّاص البارد",
        styleBadgeAr = "تبسيط دقيق",
        category = PersonalityCategory.ENDGAME,
        descriptionAr = "يقتنص ميزة صغيرة ثم يصفي الرقعة بأبرع دقة حسابية دون تعاطف.",
        aggressionWeight = 0.94f, tacticalWeight = 1.20f, safetyWeight = 1.32f,
        traits = PersonalityTraits(simplifyAdvantageWeight = 22f, captureWeight = 10f, endgameKingWeight = 12f)
    ),
    CHAMELEON_ADAPTOR(
        id = "chameleon",
        nameAr = "الحرباء المتكيّفة",
        styleBadgeAr = "تبدّل تكتيكي",
        category = PersonalityCategory.PSYCHOLOGICAL,
        descriptionAr = "يبدّل إيقاعه بين الهجوم والهدوء حسب المرحلة ويغيّر أجنحة الضغط.",
        aggressionWeight = 1.12f, tacticalWeight = 1.16f, safetyWeight = 1.08f,
        traits = PersonalityTraits(centerWeight = 10f, flankWeight = 10f, mysteryDiversity = true)
    ),
    GRANDMASTER_PRECISION(
        id = "gm_precision",
        nameAr = "دقة الأساتذة الكبار",
        styleBadgeAr = "توازن مثالي",
        category = PersonalityCategory.BASELINE,
        descriptionAr = "مزيج متزن من السيطرة الموضعية، أمان الملك، واستثمار اللحظة الحاسمة.",
        aggressionWeight = 1.06f, tacticalWeight = 1.18f, safetyWeight = 1.18f,
        traits = PersonalityTraits(centerWeight = 12f, kingSafetyWeight = 12f, openFileWeight = 10f, endgameKingWeight = 10f)
    );

    companion object {
        fun byId(id: String): EnginePersonality {
            if (id.isBlank() || id == "traditional" || id == "classic") return CLASSIC
            return entries.find { it.id == id } ?: CLASSIC
        }
    }
}
