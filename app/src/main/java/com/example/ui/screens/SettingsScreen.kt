package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import com.example.chess.EvalBarPosition
import com.example.chess.SoundProfile
import com.example.chess.StockfishProbeResult
import com.example.chess.StrengthTier
import com.example.chess.SyzygyTableEntry
import com.example.chess.VibrationStyle
import com.example.data.UserPreferences
import com.example.overlay.FloatingChessService
import com.example.ui.components.MiniBoardThumbnail
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.formatArabicNumber
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    stockfishStatus: StockfishProbeResult,
    syzygyTables: List<SyzygyTableEntry>,
    syzygyStatusMessage: String?,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectBoardTheme: (BoardTheme) -> Unit,
    onOpenBoardCustomization: () -> Unit,
    onSelectStrengthTier: (StrengthTier) -> Unit,
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
    onSelectSoundProfile: (SoundProfile) -> Unit,
    onPreviewSoundProfile: (SoundProfile) -> Unit = {},
    onChangeSoundVolume: (Int) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSelectVibrationStyle: (VibrationStyle) -> Unit,
    onToggleAutoReview: (Boolean) -> Unit,
    onUpdateReminders: (Boolean, Int, Int) -> Unit,
    onTestNotification: () -> Unit,
    onImportSyzygyFiles: (List<Uri>) -> Unit,
    onDeleteSyzygyTable: (String) -> Unit,
    onClearSyzygyTables: () -> Unit,
    onOpenAboutLicenses: () -> Unit,
    onSelectStockfishThreads: (Int) -> Unit = {},
    onSelectStockfishHashMb: (Int) -> Unit = {},
    onTogglePowerSaving: (Boolean) -> Unit = {},
    onSelectEvalBarPosition: (EvalBarPosition) -> Unit = {},
    onToggleShowEngineArrows: (Boolean) -> Unit = {},
    onSelectAnalysisMultiPvCount: (Int) -> Unit = {},
    onTogglePremove: (Boolean) -> Unit = {},
    onToggleUnassisted: (Boolean) -> Unit = {},
    onToggleShowThreats: (Boolean) -> Unit = {},
    onToggleBotResign: (Boolean) -> Unit = {},
    onTogglePauseOnBlunder: (Boolean) -> Unit = {},
    onTogglePauseOnMistake: (Boolean) -> Unit = {},
    onToggleShowMyMoveStrength: (Boolean) -> Unit = {},
    onToggleShowOpponentMoveStrength: (Boolean) -> Unit = {},
    onToggleShowMultiPvNumbers: (Boolean) -> Unit = {},
    onToggleArrowStrengthColors: (Boolean) -> Unit = {},
    onTogglePreviousBestGhostArrow: (Boolean) -> Unit = {},
    onToggleColorLastMoveByQuality: (Boolean) -> Unit = {},
    onToggleFigurineNotation: (Boolean) -> Unit = {},
    onToggleShowCapturedPieces: (Boolean) -> Unit = {},
    onToggleShowCoordinates: (Boolean) -> Unit = {},
    onToggleTapOnlyDisableDrag: (Boolean) -> Unit = {},
    onToggleEnlargeOnDrag: (Boolean) -> Unit = {},
    onToggleShowAverageCplInReport: (Boolean) -> Unit = {},
    onChangeBoardScalePercent: (Int) -> Unit = {}
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

    val syzygyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportSyzygyFiles(uris)
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
                        text = "اضبط المظهر والمحرك وجداول النهايات والنافذة العائمة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. المظهر
            item {
                SettingsSectionCard(
                    title = "المظهر",
                    icon = Icons.Rounded.Palette
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val selected = prefs.themeMode == mode
                            Surface(
                                onClick = { onSelectThemeMode(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
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
                                            AppThemeMode.LIGHT -> "فاتح"
                                            AppThemeMode.DARK -> "داكن"
                                            AppThemeMode.SYSTEM -> "النظام"
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

            // 2. محرك الشطرنج (Stockfish UCI + 5 Tiers + 50 Personalities)
            item {
                SettingsSectionCard(
                    title = "محرك الشطرنج (Stockfish UCI)",
                    icon = Icons.Rounded.Psychology
                ) {
                    val isNative = stockfishStatus.isNativeStockfishReady
                    val statusTint = if (isNative) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
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
                                        .heightIn(min = 48.dp)
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
                                        .heightIn(min = 48.dp)
                                        .testTag("stockfish_native_help_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("تفاصيل Stockfish", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

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
                                    .heightIn(min = 48.dp)
                                    .testTag("backend_option_${option.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                    Text(
                                        text = option.nameAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 5 Ruqaa Quick Strength Tier Pills
                    Text(
                        text = "المستويات الخمسة السريعة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StrengthTier.entries.forEach { tier ->
                            val active = prefs.effectiveStrengthTier == tier
                            Surface(
                                onClick = { onSelectStrengthTier(tier) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("settings_tier_${tier.id}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = if (active) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = tier.nameAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (tier.targetElo <= 0) "∞" else formatArabicNumber(tier.targetElo),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    val eloSubtitle = if (prefs.effectiveEngineElo <= 0) {
                        "أقصى قوة • بلا حد إيلو"
                    } else {
                        "${prefs.effectiveStrengthTier.nameAr} • نحو ${formatArabicNumber(prefs.effectiveEngineElo)} إيلو"
                    }
                    SettingsNavRow(
                        title = "تخصيص قوة المحرك (منزلق إيلو)",
                        subtitle = eloSubtitle,
                        icon = Icons.Rounded.Speed,
                        tag = "engine_elo_setting_row",
                        onClick = onOpenEngineEloDialog
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "الأسلوب البشري",
                        subtitle = "توزيع احتمالي واقعي على المستويات المحدودة دون تعنت آلي",
                        checked = prefs.humanStyleEnabled,
                        tag = "human_style_switch",
                        onCheckedChange = onToggleHumanStyle
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsNavRow(
                        title = "شخصية المحرك والشدّة والمخاطرة (٥٠ شخصية)",
                        subtitle = "${prefs.enginePersonality.nameAr} • شدّة ${formatArabicNumber(prefs.personalityIntensity)}٪ • ${prefs.personalityRisk.nameAr}",
                        icon = Icons.Rounded.AutoAwesome,
                        tag = "engine_personality_row",
                        onClick = onOpenPersonalitySheet
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "شريط التقييم الحي",
                        subtitle = "إظهار أفضلية الأبيض والأسود مع الرقعة",
                        checked = prefs.showEvalBar,
                        tag = "eval_bar_switch",
                        onCheckedChange = onToggleEvalBar
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Text(
                        text = "موضع شريط التقييم",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EvalBarPosition.entries.forEach { pos ->
                            FilterChip(
                                selected = prefs.evalBarPosition == pos,
                                onClick = { onSelectEvalBarPosition(pos) },
                                label = { Text(pos.nameAr) },
                                modifier = Modifier.testTag("eval_pos_${pos.id}")
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Text(
                        text = "خيوط المعالج (Threads): ${formatArabicNumber(prefs.stockfishThreads)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 4).forEach { t ->
                            FilterChip(
                                selected = prefs.stockfishThreads == t,
                                onClick = { onSelectStockfishThreads(t) },
                                label = { Text(formatArabicNumber(t)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        text = "ذاكرة المحرك (Hash): ${formatArabicNumber(prefs.stockfishHashMb)} م.ب",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(16, 32, 64, 128, 256).forEach { mb ->
                            FilterChip(
                                selected = prefs.stockfishHashMb == mb,
                                onClick = { onSelectStockfishHashMb(mb) },
                                label = { Text("${formatArabicNumber(mb)} م.ب") }
                            )
                        }
                    }
                    SettingsSwitchRow(
                        title = "وضع توفير الطاقة",
                        subtitle = "تقليل عمق وزمن البحث التلقائي للحفاظ على البطارية",
                        checked = prefs.powerSavingMode,
                        tag = "power_saving_switch",
                        onCheckedChange = onTogglePowerSaving
                    )
                    Text(
                        text = "عدد خطوط التحليل (MultiPV): ${formatArabicNumber(prefs.analysisMultiPvCount)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..5).forEach { cnt ->
                            FilterChip(
                                selected = prefs.analysisMultiPvCount == cnt,
                                onClick = { onSelectAnalysisMultiPvCount(cnt) },
                                label = { Text(formatArabicNumber(cnt)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    SettingsSwitchRow(
                        title = "أسهم المحرك على الرقعة",
                        subtitle = "رسم أسهم أفضل الخطوط المقترحة أثناء التحليل",
                        checked = prefs.showEngineArrowsOnBoard,
                        tag = "engine_arrows_switch",
                        onCheckedChange = onToggleShowEngineArrows
                    )
                }
            }

            // 3. جداول نهايات Syzygy الاختيارية (Section 5.5)
            item {
                val totalBytes = remember(syzygyTables) { syzygyTables.sumOf { it.sizeBytes } }
                val totalMbText = remember(totalBytes) {
                    val kb = (totalBytes / 1024L).toInt()
                    if (kb >= 1024) "${formatArabicNumber(kb / 1024)} م.ب" else "${formatArabicNumber(kb)} ك.ب"
                }
                SettingsSectionCard(
                    title = "جداول نهايات Syzygy الاختيارية",
                    icon = Icons.Rounded.Storage
                ) {
                    Text(
                        text = if (syzygyTables.isEmpty()) {
                            "لم تُستورد جداول نهايات (.rtbw / .rtbz). يمكنك استيراد حتى ١٢٨ ميجابايت من جهازك (بحد ٣٢ ميجابايت للملف)."
                        } else {
                            "المستورد: ${formatArabicNumber(syzygyTables.size)} ملف • الإجمالي $totalMbText (حد ١٢٨ م.ب)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!syzygyStatusMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = syzygyStatusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { syzygyPickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("import_syzygy_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("استيراد .rtbw/.rtbz")
                        }
                        if (syzygyTables.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onClearSyzygyTables,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .heightIn(min = 48.dp)
                                    .testTag("clear_syzygy_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("مسح الكل")
                            }
                        }
                    }
                    syzygyTables.take(6).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${entry.fileName} (${if (entry.isWdl) "WDL" else "DTZ"}) • ${formatArabicNumber((entry.sizeBytes / 1024L).toInt().coerceAtLeast(1))} ك.ب",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteSyzygyTable(entry.fileName) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف ${entry.fileName}", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 4. النافذة العائمة
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
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("launch_floating_window_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (hasOverlayPerm) "تشغيل" else "منح الإذن")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "إظهار تلقائي عند التصغير",
                        subtitle = "فتح النافذة العائمة تلقائياً عند مغادرة التطبيق",
                        checked = prefs.floatingAutoShow,
                        tag = "floating_auto_show_switch",
                        onCheckedChange = onToggleFloatingAutoShow
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsNavRow(
                        title = "تخصيص النافذة العائمة",
                        subtitle = "العرض ${formatArabicNumber(prefs.floatingWidthDp)} dp • ${prefs.floatingFrameShape.nameAr} • شفافية ${formatArabicNumber(prefs.floatingOpacityPercent)}٪",
                        icon = Icons.Rounded.Tune,
                        tag = "floating_customization_row",
                        onClick = onOpenFloatingCustomization
                    )
                }
            }

            // 5. الصوت والاهتزاز والتنبيهات (Ruqaa Section 12)
            item {
                SettingsSectionCard(
                    title = "الصوت والاهتزاز والتنبيهات",
                    icon = Icons.AutoMirrored.Rounded.VolumeUp
                ) {
                    SettingsSwitchRow(
                        title = "المؤثرات الصوتية",
                        subtitle = "٨ طوابع صوتية واقعية لكل أحداث اللعب (اضغط للمعاينة)",
                        checked = prefs.soundEnabled,
                        tag = "sound_switch",
                        onCheckedChange = onToggleSound
                    )
                    if (prefs.soundEnabled) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SoundProfile.entries.forEach { sp ->
                                FilterChip(
                                    selected = prefs.soundProfile == sp,
                                    onClick = {
                                        onSelectSoundProfile(sp)
                                        onPreviewSoundProfile(sp)
                                    },
                                    label = { Text(sp.nameAr) },
                                    modifier = Modifier.testTag("sound_profile_${sp.id}")
                                )
                            }
                        }
                        Text(
                            text = "${prefs.soundProfile.nameAr}: ${prefs.soundProfile.descAr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "مستوى الصوت: ${formatArabicNumber(prefs.soundVolume)}٪",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = prefs.soundVolume.toFloat(),
                            onValueChange = { onChangeSoundVolume(it.roundToInt().coerceIn(0, 100)) },
                            valueRange = 0f..100f,
                            modifier = Modifier.testTag("sound_volume_slider")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "الاهتزاز التفاعلي",
                        subtitle = "نمط نبضات مخصص للنقلة والأسر والتبييت والكش والنهاية",
                        checked = prefs.hapticsEnabled,
                        tag = "haptics_switch",
                        onCheckedChange = onToggleHaptics
                    )
                    if (prefs.hapticsEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VibrationStyle.entries.forEach { vs ->
                                FilterChip(
                                    selected = prefs.vibrationStyle == vs,
                                    onClick = { onSelectVibrationStyle(vs) },
                                    label = { Text(vs.nameAr) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("vibration_style_${vs.level}")
                                )
                            }
                        }
                    }

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
                        val times = listOf(9 to "٩:٠٠ ص", 16 to "٤:٠٠ م", 19 to "٧:٠٠ م", 21 to "٩:٠٠ م")
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("معاينة التنبيه التحفيزي الآن")
                    }
                }
            }

            // 6. إعدادات اللعب
            item {
                SettingsSectionCard(
                    title = "إعدادات اللعب",
                    icon = Icons.Rounded.SportsEsports
                ) {
                    SettingsSwitchRow(
                        title = "الحركة المسبقة (Pre-move)",
                        subtitle = "تحديد نقلتك التالية مسبقاً أثناء تفكير الخصم",
                        checked = prefs.premoveEnabled,
                        tag = "settings_premove_switch",
                        onCheckedChange = onTogglePremove
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "لعب بدون مساعدة",
                        subtitle = "إخفاء التقييم والأسهم والتهديدات أثناء المباريات الجارية",
                        checked = prefs.unassistedMode,
                        tag = "settings_unassisted_switch",
                        onCheckedChange = onToggleUnassisted
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "إظهار التهديدات",
                        subtitle = "رسم أسهم تحذيرية لتهديدات الخصم المباشرة",
                        checked = prefs.showThreats,
                        tag = "settings_threats_switch",
                        onCheckedChange = onToggleShowThreats
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "استسلام البوت عند الخسارة الحتمية",
                        subtitle = "انسحاب المحرك تلقائياً بعد ٣ نقلات متتالية بأفضلية ساحقة",
                        checked = prefs.botResignEnabled,
                        tag = "settings_bot_resign_switch",
                        onCheckedChange = onToggleBotResign
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "إيقاف مؤقت عند الخطأ الفادح (??)",
                        subtitle = "تنبيه فوري مع خيار التراجع عن الخطأ الكبير",
                        checked = prefs.pauseOnBlunder,
                        tag = "settings_pause_blunder_switch",
                        onCheckedChange = onTogglePauseOnBlunder
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "إيقاف مؤقت عند الخطأ (?)",
                        subtitle = "تنبيه فوري عند ارتكاب خطأ متوسط أثناء التدريب",
                        checked = prefs.pauseOnMistake,
                        tag = "settings_pause_mistake_switch",
                        onCheckedChange = onTogglePauseOnMistake
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "إظهار قوة نقلاتي",
                        subtitle = "تمييز جودة كل نقلة تلعبها في الشريط والرقعة",
                        checked = prefs.showMyMoveStrength,
                        tag = "settings_my_strength_switch",
                        onCheckedChange = onToggleShowMyMoveStrength
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "إظهار قوة نقلات الخصم",
                        subtitle = "عرض تقييم وجودة نقلات الخصم للمراجعة السريعة",
                        checked = prefs.showOpponentMoveStrength,
                        tag = "settings_opp_strength_switch",
                        onCheckedChange = onToggleShowOpponentMoveStrength
                    )
                }
            }

            // 7. المراجعة والتحليل المتقدم
            item {
                SettingsSectionCard(
                    title = "المراجعة والإعدادات المتقدمة",
                    icon = Icons.Rounded.AutoAwesome
                ) {
                    SettingsSwitchRow(
                        title = "المراجعة التلقائية بعد المباراة",
                        subtitle = "فتح تقرير الأخطاء والتحليل مباشرة بعد نهاية الدور",
                        checked = prefs.autoReviewAfterGame,
                        tag = "auto_review_switch",
                        onCheckedChange = onToggleAutoReview
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        title = "تلوين الأسهم حسب القوة",
                        subtitle = "تمييز أسهم الخطوط بألوان تدريجية حسب الأفضلية",
                        checked = prefs.arrowStrengthColors,
                        tag = "arrow_strength_colors_switch",
                        onCheckedChange = onToggleArrowStrengthColors
                    )
                    SettingsSwitchRow(
                        title = "أرقام ترتيب أسهم MultiPV",
                        subtitle = "عرض أرقام ١..٥ داخل رأس كل سهم تحليل",
                        checked = prefs.showMultiPvArrowNumbers,
                        tag = "multipv_numbers_switch",
                        onCheckedChange = onToggleShowMultiPvNumbers
                    )
                    SettingsSwitchRow(
                        title = "سهم أفضل نقلة سابقة (شبح)",
                        subtitle = "إظهار سهم شفاف لأفضل نقلة متاحة قبل النقلة المختارة في المراجعة",
                        checked = prefs.showPreviousBestGhostArrow,
                        tag = "ghost_arrow_switch",
                        onCheckedChange = onTogglePreviousBestGhostArrow
                    )
                    SettingsSwitchRow(
                        title = "تلوين آخر نقلة حسب الجودة",
                        subtitle = "تمييز خانة الوصول وشارة الجودة (!! ★ ?! ? ??)",
                        checked = prefs.colorLastMoveByQuality,
                        tag = "color_last_move_quality_switch",
                        onCheckedChange = onToggleColorLastMoveByQuality
                    )
                    SettingsSwitchRow(
                        title = "الترميز التصويري للقطع (Figurine SAN)",
                        subtitle = "عرض رموز ♔♕♖♗♘ بدلاً من الحروف الإنجليزية في السجل",
                        checked = prefs.figurineNotation,
                        tag = "figurine_notation_switch",
                        onCheckedChange = onToggleFigurineNotation
                    )
                    SettingsSwitchRow(
                        title = "إظهار القطع المأسورة والأفضلية",
                        subtitle = "عرض القطع المأسورة وفارق المادة بجانب أسماء اللاعبين",
                        checked = prefs.showCapturedPieces,
                        tag = "show_captured_pieces_switch",
                        onCheckedChange = onToggleShowCapturedPieces
                    )
                    SettingsSwitchRow(
                        title = "إحداثيات الرقعة (a-h / 1-8)",
                        subtitle = "عرض حروف وأرقام المربعات على أطراف الرقعة",
                        checked = prefs.showCoordinates,
                        tag = "show_coordinates_switch",
                        onCheckedChange = onToggleShowCoordinates
                    )
                    SettingsSwitchRow(
                        title = "النقر فقط (تعطيل السحب)",
                        subtitle = "تثبيت اللعب بطريقة اختيار المربع ثم الوجهة",
                        checked = prefs.tapOnlyDisableDrag,
                        tag = "tap_only_switch",
                        onCheckedChange = onToggleTapOnlyDisableDrag
                    )
                    SettingsSwitchRow(
                        title = "تكبير القطعة عند التحديد",
                        subtitle = "تكبير بصري لطيف للقطعة المختارة لتسهيل الرؤية",
                        checked = prefs.enlargeOnDrag,
                        tag = "enlarge_on_drag_switch",
                        onCheckedChange = onToggleEnlargeOnDrag
                    )
                    SettingsSwitchRow(
                        title = "متوسط خسارة السنتيبيدق (ACPL) في التقرير",
                        subtitle = "إظهار إحصائية ACPL لك وللخصم في تقرير المباراة",
                        checked = prefs.showAverageCplInReport,
                        tag = "show_acpl_report_switch",
                        onCheckedChange = onToggleShowAverageCplInReport
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Text(
                        text = "نسبة حجم الرقعة: ${formatArabicNumber(prefs.boardScalePercent)}٪",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = prefs.boardScalePercent.toFloat(),
                        onValueChange = { onChangeBoardScalePercent(it.roundToInt().coerceIn(85, 100)) },
                        valueRange = 85f..100f,
                        steps = 14,
                        modifier = Modifier.testTag("board_scale_slider")
                    )
                }
            }

            // 7. حول رُقعة والتراخيص
            item {
                SettingsSectionCard(
                    title = "حول رُقعة والتراخيص",
                    icon = Icons.Rounded.Info
                ) {
                    Text(
                        text = "رُقعة (Ruqaa) • الإصدار ٣.٢.٣ — شطرنج عربي متكامل يعمل ١٠٠٪ دون إنترنت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onOpenAboutLicenses,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("open_about_licenses_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("عرض تراخيص Stockfish GPLv3 والمؤلفين وخط IBM Plex")
                    }
                }
            }
        }
    }

    if (showStockfishHelpDialog) {
        AlertDialog(
            onDismissRequest = { showStockfishHelpDialog = false },
            title = {
                Text(
                    text = "بنية محرك Stockfish UCI",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val statusDesc = if (stockfishStatus.isNativeStockfishReady) {
                    "الحالة: متصل (${stockfishStatus.engineName})\nالمسار: ${stockfishStatus.nativeBinaryPath ?: "-"}\nالشبكة العصبية: nn-1a298aa575a0.nnue مدمجة وجاهزة."
                } else {
                    "الحالة: ${stockfishStatus.diagnosticMessageAr}"
                }
                Text(
                    text = "$statusDesc\n\nيدعم التطبيق محرك Stockfish 19 الأصلي (arm64-v8a و x86_64) عبر بروتوكول UCI الكامل مع شبكة NNUE وذاكرة Hash ٣٢ ميجابايت ومهل انتظار ٤٥/٢٠ ثانية.",
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
            .heightIn(min = 48.dp)
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
        modifier = modifier.heightIn(min = 48.dp),
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
