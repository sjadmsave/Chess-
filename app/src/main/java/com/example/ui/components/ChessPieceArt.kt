package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.PieceType

@Composable
fun ChessPieceGraphic(
    piece: ChessPiece,
    modifier: Modifier = Modifier,
    pieceSize: Dp? = null,
    pieceStyle: PieceStyle = PieceStyle.CLASSIC_STAUNTON
) {
    val sizeMod = if (pieceSize != null) modifier.size(pieceSize) else modifier
    val colorName = if (piece.color == PieceColor.WHITE) "أبيض" else "أسود"
    Canvas(
        modifier = sizeMod.semantics {
            contentDescription = "${piece.type.nameAr} $colorName"
        }
    ) {
        drawChessPiece(piece, pieceStyle)
    }
}

private data class PiecePalette(
    val fillBrush: Brush,
    val strokeColor: Color,
    val detailColor: Color,
    val jewelColor: Color? = null,
    val glowColor: Color? = null
)

private fun paletteFor(isWhite: Boolean, style: PieceStyle): PiecePalette = when (style) {
    PieceStyle.CLASSIC_STAUNTON -> if (isWhite) {
        PiecePalette(
            fillBrush = Brush.verticalGradient(listOf(Color(0xFFFFFDF7), Color(0xFFE9E2D0), Color(0xFFD5CAB2))),
            strokeColor = Color(0xFF2B2620),
            detailColor = Color(0xFF473E33)
        )
    } else {
        PiecePalette(
            fillBrush = Brush.verticalGradient(listOf(Color(0xFF3E4554), Color(0xFF1F242E), Color(0xFF10131A))),
            strokeColor = Color(0xFFE2E8F0),
            detailColor = Color(0xFFCBD5E1)
        )
    }

    PieceStyle.MODERN_GEOMETRIC -> if (isWhite) {
        PiecePalette(
            fillBrush = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0))),
            strokeColor = Color(0xFF0F172A),
            detailColor = Color(0xFF334155)
        )
    } else {
        PiecePalette(
            fillBrush = Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF0F172A))),
            strokeColor = Color(0xFF94A3B8),
            detailColor = Color(0xFFE2E8F0)
        )
    }

    PieceStyle.HIGH_CONTRAST -> if (isWhite) {
        PiecePalette(
            fillBrush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))),
            strokeColor = Color(0xFF000000),
            detailColor = Color(0xFF0F172A)
        )
    } else {
        PiecePalette(
            fillBrush = Brush.verticalGradient(listOf(Color(0xFF111827), Color(0xFF000000))),
            strokeColor = Color(0xFFFFFFFF),
            detailColor = Color(0xFFF8FAFC)
        )
    }

    PieceStyle.ROYAL_HERITAGE -> if (isWhite) {
        PiecePalette(
            fillBrush = Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFD97706))),
            strokeColor = Color(0xFF451A03),
            detailColor = Color(0xFF78350F),
            jewelColor = Color(0xFF059669)
        )
    } else {
        PiecePalette(
            fillBrush = Brush.verticalGradient(listOf(Color(0xFF4C1D24), Color(0xFF280B12), Color(0xFF140508))),
            strokeColor = Color(0xFFFBBF24),
            detailColor = Color(0xFFFDE68A),
            jewelColor = Color(0xFFEF4444)
        )
    }

    PieceStyle.WOOD_CARVED -> if (isWhite) {
        PiecePalette(
            fillBrush = Brush.verticalGradient(listOf(Color(0xFFF9E5C0), Color(0xFFE3BC82), Color(0xFFC29050))),
            strokeColor = Color(0xFF4A2C11),
            detailColor = Color(0xFF6B3E1B)
        )
    } else {
        PiecePalette(
            fillBrush = Brush.verticalGradient(listOf(Color(0xFF7A4428), Color(0xFF512814), Color(0xFF2E1409))),
            strokeColor = Color(0xFFEBD3B0),
            detailColor = Color(0xFFD6B080)
        )
    }

    PieceStyle.NEON_CYBER -> if (isWhite) {
        PiecePalette(
            fillBrush = Brush.verticalGradient(listOf(Color(0xFFECFEFF), Color(0xFFA5F3FC), Color(0xFF22D3EE))),
            strokeColor = Color(0xFF0E7490),
            detailColor = Color(0xFF155E75),
            glowColor = Color(0x5522D3EE)
        )
    } else {
        PiecePalette(
            fillBrush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))),
            strokeColor = Color(0xFFFBBF24),
            detailColor = Color(0xFF38BDF8),
            glowColor = Color(0x55FBBF24)
        )
    }
}

