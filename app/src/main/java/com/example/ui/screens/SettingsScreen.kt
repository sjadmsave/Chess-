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
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
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
    onChangeSoundVolume: (Int) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSelectVibrationStyle: (VibrationStyle) -> Unit,
    onToggleAutoReview: (Boolean) -> Unit,
    onUpdateReminders: (Boolean, Int, Int) -> Unit,
    onTestNotification: () -> Unit,
    onImportSyzygyFiles: (List<Uri>) -> Unit,
    onDeleteSyzygyTable: (String) -> Unit,
    onClearSyzygyTables: () -> Unit,
    onOpenAboutLicenses: () -> Unit
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

            // 2. محرك الشطرنج (Stockfish UCI + 5 Tiers + 13 Personalities)
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
                        title = "شخصية المحرك والشدّة والمخاطرة",
                        subtitle = "${prefs.enginePersonality.nameAr} • شدّة ${formatArabicNumber(prefs.personalityIntensity)}٪ • ${prefs.personalityRisk.nameAr}",
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
                            Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
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
                        subtitle = "٣ طوابع (ناعم، خشبي، رقمي) لكل أحداث اللعب",
                        checked = prefs.soundEnabled,
                        tag = "sound_switch",
                        onCheckedChange = onToggleSound
                    )
                    if (prefs.soundEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SoundProfile.entries.forEach { sp ->
                                FilterChip(
                                    selected = prefs.soundProfile == sp,
                                    onClick = { onSelectSoundProfile(sp) },
                                    label = { Text(sp.nameAr) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sound_profile_${sp.id}")
                                )
                            }
                        }
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

            // 6. المراجعة والتلميحات
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
                Text(
                    text = "يدعم التطبيق بروتوكول UCI الكامل لمحرك Stockfish مع إدارة خيط واحد مخصّص ومهل انتظار دقيقة (٤٥ ثانية للبدء، ٢٠ ثانية للاستعداد، وذاكرة Hash ٣٢ ميجابايت).\n\n" +
                        "عند بناء الحزمة عبر GitHub Actions تُدمج مكتبات arm64-v8a و x86_64، وفي بيئات المعاينة يعمل المحرك الاحتياطي المدمج تلقائياً بنفس صيغ الإيلو والشخصيات الـ ١٣.",
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
