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
import androidx.compose.foundation.layout.height
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
import com.example.ui.theme.MonoFontFamily

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
    var widthDp by remember { mutableIntStateOf(prefs.floatingWidthDp) }
    var frameShape by remember { mutableStateOf(prefs.floatingFrameShape) }
    var opacityPercent by remember { mutableIntStateOf(prefs.floatingOpacityPercent) }
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
                        val previewFraction = ((widthDp - 160) / 160f).coerceIn(0f, 1f)
                        val previewBoxSize = (118 + (previewFraction * 44)).dp
                        Surface(
                            modifier = Modifier
                                .size(width = previewBoxSize, height = previewBoxSize + 24.dp)
                                .alpha(opacityPercent / 100f),
                            shape = RoundedCornerShape(frameShape.cornerDp.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(5.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    listOf("•••", "✎", "↩", "⇅", "⋮⋮").forEach { sym ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF1F5F9),
                                            border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                                        ) {
                                            Text(
                                                text = sym,
                                                fontSize = 9.sp,
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                            }
                        }
                    }
                }
            }
        }

        // Width Slider + Presets
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "عرض النافذة • $widthDp",
                    fontFamily = MonoFontFamily,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = widthDp.toFloat(),
                    onValueChange = { widthDp = it.toInt() },
                    valueRange = 160f..320f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("صغير" to 189, "متوسط" to 235, "كبير" to 290).forEach { (label, w) ->
                        val selected = kotlin.math.abs(widthDp - w) <= 18
                        SelectablePillButton(
                            label = label,
                            selected = selected,
                            onClick = { widthDp = w },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = "يُحفظ العرض المختار وتلائم النافذة المساحة المتاحة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Frame Shape
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

        // Opacity Slider
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "عتامة الإطار • $opacityPercent%",
                    fontFamily = MonoFontFamily,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = opacityPercent.toFloat(),
                    onValueChange = { opacityPercent = it.toInt() },
                    valueRange = 35f..100f
                )
            }
        }

        // Bubble Size + Live Bubble Icon
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("حجم الفقاعة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .size((bubbleSize.sizeDp * 0.65f).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ChessPieceGraphic(
                            piece = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE),
                            pieceSize = (bubbleSize.sizeDp * 0.42f).dp
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

        // Footer: Default / Cancel / Save
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        widthDp = 220
                        frameShape = FloatingFrameShape.COMPACT
                        opacityPercent = 85
                        bubbleSize = FloatingBubbleSize.SMALL
                    }
                ) {
                    Text("افتراضي")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            onSave(widthDp, frameShape, opacityPercent, bubbleSize)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_floating_custom_btn")
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
        modifier = modifier,
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
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