private fun DrawScope.drawChessPiece(piece: ChessPiece, style: PieceStyle) {
    val w = size.width
    val h = size.height
    val isWhite = piece.color == PieceColor.WHITE
    val pal = paletteFor(isWhite, style)
    val sw = (w * if (style == PieceStyle.MODERN_GEOMETRIC) 0.052f else 0.045f).coerceAtLeast(1.4f)

    // Shadow or Cyber Glow
    if (pal.glowColor != null) {
        drawCircle(
            color = pal.glowColor,
            radius = w * 0.42f,
            center = Offset(w * 0.50f, h * 0.52f)
        )
    } else {
        drawOval(
            color = Color(0x33000000),
            topLeft = Offset(w * 0.16f, h * 0.85f),
            size = Size(w * 0.68f, h * 0.10f)
        )
    }

    if (style == PieceStyle.MODERN_GEOMETRIC) {
        drawGeometricPiece(piece.type, w, h, pal, sw)
        return
    }

    when (piece.type) {
        PieceType.PAWN -> drawPawn(w, h, pal, sw, style)
        PieceType.ROOK -> drawRook(w, h, pal, sw, style)
        PieceType.KNIGHT -> drawKnight(w, h, pal, sw, style)
        PieceType.BISHOP -> drawBishop(w, h, pal, sw, style)
        PieceType.QUEEN -> drawQueen(w, h, pal, sw, style)
        PieceType.KING -> drawKing(w, h, pal, sw, style)
    }
}

private fun DrawScope.drawGeometricPiece(
    type: PieceType,
    w: Float,
    h: Float,
    pal: PiecePalette,
    sw: Float
) {
    // Crisp architectural base bar
    drawRect(
        brush = pal.fillBrush,
        topLeft = Offset(w * 0.20f, h * 0.80f),
        size = Size(w * 0.60f, h * 0.10f)
    )
    drawRect(
        color = pal.strokeColor,
        topLeft = Offset(w * 0.20f, h * 0.80f),
        size = Size(w * 0.60f, h * 0.10f),
        style = Stroke(sw)
    )

    val shape = Path().apply {
        when (type) {
            PieceType.PAWN -> {
                moveTo(w * 0.28f, h * 0.80f)
                lineTo(w * 0.50f, h * 0.40f)
                lineTo(w * 0.72f, h * 0.80f)
                close()
            }
            PieceType.ROOK -> {
                moveTo(w * 0.26f, h * 0.80f)
                lineTo(w * 0.30f, h * 0.20f)
                lineTo(w * 0.42f, h * 0.20f)
                lineTo(w * 0.42f, h * 0.30f)
                lineTo(w * 0.58f, h * 0.30f)
                lineTo(w * 0.58f, h * 0.20f)
                lineTo(w * 0.70f, h * 0.20f)
                lineTo(w * 0.74f, h * 0.80f)
                close()
            }
            PieceType.KNIGHT -> {
                moveTo(w * 0.26f, h * 0.80f)
                lineTo(w * 0.38f, h * 0.46f)
                lineTo(w * 0.20f, h * 0.42f)
                lineTo(w * 0.42f, h * 0.14f)
                lineTo(w * 0.66f, h * 0.24f)
                lineTo(w * 0.74f, h * 0.80f)
                close()
            }
            PieceType.BISHOP -> {
                moveTo(w * 0.27f, h * 0.80f)
                lineTo(w * 0.36f, h * 0.38f)
                lineTo(w * 0.50f, h * 0.12f)
                lineTo(w * 0.64f, h * 0.38f)
                lineTo(w * 0.73f, h * 0.80f)
                close()
            }
            PieceType.QUEEN -> {
                moveTo(w * 0.24f, h * 0.80f)
                lineTo(w * 0.16f, h * 0.24f)
                lineTo(w * 0.38f, h * 0.44f)
                lineTo(w * 0.50f, h * 0.14f)
                lineTo(w * 0.62f, h * 0.44f)
                lineTo(w * 0.84f, h * 0.24f)
                lineTo(w * 0.76f, h * 0.80f)
                close()
            }
            PieceType.KING -> {
                moveTo(w * 0.24f, h * 0.80f)
                lineTo(w * 0.26f, h * 0.32f)
                lineTo(w * 0.50f, h * 0.22f)
                lineTo(w * 0.74f, h * 0.32f)
                lineTo(w * 0.76f, h * 0.80f)
                close()
            }
        }
    }
    drawPath(shape, pal.fillBrush)
    drawPath(shape, pal.strokeColor, style = Stroke(sw, join = StrokeJoin.Miter))

    when (type) {
        PieceType.PAWN -> {
            drawCircle(pal.fillBrush, radius = w * 0.13f, center = Offset(w * 0.50f, h * 0.26f))
            drawCircle(pal.strokeColor, radius = w * 0.13f, center = Offset(w * 0.50f, h * 0.26f), style = Stroke(sw))
        }
        PieceType.BISHOP -> {
            drawLine(pal.detailColor, Offset(w * 0.44f, h * 0.44f), Offset(w * 0.58f, h * 0.28f), strokeWidth = sw)
        }
        PieceType.KNIGHT -> {
            drawCircle(pal.detailColor, radius = w * 0.035f, center = Offset(w * 0.42f, h * 0.28f))
        }
        PieceType.KING -> {
            drawLine(pal.strokeColor, Offset(w * 0.50f, h * 0.06f), Offset(w * 0.50f, h * 0.22f), strokeWidth = sw * 1.4f)
            drawLine(pal.strokeColor, Offset(w * 0.40f, h * 0.13f), Offset(w * 0.60f, h * 0.13f), strokeWidth = sw * 1.4f)
        }
        else -> Unit
    }
}

