package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.chess.BoardTheme
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.PieceType
import com.example.data.UserPreferences
import com.example.ui.components.ChessPieceGraphic
import com.example.ui.theme.AppThemeMode

@Composable
fun SettingsSheetContent(
    prefs: UserPreferences,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectBoardTheme: (BoardTheme) -> Unit,
    onSelectPieceStyle: (PieceStyle) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleEvalBar: (Boolean) -> Unit,
    onUpdateReminders: (Boolean, Int, Int) -> Unit,
    onTestNotification: () -> Unit
) {
    val context = LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onUpdateReminders(true, prefs.reminderHour, prefs.reminderMinute)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("التخصيص والإعدادات", style = MaterialTheme.typography.headlineSmall)
        }

        // Theme Mode (Dark / Light / System)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مظهر التطبيق (الوضع الليلي / النهاري)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = prefs.themeMode == mode,
                            onClick = { onSelectThemeMode(mode) },
                            label = { Text(mode.labelAr) }
                        )
                    }
                }
            }
        }

        // Artistic Board Themes
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("التصميم الفني لرقعة اللعب", style = MaterialTheme.typography.titleSmall)
                }
                BoardTheme.entries.chunked(2).forEach { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowThemes.forEach { theme ->
                            val selected = prefs.boardTheme == theme
                            Card(
                                onClick = { onSelectBoardTheme(theme) },
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Mini 2x2 board swatch
                                    Column(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.5.dp, theme.boardBorder, RoundedCornerShape(6.dp))
                                    ) {
                                        Row(Modifier.weight(1f)) {
                                            Box(Modifier.weight(1f).height(17.dp).background(theme.lightSquare))
                                            Box(Modifier.weight(1f).height(17.dp).background(theme.darkSquare))
                                        }
                                        Row(Modifier.weight(1f)) {
                                            Box(Modifier.weight(1f).height(17.dp).background(theme.darkSquare))
                                            Box(Modifier.weight(1f).height(17.dp).background(theme.lightSquare))
                                        }
                                    }
                                    Text(
                                        text = theme.nameAr,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // Artistic Piece Styles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تصميم وشكل القطع", style = MaterialTheme.typography.titleSmall)
                PieceStyle.entries.forEach { style ->
                    val selected = prefs.pieceStyle == style
                    Card(
                        onClick = { onSelectPieceStyle(style) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(style.nameAr, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    style.subtitleAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(prefs.boardTheme.darkSquare.copy(alpha = 0.85f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChessPieceGraphic(
                                    piece = ChessPiece(PieceType.KING, PieceColor.WHITE),
                                    pieceSize = 30.dp,
                                    pieceStyle = style
                                )
                                ChessPieceGraphic(
                                    piece = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK),
                                    pieceSize = 30.dp,
                                    pieceStyle = style
                                )
                            }
                        }
                    }
                }
            }
        }

        // Audio, Haptics & Eval Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("التفاعل والصوت والتحليل", style = MaterialTheme.typography.titleSmall)
                    }
                    SettingSwitchRow(
                        title = "المؤثرات الصوتية أثناء اللعب",
                        subtitle = "أصوات تحريك القطع والأسر والكش",
                        checked = prefs.soundEnabled,
                        onCheckedChange = onToggleSound
                    )
                    SettingSwitchRow(
                        title = "الاهتزاز التفاعلي",
                        subtitle = "استجابة لمسية عند اختيار ونقل القطع",
                        checked = prefs.hapticsEnabled,
                        onCheckedChange = onToggleHaptics
                    )
                    SettingSwitchRow(
                        title = "شريط التقييم الحي",
                        subtitle = "عرض أفضلية الأبيض والأسود مباشرة بجانب الرقعة",
                        checked = prefs.showEvalBar,
                        onCheckedChange = onToggleEvalBar
                    )
                }
            }
        }

        // Smart Daily Motivational Reminders
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("التنبيهات الذكية والتحفيز اليومي", style = MaterialTheme.typography.titleSmall)
                    }
                    SettingSwitchRow(
                        title = "تفعيل التذكير اليومي الذكي",
                        subtitle = "إشعار يومي لحفظ سلسلة التدريب وتحدي الألغاز",
                        checked = prefs.remindersEnabled,
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

                    val times = listOf(9 to "9:00 ص", 16 to "4:00 م", 19 to "7:00 م", 21 to "9:00 م")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        times.forEach { (hr, label) ->
                            FilterChip(
                                selected = prefs.reminderHour == hr,
                                onClick = { onUpdateReminders(prefs.remindersEnabled, hr, 0) },
                                label = { Text(label) }
                            )
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
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
