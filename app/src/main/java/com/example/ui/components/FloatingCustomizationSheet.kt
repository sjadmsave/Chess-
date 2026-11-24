package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ChessBoardState
import com.example.chess.ChessPiece
import com.example.chess.FloatingBubbleSize
import com.example.chess.FloatingFrameShape
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.data.UserPreferences
import com.example.ui.theme.formatArabicNumber

@Composable
fun FloatingCustomizationSheetContent(
    prefs: UserPreferences,
    onSave: (
        widthDp: Int,
        frameShape: FloatingFrameShape,
        opacityPercent: Int,
        bubbleSize: FloatingBubbleSize
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var widthDp by remember { mutableIntStateOf(prefs.floatingWidthDp.coerceIn(180, 560)) }
    var frameShape by remember { mutableStateOf(prefs.floatingFrameShape) }
    var opacityPercent by remember { mutableIntStateOf(prefs.floatingOpacityPercent.coerceIn(65, 100)) }
    var bubbleSize by remember { mutableStateOf(prefs.floatingBubbleSize) }

    val previewBoard = remember {
        ChessBoardState.fromFen("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "تخصيص النافذة العائمة",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Live Floating Overlay Preview Card
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "معاينة النافذة",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewFraction = ((widthDp - 180) / 380f).coerceIn(0f, 1f)
                        val previewBoxSize = (122 + (previewFraction * 66)).dp
                        Surface(
                            modifier = Modifier
                                .size(width = previewBoxSize, height = previewBoxSize + 52.dp)
                                .alpha(opacityPercent / 100f),
                            shape = RoundedCornerShape(frameShape.cornerDp.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(5.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("•••", "✎", "↩", "⇅", "⋮⋮").forEachIndexed { idx, sym ->
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 3.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = sym,
                                                    fontSize = 9.sp,
                                                    color = if (idx == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape((frameShape.cornerDp - 3).coerceAtLeast(0).dp))
                                ) {
                                    ChessBoardView(
                                        boardState = previewBoard,
                                        boardTheme = prefs.boardTheme,
                                        pieceStyle = prefs.pieceStyle,
                                        selectedSquare = null,
                                        legalMovesForSelected = emptyList(),
                                        lastMove = null,
                                        hintMove = null,
                                        flipped = false,
                                        interactive = false,
                                        onSquareClick = {}
                                    )
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "−  +  ▢",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "دورك",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "◢",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Width Slider (180..560 dp) + Presets
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "عرض النافذة • ${formatArabicNumber(widthDp)} dp",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = widthDp.toFloat(),
                    onValueChange = { widthDp = it.toInt().coerceIn(180, 560) },
                    valueRange = 180f..560f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("صغير" to 210, "متوسط" to 270, "كبير" to 380, "عريض" to 480).forEach { (label, w) ->
                        val selected = kotlin.math.abs(widthDp - w) <= 28
                        SelectablePillButton(
                            label = label,
                            selected = selected,
                            onClick = { widthDp = w },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = "العرض المدعوم من ${formatArabicNumber(180)} إلى ${formatArabicNumber(560)} dp ويمكن تغييره بالسحب من مقبض الزاوية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Frame Shape (مستديرة 12dp، مدمجة 7dp، مربعة 0dp)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("شكل الإطار", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingFrameShape.entries.forEach { shape ->
                        SelectablePillButton(
                            label = shape.nameAr,
                            selected = frameShape == shape,
                            onClick = { frameShape = shape },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Opacity Slider (65%..100%)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "عتامة الإطار • ${formatArabicNumber(opacityPercent)}٪",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = opacityPercent.toFloat(),
                    onValueChange = { opacityPercent = it.toInt().coerceIn(65, 100) },
                    valueRange = 65f..100f
                )
            }
        }

        // Bubble Size + Live Bubble Icon (44 / 52 / 64 dp)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "حجم الفقاعة (${formatArabicNumber(bubbleSize.sizeDp)} dp)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size((bubbleSize.sizeDp * 0.72f).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ChessPieceGraphic(
                            piece = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE),
                            pieceSize = (bubbleSize.sizeDp * 0.45f).dp
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingBubbleSize.entries.forEach { bs ->
                        SelectablePillButton(
                            label = bs.nameAr,
                            selected = bubbleSize == bs,
                            onClick = { bubbleSize = bs },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Footer: Default (Ruqaa Section 11/13) / Cancel / Save
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        widthDp = 270
                        frameShape = FloatingFrameShape.ROUNDED
                        opacityPercent = 100
                        bubbleSize = FloatingBubbleSize.MEDIUM
                    }
                ) {
                    Text("افتراضي")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            onSave(widthDp, frameShape, opacityPercent, bubbleSize)
                            onDismiss()
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("save_floating_custom_btn")
                    ) {
                        Text("حفظ")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectablePillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
