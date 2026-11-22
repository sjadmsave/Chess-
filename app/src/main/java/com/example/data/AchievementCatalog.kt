package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

data class AchievementDef(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val xpBonus: Int,
    val icon: ImageVector
)

object AchievementCatalog {
    val all: List<AchievementDef> = listOf(
        AchievementDef(
            id = "first_win",
            titleAr = "الانتصار الأول",
            descriptionAr = "حقق فوزك الأول ضد أي أستاذ ذكي.",
            xpBonus = 50,
            icon = Icons.Rounded.EmojiEvents
        ),
        AchievementDef(
            id = "clean_game",
            titleAr = "درع الانضباط",
            descriptionAr = "أكمل مباراة من 10 نقلات فأكثر بدون أي خطأ فادح.",
            xpBonus = 80,
            icon = Icons.Rounded.Shield
        ),
        AchievementDef(
            id = "sharp_accuracy",
            titleAr = "دقة المايسترو",
            descriptionAr = "حقق دقة لعب 88% أو أعلى في مباراة كاملة.",
            xpBonus = 100,
            icon = Icons.Rounded.AutoAwesome
        ),
        AchievementDef(
            id = "puzzle_hunter",
            titleAr = "قناص الألغاز",
            descriptionAr = "حل 5 ألغاز تكتيكية بنجاح.",
            xpBonus = 75,
            icon = Icons.Rounded.Extension
        ),
        AchievementDef(
            id = "daily_streak_3",
            titleAr = "شعلة الاستمرار",
            descriptionAr = "حافظ على سلسلة التحدي اليومي لـ 3 أيام متتالية.",
            xpBonus = 90,
            icon = Icons.Rounded.LocalFireDepartment
        ),
        AchievementDef(
            id = "beat_tal",
            titleAr = "قاهر الساحر",
            descriptionAr = "اهزم ميخائيل تال في مباراة كاملة.",
            xpBonus = 120,
            icon = Icons.Rounded.Bolt
        ),
        AchievementDef(
            id = "beat_polgar",
            titleAr = "كاسر الهجوم",
            descriptionAr = "اهزم جوديت بولغار في مباراة كاملة.",
            xpBonus = 150,
            icon = Icons.Rounded.Bolt
        ),
        AchievementDef(
            id = "beat_kasparov",
            titleAr = "متحدي البطل",
            descriptionAr = "اهزم غاري كاسباروف في مباراة كاملة.",
            xpBonus = 180,
            icon = Icons.Rounded.MilitaryTech
        ),
        AchievementDef(
            id = "beat_anand",
            titleAr = "صائد النمر",
            descriptionAr = "اهزم فيسواناثان أناند في مباراة كاملة.",
            xpBonus = 240,
            icon = Icons.Rounded.MilitaryTech
        ),
        AchievementDef(
            id = "beat_carlsen",
            titleAr = "أستاذ النخبة",
            descriptionAr = "اهزم ماغنوس كارلسن في قمة التحدي.",
            xpBonus = 300,
            icon = Icons.Rounded.WorkspacePremium
        ),
        AchievementDef(
            id = "strategist_1200",
            titleAr = "العقل الاستراتيجي",
            descriptionAr = "ارفع تصنيفك الشخصي إلى 1200 ELO فأعلى.",
            xpBonus = 150,
            icon = Icons.Rounded.Psychology
        )
    )
}
