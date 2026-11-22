package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.chess.BoardTheme
import com.example.chess.ChessBoardState
import com.example.chess.EngineBackendPreference
import com.example.chess.StockfishProbeResult
import com.example.data.UserPreferences
import com.example.overlay.FloatingChessService
import com.example.ui.components.MiniBoardThumbnail
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MonoFontFamily

@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    stockfishStatus: StockfishProbeResult,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectBoardTheme: (BoardTheme) -> Unit,
    onOpenBoardCustomization: () -> Unit,
    onOpenEngineEloDialog: () -> Unit,
    onToggleHumanStyle: (Boolean) -> Unit,
    onOpenPersonalitySheet: () -> Unit,
    onSelectEngineBackend: (EngineBackendPreference) -> Unit,
    onSelectStockfishMoveTime: (Int) -> Unit,
    onRefreshStockfish: () -> Unit,
    onToggleEvalBar: (Boolean) -> Unit,
    onToggleFloatingAutoShow: (Boolean) -> Unit,
    onOpenFloatingNow: () -> Unit,
    onOpenFloatingCustomization: () -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleAutoReview: (Boolean) -> Unit,
    onUpdateReminders: (Boolean, Int, Int) -> Unit,
    onTestNotification: () -> Unit
) {
    val context = LocalContext.current
    val hasOverlayPerm = FloatingChessService.canDrawOverlay(context)
    var showStockfishHelpDialog by remember { mutableStateOf(false) }
    val previewBoard = remember {
        ChessBoardState.fromFen("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2")
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onUpdateReminders(true, prefs.reminderHour, prefs.reminderMinute)
        }
    }

    val quickBoardThemes: List<Pair<BoardTheme, String>> = remember {
        listOf(
            BoardTheme.DEEP_OCEAN to "أزرق",
            BoardTheme.TOURNAMENT_EMERALD to "زيتوني",
            BoardTheme.ROYAL_WALNUT to "خشبي",
            BoardTheme.OBSIDIAN_GOLD to "رمادي"
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "الإعدادات",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "اضبط المظهر والمحرك وميزات اللعب",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. المظهر (Appearance Card)
            item {
                SettingsSectionCard(
                    title = "المظهر",
                    icon = Icons.Rounded.Palette
                ) {
                    // Light / Dark / System switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val selected = prefs.themeMode == mode
                            Surface(
                                onClick = { onSelectThemeMode(mode) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (mode == AppThemeMode.DARK) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = when (mode) {
                                            AppThemeMode.LIGHT -> "نهاري"
                                            AppThemeMode.DARK -> "ليلي"
                                            AppThemeMode.SYSTEM -> "تلقائي"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    SettingsNavRow(
                        title = "تخصيص الرقعة والقطع",
                        subtitle = "${prefs.pieceStyle.nameAr} • خامة ${prefs.boardMaterial.nameAr}",
                        icon = Icons.Rounded.GridView,
                        tag = "open_board_customization_row",
                        onClick = onOpenBoardCustomization
                    )

                    // Inline mini preview board + 4 color pills matching screenshot 8
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MiniBoardThumbnail(
                            boardState = previewBoard,
                            boardTheme = prefs.boardTheme,
                            pieceStyle = prefs.pieceStyle,
                            size = 76.dp
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickBoardThemes.take(2).forEach { (theme, shortLabel) ->
                                    ThemeSwatchPill(
                                        label = shortLabel,
                                        theme = theme,
                                        selected = prefs.boardTheme == theme,
                                        onClick = { onSelectBoardTheme(theme) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickBoardThemes.drop(2).forEach { (theme, shortLabel) ->
                                    ThemeSwatchPill(
                                        label = shortLabel,
                                        theme = theme,
                                        selected = prefs.boardTheme == theme,
                                        onClick = { onSelectBoardTheme(theme) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. محرك الشطرنج (Chess Engine & Stockfish UCI Card)
            item {
                SettingsSectionCard(
                    title = "محرك الشطرنج (ستوك فيش UCI)",
                    icon = Icons.Rounded.Psychology
                ) {
                    // Stockfish Engine Backend Status Box
                    val isNative = stockfishStatus.isNativeStockfishReady
                    val statusTint = if (isNative) Color(0xFF15803D) else MaterialTheme.colorScheme.primary
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = statusTint.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, statusTint.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Memory,
                                        contentDescription = null,
                                        tint = statusTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stockfishStatus.backend.labelAr,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = statusTint
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = statusTint.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = if (isNative) "UCI متصل" else "احتياطي ذكي",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusTint,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = stockfishStatus.diagnosticMessageAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onRefreshStockfish,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("refresh_stockfish_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("فحص المحرك", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { showStockfishHelpDialog = true },
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .testTag("stockfish_native_help_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تفعيل النسخة الأصلية", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // Backend preference selector
                    Text(
                        text = "وضع تشغيل المحرك",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EngineBackendPreference.entries.forEach { option ->
                            val selected = prefs.engineBackendPreference == option
                            Surface(
                                onClick = { onSelectEngineBackend(option) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backend_option_${option.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Text(
                                    text = option.nameAr,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }

                    // Thinking time selector
                    Text(
                        text = "زمن تفكير المحرك لكل نقلة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            400 to "سريع (0.4ث)",
                            900 to "متوازن (0.9ث)",
                            1800 to "عميق (1.8ث)"
                        ).forEach { (ms, label) ->
                            FilterChip(
                                selected = prefs.stockfishMoveTimeMs == ms,
                                onClick = { onSelectStockfishMoveTime(ms) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("movetime_chip_$ms")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsNavRow(
                        title = "قوة المحرك",
                        subtitle = "مخصص • نحو ${prefs.customEngineElo} إيلو",
                        icon = Icons.Rounded.Speed,
                        tag = "engine_elo_setting_row",
                        onClick = onOpenEngineEloDialog
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "الأسلوب البشري",
                        subtitle = "محاكاة تباين وتفكير البشر دون تعنت آلي بحت",
                        checked = prefs.humanStyleEnabled,
                        tag = "human_style_switch",
                        onCheckedChange = onToggleHumanStyle
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsNavRow(
                        title = "شخصية المحرك",
                        subtitle = "${prefs.enginePersonality.nameAr} — ${prefs.enginePersonality.descriptionAr}",
                        icon = Icons.Rounded.AutoAwesome,
                        tag = "engine_personality_row",
                        onClick = onOpenPersonalitySheet
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "شريط التقييم الحي",
                        subtitle = "إظهار أفضلية الأبيض والأسود بجانب الرقعة",
                        checked = prefs.showEvalBar,
                        tag = "eval_bar_switch",
                        onCheckedChange = onToggleEvalBar
                    )
                }
            }

            // 3. النافذة العائمة (Floating Window Card)
            item {
                SettingsSectionCard(
                    title = "النافذة العائمة",
                    icon = Icons.Rounded.PictureInPictureAlt
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حالة إذن العرض فوق التطبيقات",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasOverlayPerm) "مفعّل وجاهز للعرض فوق جميع التطبيقات" else "يتطلب منح إذن العرض فوق التطبيقات",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenFloatingNow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("launch_floating_window_btn")
                        ) {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (hasOverlayPerm) "تشغيل" else "منح الإذن")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "إظهار تلقائي عند التصغير",
                        subtitle = "فتح الفقاعة العائمة عند الخروج من التطبيق",
                        checked = prefs.floatingAutoShow,
                        tag = "floating_auto_show_switch",
                        onCheckedChange = onToggleFloatingAutoShow
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsNavRow(
                        title = "تخصيص النافذة العائمة",
                        subtitle = "العرض ${prefs.floatingWidthDp}dp • ${prefs.floatingFrameShape.nameAr} • شفافية ${prefs.floatingOpacityPercent}%",
                        icon = Icons.Rounded.Tune,
                        tag = "floating_customization_row",
                        onClick = onOpenFloatingCustomization
                    )
                }
            }

            // 4. الصوت والاهتزاز والتذكير اليومي
            item {
                SettingsSectionCard(
                    title = "الصوت والاهتزاز والتنبيهات",
                    icon = Icons.AutoMirrored.Rounded.VolumeUp
                ) {
                    SettingsSwitchRow(
                        title = "المؤثرات الصوتية",
                        subtitle = "أصوات النقلات والأسر والكش والفوز",
                        checked = prefs.soundEnabled,
                        tag = "sound_switch",
                        onCheckedChange = onToggleSound
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "الاهتزاز التفاعلي",
                        subtitle = "استجابة لمسية عند تحريك القطع",
                        checked = prefs.hapticsEnabled,
                        tag = "haptics_switch",
                        onCheckedChange = onToggleHaptics
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "التذكير اليومي الذكي",
                        subtitle = "تنبيه لحفظ سلسلة الألغاز والمستوى",
                        checked = prefs.remindersEnabled,
                        tag = "reminders_switch",
                        onCheckedChange = { enable ->
                            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!granted) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onUpdateReminders(true, prefs.reminderHour, prefs.reminderMinute)
                                }
                            } else {
                                onUpdateReminders(enable, prefs.reminderHour, prefs.reminderMinute)
                            }
                        }
                    )
                    if (prefs.remindersEnabled) {
                        val times = listOf(9 to "9:00 ص", 16 to "4:00 م", 19 to "7:00 م", 21 to "9:00 م")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            times.forEach { (hr, label) ->
                                FilterChip(
                                    selected = prefs.reminderHour == hr,
                                    onClick = { onUpdateReminders(true, hr, 0) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onTestNotification()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("معاينة التنبيه التحفيزي الآن")
                    }
                }
            }

            // 5. المراجعة والتلميحات (Review & Hints Card)
            item {
                SettingsSectionCard(
                    title = "المراجعة والتلميحات",
                    icon = Icons.Rounded.AutoAwesome
                ) {
                    SettingsSwitchRow(
                        title = "المراجعة التلقائية بعد المباراة",
                        subtitle = "فتح تقرير الأخطاء والتحليل مباشرة بعد نهاية الدور",
                        checked = prefs.autoReviewAfterGame,
                        tag = "auto_review_switch",
                        onCheckedChange = onToggleAutoReview
                    )
                }
            }

            // 6. حول التطبيق والتراخيص (About & Licenses)
            item {
                SettingsSectionCard(
                    title = "حول التطبيق والتراخيص",
                    icon = Icons.Rounded.Info
                ) {
                    Text(
                        text = "أرينا الشطرنج • محرك تحليل وتدريب ذكي يعمل 100% بدون إنترنت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "الإصدار 2.0 • يدعم بروتوكول UCI لمحرك Stockfish و PGN / FEN والنافذة العائمة",
                        fontFamily = MonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showStockfishHelpDialog) {
        AlertDialog(
            onDismissRequest = { showStockfishHelpDialog = false },
            title = {
                Text(
                    text = "تفعيل محرك Stockfish C++ الأصلي",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "يدعم التطبيق بروتوكول UCI الكامل لمحرك Stockfish (بما في ذلك Skill Level و UCI_Elo والخيوط المتعددة).\n\n" +
                        "بسبب عدم توزيع ملفات Stockfish C++ المترجمة (.so) وأوزان NNUE على المستودعات الجاهزة، يعمل التطبيق افتراضياً بالمحرك المدمج المتطور (Zobrist TT + PVS + Null-Move + LMR) بدون إنترنت.\n\n" +
                        "لتشغيل النسخة الأصلية من Stockfish NNUE:\n" +
                        "• عند تصدير المشروع، ضع ملف libstockfish.so المترجم لمعمارية arm64-v8a داخل مجلد jniLibs/arm64-v8a/.\n" +
                        "• أو ثبّت تطبيق محرك Open Exchange يدعم UCI وسيتم اكتشافه تلقائياً.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showStockfishHelpDialog = false }) {
                    Text("حسناً")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Rounded.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun ThemeSwatchPill(
    label: String,
    theme: BoardTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.darkSquare)
                    .border(1.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