private fun DrawScope.drawBase(
    w: Float,
    h: Float,
    pal: PiecePalette,
    sw: Float,
    style: PieceStyle,
    widthFactor: Float = 0.64f
) {
    val left = w * (1f - widthFactor) / 2f
    val bw = w * widthFactor
    if (style == PieceStyle.ROYAL_HERITAGE) {
        drawRoundRect(
            brush = pal.fillBrush,
            topLeft = Offset(left - w * 0.03f, h * 0.81f),
            size = Size(bw + w * 0.06f, h * 0.09f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        drawRoundRect(
            color = pal.strokeColor,
            topLeft = Offset(left - w * 0.03f, h * 0.81f),
            size = Size(bw + w * 0.06f, h * 0.09f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
            style = Stroke(width = sw, join = StrokeJoin.Round)
        )
    }
    drawRoundRect(
        brush = pal.fillBrush,
        topLeft = Offset(left, h * 0.74f),
        size = Size(bw, h * 0.11f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
    )
    drawRoundRect(
        color = pal.strokeColor,
        topLeft = Offset(left, h * 0.74f),
        size = Size(bw, h * 0.11f),
        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
        style = Stroke(width = sw, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawPawn(w: Float, h: Float, pal: PiecePalette, sw: Float, style: PieceStyle) {
    val body = Path().apply {
        moveTo(w * 0.30f, h * 0.75f)
        cubicTo(w * 0.34f, h * 0.60f, w * 0.40f, h * 0.48f, w * 0.41f, h * 0.42f)
        lineTo(w * 0.59f, h * 0.42f)
        cubicTo(w * 0.60f, h * 0.48f, w * 0.66f, h * 0.60f, w * 0.70f, h * 0.75f)
        close()
    }
    drawPath(body, pal.fillBrush)
    drawPath(body, pal.strokeColor, style = Stroke(sw, join = StrokeJoin.Round))

    drawRoundRect(
        brush = pal.fillBrush,
        topLeft = Offset(w * 0.35f, h * 0.38f),
        size = Size(w * 0.30f, h * 0.06f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    drawRoundRect(
        color = pal.strokeColor,
        topLeft = Offset(w * 0.35f, h * 0.38f),
        size = Size(w * 0.30f, h * 0.06f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
        style = Stroke(sw * 0.85f)
    )

    drawCircle(brush = pal.fillBrush, radius = w * 0.15f, center = Offset(w * 0.50f, h * 0.25f))
    drawCircle(color = pal.strokeColor, radius = w * 0.15f, center = Offset(w * 0.50f, h * 0.25f), style = Stroke(sw))
    if (pal.jewelColor != null) {
        drawCircle(color = pal.jewelColor, radius = w * 0.04f, center = Offset(w * 0.50f, h * 0.58f))
    }
    drawBase(w, h, pal, sw, style, widthFactor = 0.56f)
}

private fun DrawScope.drawRook(w: Float, h: Float, pal: PiecePalette, sw: Float, style: PieceStyle) {
    val tower = Path().apply {
        moveTo(w * 0.28f, h * 0.75f)
        lineTo(w * 0.32f, h * 0.35f)
        lineTo(w * 0.25f, h * 0.31f)
        lineTo(w * 0.25f, h * 0.15f)
        lineTo(w * 0.35f, h * 0.15f)
        lineTo(w * 0.35f, h * 0.23f)
        lineTo(w * 0.44f, h * 0.23f)
        lineTo(w * 0.44f, h * 0.15f)
        lineTo(w * 0.56f, h * 0.15f)
        lineTo(w * 0.56f, h * 0.23f)
        lineTo(w * 0.65f, h * 0.23f)
        lineTo(w * 0.65f, h * 0.15f)
        lineTo(w * 0.75f, h * 0.15f)
        lineTo(w * 0.75f, h * 0.31f)
        lineTo(w * 0.68f, h * 0.35f)
        lineTo(w * 0.72f, h * 0.75f)
        close()
    }
    drawPath(tower, pal.fillBrush)
    drawPath(tower, pal.strokeColor, style = Stroke(sw, join = StrokeJoin.Round))
    drawLine(pal.detailColor, Offset(w * 0.32f, h * 0.35f), Offset(w * 0.68f, h * 0.35f), strokeWidth = sw * 0.8f)
    if (style == PieceStyle.WOOD_CARVED) {
        drawLine(pal.detailColor, Offset(w * 0.34f, h * 0.55f), Offset(w * 0.66f, h * 0.55f), strokeWidth = sw * 0.7f)
    }
    if (pal.jewelColor != null) {
        drawCircle(color = pal.jewelColor, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.52f))
    }
    drawBase(w, h, pal, sw, style, widthFactor = 0.62f)
}

private fun DrawScope.drawKnight(w: Float, h: Float, pal: PiecePalette, sw: Float, style: PieceStyle) {
    val horse = Path().apply {
        moveTo(w * 0.27f, h * 0.75f)
        cubicTo(w * 0.28f, h * 0.56f, w * 0.38f, h * 0.46f, w * 0.44f, h * 0.38f)
        cubicTo(w * 0.34f, h * 0.41f, w * 0.24f, h * 0.43f, w * 0.20f, h * 0.38f)
        cubicTo(w * 0.17f, h * 0.33f, w * 0.26f, h * 0.24f, w * 0.36f, h * 0.18f)
        lineTo(w * 0.40f, h * 0.10f)
        lineTo(w * 0.49f, h * 0.16f)
        lineTo(w * 0.55f, h * 0.10f)
        cubicTo(w * 0.72f, h * 0.20f, w * 0.77f, h * 0.46f, w * 0.73f, h * 0.75f)
        close()
    }
    drawPath(horse, pal.fillBrush)
    drawPath(horse, pal.strokeColor, style = Stroke(sw, join = StrokeJoin.Round))
    drawCircle(color = pal.jewelColor ?: pal.detailColor, radius = w * 0.032f, center = Offset(w * 0.39f, h * 0.26f))
    drawLine(
        color = pal.detailColor,
        start = Offset(w * 0.60f, h * 0.22f),
        end = Offset(w * 0.65f, h * 0.58f),
        strokeWidth = sw * 0.75f,
        cap = StrokeCap.Round
    )
    drawBase(w, h, pal, sw, style, widthFactor = 0.64f)
}

private fun DrawScope.drawBishop(w: Float, h: Float, pal: PiecePalette, sw: Float, style: PieceStyle) {
    val mitre = Path().apply {
        moveTo(w * 0.28f, h * 0.75f)
        cubicTo(w * 0.33f, h * 0.58f, w * 0.38f, h * 0.50f, w * 0.36f, h * 0.40f)
        cubicTo(w * 0.32f, h * 0.28f, w * 0.41f, h * 0.18f, w * 0.50f, h * 0.14f)
        cubicTo(w * 0.59f, h * 0.18f, w * 0.68f, h * 0.28f, w * 0.64f, h * 0.40f)
        cubicTo(w * 0.62f, h * 0.50f, w * 0.67f, h * 0.58f, w * 0.72f, h * 0.75f)
        close()
    }
    drawPath(mitre, pal.fillBrush)
    drawPath(mitre, pal.strokeColor, style = Stroke(sw, join = StrokeJoin.Round))

    drawCircle(brush = pal.fillBrush, radius = w * 0.055f, center = Offset(w * 0.50f, h * 0.10f))
    drawCircle(color = pal.strokeColor, radius = w * 0.055f, center = Offset(w * 0.50f, h * 0.10f), style = Stroke(sw * 0.85f))

    drawLine(
        color = pal.detailColor,
        start = Offset(w * 0.44f, h * 0.34f),
        end = Offset(w * 0.57f, h * 0.23f),
        strokeWidth = sw * 0.9f,
        cap = StrokeCap.Round
    )
    drawLine(pal.detailColor, Offset(w * 0.37f, h * 0.46f), Offset(w * 0.63f, h * 0.46f), strokeWidth = sw * 0.8f)
    if (pal.jewelColor != null) {
        drawCircle(color = pal.jewelColor, radius = w * 0.04f, center = Offset(w * 0.50f, h * 0.60f))
    }
    drawBase(w, h, pal, sw, style, widthFactor = 0.62f)
}

private fun DrawScope.drawQueen(w: Float, h: Float, pal: PiecePalette, sw: Float, style: PieceStyle) {
    val crown = Path().apply {
        moveTo(w * 0.27f, h * 0.75f)
        lineTo(w * 0.18f, h * 0.28f)
        lineTo(w * 0.33f, h * 0.44f)
        lineTo(w * 0.38f, h * 0.18f)
        lineTo(w * 0.46f, h * 0.40f)
        lineTo(w * 0.50f, h * 0.15f)
        lineTo(w * 0.54f, h * 0.40f)
        lineTo(w * 0.62f, h * 0.18f)
        lineTo(w * 0.67f, h * 0.44f)
        lineTo(w * 0.82f, h * 0.28f)
        lineTo(w * 0.73f, h * 0.75f)
        close()
    }
    drawPath(crown, pal.fillBrush)
    drawPath(crown, pal.strokeColor, style = Stroke(sw, join = StrokeJoin.Round))

    val points = listOf(
        Offset(w * 0.18f, h * 0.25f),
        Offset(w * 0.38f, h * 0.16f),
        Offset(w * 0.50f, h * 0.12f),
        Offset(w * 0.62f, h * 0.16f),
        Offset(w * 0.82f, h * 0.25f)
    )
    for (pt in points) {
        drawCircle(brush = pal.fillBrush, radius = w * 0.038f, center = pt)
        drawCircle(color = pal.strokeColor, radius = w * 0.038f, center = pt, style = Stroke(sw * 0.7f))
    }
    if (pal.jewelColor != null) {
        drawCircle(color = pal.jewelColor, radius = w * 0.05f, center = Offset(w * 0.50f, h * 0.56f))
    }
    drawLine(pal.detailColor, Offset(w * 0.29f, h * 0.66f), Offset(w * 0.71f, h * 0.66f), strokeWidth = sw * 0.85f)
    drawBase(w, h, pal, sw, style, widthFactor = 0.66f)
}

private fun DrawScope.drawKing(w: Float, h: Float, pal: PiecePalette, sw: Float, style: PieceStyle) {
    drawLine(
        color = pal.strokeColor,
        start = Offset(w * 0.50f, h * 0.06f),
        end = Offset(w * 0.50f, h * 0.23f),
        strokeWidth = sw * 1.35f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = pal.strokeColor,
        start = Offset(w * 0.41f, h * 0.13f),
        end = Offset(w * 0.59f, h * 0.13f),
        strokeWidth = sw * 1.35f,
        cap = StrokeCap.Round
    )

    val body = Path().apply {
        moveTo(w * 0.27f, h * 0.75f)
        cubicTo(w * 0.20f, h * 0.50f, w * 0.22f, h * 0.28f, w * 0.38f, h * 0.30f)
        cubicTo(w * 0.44f, h * 0.21f, w * 0.56f, h * 0.21f, w * 0.62f, h * 0.30f)
        cubicTo(w * 0.78f, h * 0.28f, w * 0.80f, h * 0.50f, w * 0.73f, h * 0.75f)
        close()
    }
    drawPath(body, pal.fillBrush)
    drawPath(body, pal.strokeColor, style = Stroke(sw, join = StrokeJoin.Round))

    drawLine(pal.detailColor, Offset(w * 0.50f, h * 0.26f), Offset(w * 0.50f, h * 0.64f), strokeWidth = sw * 0.7f)
    drawLine(pal.detailColor, Offset(w * 0.30f, h * 0.66f), Offset(w * 0.70f, h * 0.66f), strokeWidth = sw * 0.85f)
    if (pal.jewelColor != null) {
        drawCircle(color = pal.jewelColor, radius = w * 0.048f, center = Offset(w * 0.50f, h * 0.45f))
    }
    drawBase(w, h, pal, sw, style, widthFactor = 0.66f)
}
