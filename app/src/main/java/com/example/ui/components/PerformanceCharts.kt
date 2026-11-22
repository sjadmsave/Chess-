package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RatingPointEntity
import com.example.ui.theme.MonoFontFamily
import kotlin.math.max
import kotlin.math.min

@Composable
fun EvaluationBar(
    evalWhiteCp: Int,
    flipped: Boolean,
    modifier: Modifier = Modifier
) {
    // Sigmoid-like clamping to [-800..800] cp for smooth visual fraction
    val clamped = evalWhiteCp.coerceIn(-900, 900)
    val rawWhiteFraction = 0.5f + (clamped / 1800f)
    val whiteFraction by animateFloatAsState(
        targetValue = rawWhiteFraction.coerceIn(0.06f, 0.94f),
        animationSpec = tween(350),
        label = "evalFraction"
    )
    val evalLabel = when {
        evalWhiteCp >= 90000 -> "M"
        evalWhiteCp <= -90000 -> "-M"
        else -> {
            val pawns = evalWhiteCp / 100f
            val sign = if (pawns > 0.05f) "+" else ""
            String.format(java.util.Locale.US, "%s%.1f", sign, pawns)
        }
    }

    Column(
        modifier = modifier
            .width(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val topIsBlack = !flipped
        val topWeight = if (topIsBlack) (1f - whiteFraction) else whiteFraction
        val bottomWeight = 1f - topWeight
        val topColor = if (topIsBlack) Color(0xFF1E293B) else Color(0xFFF8FAFC)
        val bottomColor = if (topIsBlack) Color(0xFFF8FAFC) else Color(0xFF1E293B)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(topWeight.coerceAtLeast(0.05f))
                .background(topColor),
            contentAlignment = Alignment.TopCenter
        ) {
            if (evalWhiteCp < 0) {
                Text(
                    text = evalLabel,
                    fontFamily = MonoFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (topIsBlack) Color(0xFFE2E8F0) else Color(0xFF0F172A),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(bottomWeight.coerceAtLeast(0.05f))
                .background(bottomColor),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (evalWhiteCp >= 0) {
                Text(
                    text = evalLabel,
                    fontFamily = MonoFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (topIsBlack) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
fun EloProgressionChart(
    points: List<RatingPointEntity>,
    currentElo: Int,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val pointValues = if (points.isEmpty()) listOf(currentElo) else points.map { it.elo }

    val minVal = max(400, (pointValues.minOrNull() ?: currentElo) - 60)
    val maxVal = min(2800, (pointValues.maxOrNull() ?: currentElo) + 60)
    val span = (maxVal - minVal).coerceAtLeast(100)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "الأعلى: ${pointValues.maxOrNull() ?: currentElo}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "الحالي: $currentElo ELO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(top = 10.dp)
        ) {
            val w = size.width
            val h = size.height
            val padX = 14f
            val padY = 14f
            val drawW = (w - padX * 2).coerceAtLeast(1f)
            val drawH = (h - padY * 2).coerceAtLeast(1f)

            // Horizontal reference lines
            for (i in 0..3) {
                val y = padY + (drawH * i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(padX, y),
                    end = Offset(w - padX, y),
                    strokeWidth = 1.5f
                )
            }

            val coords = if (pointValues.size == 1) {
                val y = padY + drawH * (1f - (pointValues[0] - minVal).toFloat() / span)
                listOf(Offset(padX, y), Offset(w - padX, y))
            } else {
                pointValues.mapIndexed { idx, value ->
                    val x = padX + drawW * idx.toFloat() / (pointValues.size - 1).toFloat()
                    val y = padY + drawH * (1f - (value - minVal).toFloat() / span)
                    Offset(x, y)
                }
            }

            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    val prev = coords[i - 1]
                    val curr = coords[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }

            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(coords.last().x, h - padY)
                lineTo(coords.first().x, h - padY)
                close()
            }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.32f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            coords.forEach { pt ->
                drawCircle(color = lineColor, radius = 7f, center = pt)
                drawCircle(color = Color.White, radius = 3.2f, center = pt)
            }
        }
    }
}

@Composable
fun AccuracyTrendBars(
    points: List<RatingPointEntity>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.tertiary
    val recent = points.takeLast(10)
    if (recent.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        recent.forEachIndexed { idx, pt ->
            val frac = (pt.accuracy / 100f).coerceIn(0.15f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "${pt.accuracy}%",
                    fontFamily = MonoFontFamily,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(frac * 0.72f)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (pt.won) MaterialTheme.colorScheme.primary else barColor.copy(alpha = 0.75f)
                        )
                )
                Text(
                    text = "#${idx + 1}",
                    fontFamily = MonoFontFamily,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PhaseGaugeRow(
    labelAr: String,
    accuracy: Int,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = labelAr, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "$accuracy%",
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = color,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        LinearProgressIndicator(
            progress = { (accuracy / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
