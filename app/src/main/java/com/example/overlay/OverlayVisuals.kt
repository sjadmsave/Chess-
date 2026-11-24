package com.example.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.PieceType
import com.example.ui.components.resolvePyChessDrawable
import com.example.ui.theme.AppThemeMode
import kotlin.math.min

internal data class OverlayPalette(
    val isDark: Boolean,
    val cardBg: Int,
    val cardStroke: Int,
    val surfacePill: Int,
    val surfacePillStroke: Int,
    val surfaceDisabled: Int,
    val accent: Int,
    val accentContainer: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val textDisabled: Int,
    val onAccent: Int,
    val danger: Int,
    val footerBg: Int
) {
    companion object {
        fun resolve(context: Context, mode: AppThemeMode, opacityPercent: Int): OverlayPalette {
            val sysDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val isDark = when (mode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> sysDark
            }
            val alpha = ((opacityPercent.coerceIn(65, 100) / 100f) * 255).toInt()
            return if (isDark) {
                OverlayPalette(
                    isDark = true,
                    cardBg = Color.argb(alpha, 18, 30, 50),
                    cardStroke = Color.argb((alpha * 0.85f).toInt(), 51, 74, 110),
                    surfacePill = Color.argb(242, 30, 46, 74),
                    surfacePillStroke = Color.parseColor("#354C73"),
                    surfaceDisabled = Color.argb(175, 25, 38, 61),
                    accent = Color.parseColor("#4CC9E6"),
                    accentContainer = Color.parseColor("#006A87"),
                    textPrimary = Color.parseColor("#F1F5FA"),
                    textSecondary = Color.parseColor("#B5C4D8"),
                    textDisabled = Color.parseColor("#64748B"),
                    onAccent = Color.WHITE,
                    danger = Color.parseColor("#F87171"),
                    footerBg = Color.argb(220, 23, 37, 61)
                )
            } else {
                OverlayPalette(
                    isDark = false,
                    cardBg = Color.argb(alpha, 237, 241, 247),
                    cardStroke = Color.argb((alpha * 0.9f).toInt(), 196, 207, 222),
                    surfacePill = Color.argb(250, 255, 255, 255),
                    surfacePillStroke = Color.parseColor("#D5DFEC"),
                    surfaceDisabled = Color.argb(210, 222, 229, 238),
                    accent = Color.parseColor("#006A87"),
                    accentContainer = Color.parseColor("#006A87"),
                    textPrimary = Color.parseColor("#112336"),
                    textSecondary = Color.parseColor("#54687D"),
                    textDisabled = Color.parseColor("#94A3B8"),
                    onAccent = Color.WHITE,
                    danger = Color.parseColor("#B91C1C"),
                    footerBg = Color.argb(242, 255, 255, 255)
                )
            }
        }
    }
}

internal enum class OverlayIconType {
    MENU,
    EDIT_BOARD,
    UNDO,
    FLIP,
    DRAG_GRIP,
    MINIMIZE_BUBBLE,
    SHRINK_WINDOW,
    EXPAND_WINDOW,
    CORNER_RESIZE,
    BOARD_GRID,
    INFO,
    RESIZE_SLIDERS,
    NEW_MATCH,
    OPEN_APP,
    CLOSE_X,
    PLUS,
    MINUS,
    KING_OUTLINE,
    BUBBLE_KNIGHT
}

internal fun createPillDrawable(
    fillColor: Int,
    strokeColor: Int,
    cornerRadiusPx: Float,
    strokeWidthPx: Int = 2
): GradientDrawable = GradientDrawable().apply {
    setColor(fillColor)
    cornerRadius = cornerRadiusPx
    if (strokeWidthPx > 0) {
        setStroke(strokeWidthPx, strokeColor)
    }
}

internal class OverlayVectorIconView(
    context: Context,
    private var iconType: OverlayIconType,
    private var tintColor: Int
) : View(context) {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val path = Path()

    fun updateIcon(type: OverlayIconType, color: Int) {
        iconType = type
        tintColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(w, h)
        if (s <= 0f) return
        val ox = (w - s) * 0.5f
        val oy = (h - s) * 0.5f

        strokePaint.color = tintColor
        fillPaint.color = tintColor
        strokePaint.strokeWidth = (s * 0.105f).coerceAtLeast(2.2f)
        path.reset()

        when (iconType) {
            OverlayIconType.MENU -> {
                val r = s * 0.085f
                val cy = oy + s * 0.5f
                canvas.drawCircle(ox + s * 0.24f, cy, r, fillPaint)
                canvas.drawCircle(ox + s * 0.50f, cy, r, fillPaint)
                canvas.drawCircle(ox + s * 0.76f, cy, r, fillPaint)
            }

            OverlayIconType.EDIT_BOARD -> {
                // Rounded square + diagonal pencil
                val pad = s * 0.20f
                val rect = RectF(ox + pad, oy + pad * 1.15f, ox + s - pad * 1.15f, oy + s - pad)
                path.moveTo(rect.right - s * 0.16f, rect.top)
                path.lineTo(rect.left + s * 0.08f, rect.top)
                path.quadTo(rect.left, rect.top, rect.left, rect.top + s * 0.08f)
                path.lineTo(rect.left, rect.bottom - s * 0.08f)
                path.quadTo(rect.left, rect.bottom, rect.left + s * 0.08f, rect.bottom)
                path.lineTo(rect.right - s * 0.08f, rect.bottom)
                path.quadTo(rect.right, rect.bottom, rect.right, rect.bottom - s * 0.08f)
                path.lineTo(rect.right, rect.top + s * 0.24f)
                canvas.drawPath(path, strokePaint)

                path.reset()
                path.moveTo(ox + s * 0.45f, oy + s * 0.55f)
                path.lineTo(ox + s * 0.80f, oy + s * 0.20f)
                canvas.drawPath(path, strokePaint)
            }

            OverlayIconType.UNDO -> {
                // Curved counter-clockwise undo arrow
                path.moveTo(ox + s * 0.36f, oy + s * 0.22f)
                path.lineTo(ox + s * 0.18f, oy + s * 0.38f)
                path.lineTo(ox + s * 0.36f, oy + s * 0.54f)
                canvas.drawPath(path, strokePaint)

                path.reset()
                path.moveTo(ox + s * 0.20f, oy + s * 0.38f)
                path.lineTo(ox + s * 0.62f, oy + s * 0.38f)
                path.cubicTo(
                    ox + s * 0.84f, oy + s * 0.38f,
                    ox + s * 0.84f, oy + s * 0.74f,
                    ox + s * 0.60f, oy + s * 0.74f
                )
                path.lineTo(ox + s * 0.32f, oy + s * 0.74f)
                canvas.drawPath(path, strokePaint)
            }

            OverlayIconType.FLIP -> {
                // Up arrow on left, down arrow on right
                val lx = ox + s * 0.34f
                val rx = ox + s * 0.66f
                path.moveTo(lx, oy + s * 0.76f)
                path.lineTo(lx, oy + s * 0.24f)
                path.moveTo(lx - s * 0.14f, oy + s * 0.38f)
                path.lineTo(lx, oy + s * 0.24f)
                path.lineTo(lx + s * 0.14f, oy + s * 0.38f)

                path.moveTo(rx, oy + s * 0.24f)
                path.lineTo(rx, oy + s * 0.76f)
                path.moveTo(rx - s * 0.14f, oy + s * 0.62f)
                path.lineTo(rx, oy + s * 0.76f)
                path.lineTo(rx + s * 0.14f, oy + s * 0.62f)
                canvas.drawPath(path, strokePaint)
            }

            OverlayIconType.DRAG_GRIP -> {
                // 2 columns x 3 rows neat dots
                val r = s * 0.072f
                val xs = floatArrayOf(ox + s * 0.37f, ox + s * 0.63f)
                val ys = floatArrayOf(oy + s * 0.26f, oy + s * 0.50f, oy + s * 0.74f)
                for (yy in ys) {
                    for (xx in xs) {
                        canvas.drawCircle(xx, yy, r, fillPaint)
                    }
                }
            }

            OverlayIconType.MINIMIZE_BUBBLE -> {
                // Pip / collapse-to-bubble icon: outer frame + small bottom-end bubble
                val rect = RectF(ox + s * 0.18f, oy + s * 0.22f, ox + s * 0.82f, oy + s * 0.78f)
                canvas.drawRoundRect(rect, s * 0.12f, s * 0.12f, strokePaint)
                val pill = RectF(ox + s * 0.46f, oy + s * 0.50f, ox + s * 0.74f, oy + s * 0.70f)
                canvas.drawRoundRect(pill, s * 0.08f, s * 0.08f, fillPaint)
            }

            OverlayIconType.SHRINK_WINDOW -> {
                // Diagonal inward arrows
                strokePaint.strokeWidth = (s * 0.095f).coerceAtLeast(2f)
                // Top-left pointing in
                path.moveTo(ox + s * 0.20f, oy + s * 0.20f)
                path.lineTo(ox + s * 0.42f, oy + s * 0.42f)
                path.moveTo(ox + s * 0.26f, oy + s * 0.42f)
                path.lineTo(ox + s * 0.42f, oy + s * 0.42f)
                path.lineTo(ox + s * 0.42f, oy + s * 0.26f)
                // Bottom-right pointing in
                path.moveTo(ox + s * 0.80f, oy + s * 0.80f)
                path.lineTo(ox + s * 0.58f, oy + s * 0.58f)
                path.moveTo(ox + s * 0.74f, oy + s * 0.58f)
                path.lineTo(ox + s * 0.58f, oy + s * 0.58f)
                path.lineTo(ox + s * 0.58f, oy + s * 0.74f)
                canvas.drawPath(path, strokePaint)
            }

            OverlayIconType.EXPAND_WINDOW -> {
                // Diagonal outward arrows
                strokePaint.strokeWidth = (s * 0.095f).coerceAtLeast(2f)
                path.moveTo(ox + s * 0.42f, oy + s * 0.42f)
                path.lineTo(ox + s * 0.20f, oy + s * 0.20f)
                path.moveTo(ox + s * 0.36f, oy + s * 0.20f)
                path.lineTo(ox + s * 0.20f, oy + s * 0.20f)
                path.lineTo(ox + s * 0.20f, oy + s * 0.36f)

                path.moveTo(ox + s * 0.58f, oy + s * 0.58f)
                path.lineTo(ox + s * 0.80f, oy + s * 0.80f)
                path.moveTo(ox + s * 0.64f, oy + s * 0.80f)
                path.lineTo(ox + s * 0.80f, oy + s * 0.80f)
                path.lineTo(ox + s * 0.80f, oy + s * 0.64f)
                canvas.drawPath(path, strokePaint)
            }

            OverlayIconType.CORNER_RESIZE -> {
                // Subtle 3 diagonal grip lines in bottom-right corner
                strokePaint.strokeWidth = (s * 0.095f).coerceAtLeast(2.2f)
                path.moveTo(ox + s * 0.32f, oy + s * 0.80f)
                path.lineTo(ox + s * 0.80f, oy + s * 0.32f)

                path.moveTo(ox + s * 0.54f, oy + s * 0.80f)
                path.lineTo(ox + s * 0.80f, oy + s * 0.54f)

                path.moveTo(ox + s * 0.74f, oy + s * 0.80f)
                path.lineTo(ox + s * 0.80f, oy + s * 0.74f)
                canvas.drawPath(path, strokePaint)
            }

            OverlayIconType.BOARD_GRID -> {
                val rect = RectF(ox + s * 0.20f, oy + s * 0.20f, ox + s * 0.80f, oy + s * 0.80f)
                canvas.drawRoundRect(rect, s * 0.10f, s * 0.10f, strokePaint)
                canvas.drawLine(ox + s * 0.50f, oy + s * 0.20f, ox + s * 0.50f, oy + s * 0.80f, strokePaint)
                canvas.drawLine(ox + s * 0.20f, oy + s * 0.50f, ox + s * 0.80f, oy + s * 0.50f, strokePaint)
            }

            OverlayIconType.INFO -> {
                canvas.drawCircle(ox + s * 0.50f, oy + s * 0.50f, s * 0.31f, strokePaint)
                canvas.drawCircle(ox + s * 0.50f, oy + s * 0.35f, s * 0.055f, fillPaint)
                canvas.drawLine(ox + s * 0.50f, oy + s * 0.46f, ox + s * 0.50f, oy + s * 0.67f, strokePaint)
            }

            OverlayIconType.RESIZE_SLIDERS -> {
                strokePaint.strokeWidth = (s * 0.095f).coerceAtLeast(2f)
                canvas.drawLine(ox + s * 0.18f, oy + s * 0.34f, ox + s * 0.82f, oy + s * 0.34f, strokePaint)
                canvas.drawLine(ox + s * 0.18f, oy + s * 0.66f, ox + s * 0.82f, oy + s * 0.66f, strokePaint)
                canvas.drawCircle(ox + s * 0.38f, oy + s * 0.34f, s * 0.10f, fillPaint)
                canvas.drawCircle(ox + s * 0.64f, oy + s * 0.66f, s * 0.10f, fillPaint)
            }

            OverlayIconType.NEW_MATCH -> {
                // 4-point sparkle
                path.moveTo(ox + s * 0.50f, oy + s * 0.16f)
                path.quadTo(ox + s * 0.52f, oy + s * 0.48f, ox + s * 0.84f, oy + s * 0.50f)
                path.quadTo(ox + s * 0.52f, oy + s * 0.52f, ox + s * 0.50f, oy + s * 0.84f)
                path.quadTo(ox + s * 0.48f, oy + s * 0.52f, ox + s * 0.16f, oy + s * 0.50f)
                path.quadTo(ox + s * 0.48f, oy + s * 0.48f, ox + s * 0.50f, oy + s * 0.16f)
                path.close()
                canvas.drawPath(path, fillPaint)
            }

            OverlayIconType.OPEN_APP -> {
                path.moveTo(ox + s * 0.52f, oy + s * 0.22f)
                path.lineTo(ox + s * 0.24f, oy + s * 0.22f)
                path.lineTo(ox + s * 0.24f, oy + s * 0.78f)
                path.lineTo(ox + s * 0.78f, oy + s * 0.78f)
                path.lineTo(ox + s * 0.78f, oy + s * 0.50f)
                canvas.drawPath(path, strokePaint)

                path.reset()
                path.moveTo(ox + s * 0.48f, oy + s * 0.52f)
                path.lineTo(ox + s * 0.80f, oy + s * 0.20f)
                path.moveTo(ox + s * 0.60f, oy + s * 0.20f)
                path.lineTo(ox + s * 0.80f, oy + s * 0.20f)
                path.lineTo(ox + s * 0.80f, oy + s * 0.40f)
                canvas.drawPath(path, strokePaint)
            }

            OverlayIconType.CLOSE_X -> {
                path.moveTo(ox + s * 0.26f, oy + s * 0.26f)
                path.lineTo(ox + s * 0.74f, oy + s * 0.74f)
                path.moveTo(ox + s * 0.74f, oy + s * 0.26f)
                path.lineTo(ox + s * 0.26f, oy + s * 0.74f)
                canvas.drawPath(path, strokePaint)
            }

            OverlayIconType.PLUS -> {
                path.moveTo(ox + s * 0.24f, oy + s * 0.50f)
                path.lineTo(ox + s * 0.76f, oy + s * 0.50f)
                path.moveTo(ox + s * 0.50f, oy + s * 0.24f)
                path.lineTo(ox + s * 0.50f, oy + s * 0.76f)
                canvas.drawPath(path, strokePaint)
            }

            OverlayIconType.MINUS -> {
                path.moveTo(ox + s * 0.24f, oy + s * 0.50f)
                path.lineTo(ox + s * 0.76f, oy + s * 0.50f)
                canvas.drawPath(path, strokePaint)
            }

            OverlayIconType.KING_OUTLINE -> {
                // Clean crown/king icon for Wizard step 1
                path.moveTo(ox + s * 0.50f, oy + s * 0.14f)
                path.lineTo(ox + s * 0.50f, oy + s * 0.30f)
                path.moveTo(ox + s * 0.41f, oy + s * 0.21f)
                path.lineTo(ox + s * 0.59f, oy + s * 0.21f)
                canvas.drawPath(path, strokePaint)

                path.reset()
                path.moveTo(ox + s * 0.28f, oy + s * 0.76f)
                path.lineTo(ox + s * 0.72f, oy + s * 0.76f)
                path.cubicTo(
                    ox + s * 0.80f, oy + s * 0.46f,
                    ox + s * 0.62f, oy + s * 0.32f,
                    ox + s * 0.50f, oy + s * 0.40f
                )
                path.cubicTo(
                    ox + s * 0.38f, oy + s * 0.32f,
                    ox + s * 0.20f, oy + s * 0.46f,
                    ox + s * 0.28f, oy + s * 0.76f
                )
                path.close()
                canvas.drawPath(path, fillPaint)
            }

            OverlayIconType.BUBBLE_KNIGHT -> {
                drawClassicChessPieceCanvas(
                    canvas = canvas,
                    piece = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE),
                    left = ox + s * 0.12f,
                    top = oy + s * 0.10f,
                    size = s * 0.76f,
                    style = PieceStyle.CLASSIC_STAUNTON
                )
            }
        }
    }
}

/**
 * Draws crisp vector Staunton / Geometric chess pieces directly on an Android Canvas
 * for the floating overlay board & editor, eliminating blurry font Unicode glyphs.
 */
internal fun drawClassicChessPieceCanvas(
    canvas: Canvas,
    piece: ChessPiece,
    left: Float,
    top: Float,
    size: Float,
    style: PieceStyle = PieceStyle.CLASSIC_STAUNTON,
    context: Context? = null
) {
    if (context != null) {
        val pyChessRes = resolvePyChessDrawable(style, piece)
        if (pyChessRes != null) {
            val drawable = ContextCompat.getDrawable(context, pyChessRes)
            if (drawable != null) {
                drawable.setBounds(
                    left.toInt(),
                    top.toInt(),
                    (left + size).toInt(),
                    (top + size).toInt()
                )
                drawable.draw(canvas)
                return
            }
        }
    }
    val isWhite = piece.color == PieceColor.WHITE
    val fillColor = when (style) {
        PieceStyle.ROYAL_HERITAGE -> if (isWhite) Color.parseColor("#FDE68A") else Color.parseColor("#3B0D14")
        PieceStyle.WOOD_CARVED -> if (isWhite) Color.parseColor("#EED09D") else Color.parseColor("#5C2E14")
        PieceStyle.NEON_CYBER -> if (isWhite) Color.parseColor("#CCFBF1") else Color.parseColor("#0F172A")
        PieceStyle.HIGH_CONTRAST -> if (isWhite) Color.WHITE else Color.BLACK
        else -> if (isWhite) Color.parseColor("#FAF6EC") else Color.parseColor("#1D2432")
    }
    val strokeColor = when (style) {
        PieceStyle.ROYAL_HERITAGE -> if (isWhite) Color.parseColor("#451A03") else Color.parseColor("#FBBF24")
        PieceStyle.WOOD_CARVED -> if (isWhite) Color.parseColor("#4A2C11") else Color.parseColor("#F2DEC0")
        PieceStyle.NEON_CYBER -> if (isWhite) Color.parseColor("#0E7490") else Color.parseColor("#38BDF8")
        PieceStyle.HIGH_CONTRAST -> if (isWhite) Color.BLACK else Color.WHITE
        else -> if (isWhite) Color.parseColor("#242936") else Color.parseColor("#E2E8F0")
    }
    val detailColor = if (isWhite) strokeColor else Color.parseColor("#CBD5E1")

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        this.style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        this.style = Paint.Style.STROKE
        strokeWidth = (size * 0.048f).coerceAtLeast(1.3f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = detailColor
        this.style = Paint.Style.STROKE
        strokeWidth = (size * 0.036f).coerceAtLeast(1.0f)
        strokeCap = Paint.Cap.ROUND
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(46, 0, 0, 0)
        this.style = Paint.Style.FILL
    }

    fun x(f: Float) = left + size * f
    fun y(f: Float) = top + size * f

    // Soft ground shadow
    canvas.drawOval(RectF(x(0.18f), y(0.84f), x(0.82f), y(0.93f)), shadowPaint)

    val path = Path()

    fun drawShape(p: Path) {
        canvas.drawPath(p, fillPaint)
        canvas.drawPath(p, strokePaint)
    }

    fun drawPedestal(topY: Float = 0.77f, bottomY: Float = 0.87f, inset: Float = 0.21f) {
        val r = RectF(x(inset), y(topY), x(1f - inset), y(bottomY))
        canvas.drawRoundRect(r, size * 0.045f, size * 0.045f, fillPaint)
        canvas.drawRoundRect(r, size * 0.045f, size * 0.045f, strokePaint)
        canvas.drawLine(x(inset + 0.05f), y(topY + 0.045f), x(1f - inset - 0.05f), y(topY + 0.045f), detailPaint)
    }

    when (piece.type) {
        PieceType.PAWN -> {
            path.moveTo(x(0.30f), y(0.77f))
            path.cubicTo(x(0.33f), y(0.62f), x(0.41f), y(0.51f), x(0.42f), y(0.43f))
            path.lineTo(x(0.58f), y(0.43f))
            path.cubicTo(x(0.59f), y(0.51f), x(0.67f), y(0.62f), x(0.70f), y(0.77f))
            path.close()
            drawShape(path)

            val collar = RectF(x(0.36f), y(0.39f), x(0.64f), y(0.45f))
            canvas.drawRoundRect(collar, size * 0.03f, size * 0.03f, fillPaint)
            canvas.drawRoundRect(collar, size * 0.03f, size * 0.03f, strokePaint)

            canvas.drawCircle(x(0.50f), y(0.28f), size * 0.135f, fillPaint)
            canvas.drawCircle(x(0.50f), y(0.28f), size * 0.135f, strokePaint)
            drawPedestal(topY = 0.77f, bottomY = 0.87f, inset = 0.24f)
        }

        PieceType.ROOK -> {
            path.moveTo(x(0.27f), y(0.77f))
            path.lineTo(x(0.32f), y(0.36f))
            path.lineTo(x(0.68f), y(0.36f))
            path.lineTo(x(0.73f), y(0.77f))
            path.close()
            drawShape(path)

            path.reset()
            path.moveTo(x(0.24f), y(0.36f))
            path.lineTo(x(0.24f), y(0.18f))
            path.lineTo(x(0.36f), y(0.18f))
            path.lineTo(x(0.36f), y(0.25f))
            path.lineTo(x(0.44f), y(0.25f))
            path.lineTo(x(0.44f), y(0.18f))
            path.lineTo(x(0.56f), y(0.18f))
            path.lineTo(x(0.56f), y(0.25f))
            path.lineTo(x(0.64f), y(0.25f))
            path.lineTo(x(0.64f), y(0.18f))
            path.lineTo(x(0.76f), y(0.18f))
            path.lineTo(x(0.76f), y(0.36f))
            path.close()
            drawShape(path)
            drawPedestal(topY = 0.77f, bottomY = 0.87f, inset = 0.20f)
        }

        PieceType.KNIGHT -> {
            path.moveTo(x(0.27f), y(0.77f))
            path.cubicTo(x(0.29f), y(0.60f), x(0.39f), y(0.52f), x(0.42f), y(0.44f))
            path.cubicTo(x(0.32f), y(0.45f), x(0.22f), y(0.49f), x(0.19f), y(0.41f))
            path.cubicTo(x(0.17f), y(0.34f), x(0.27f), y(0.26f), x(0.39f), y(0.21f))
            path.lineTo(x(0.42f), y(0.12f))
            path.lineTo(x(0.51f), y(0.19f))
            path.cubicTo(x(0.72f), y(0.21f), x(0.79f), y(0.44f), x(0.73f), y(0.77f))
            path.close()
            drawShape(path)

            canvas.drawCircle(x(0.38f), y(0.30f), size * 0.028f, detailPaint.apply { this.style = Paint.Style.FILL })
            detailPaint.style = Paint.Style.STROKE
            drawPedestal(topY = 0.77f, bottomY = 0.87f, inset = 0.21f)
        }

        PieceType.BISHOP -> {
            path.moveTo(x(0.27f), y(0.77f))
            path.cubicTo(x(0.30f), y(0.60f), x(0.37f), y(0.52f), x(0.35f), y(0.39f))
            path.cubicTo(x(0.33f), y(0.28f), x(0.42f), y(0.20f), x(0.50f), y(0.18f))
            path.cubicTo(x(0.58f), y(0.20f), x(0.67f), y(0.28f), x(0.65f), y(0.39f))
            path.cubicTo(x(0.63f), y(0.52f), x(0.70f), y(0.60f), x(0.73f), y(0.77f))
            path.close()
            drawShape(path)

            canvas.drawCircle(x(0.50f), y(0.14f), size * 0.055f, fillPaint)
            canvas.drawCircle(x(0.50f), y(0.14f), size * 0.055f, strokePaint)
            canvas.drawLine(x(0.43f), y(0.31f), x(0.57f), y(0.42f), detailPaint)
            drawPedestal(topY = 0.77f, bottomY = 0.87f, inset = 0.21f)
        }

        PieceType.QUEEN -> {
            path.moveTo(x(0.27f), y(0.77f))
            path.lineTo(x(0.19f), y(0.29f))
            path.lineTo(x(0.36f), y(0.46f))
            path.lineTo(x(0.50f), y(0.21f))
            path.lineTo(x(0.64f), y(0.46f))
            path.lineTo(x(0.81f), y(0.29f))
            path.lineTo(x(0.73f), y(0.77f))
            path.close()
            drawShape(path)

            val tips = listOf(0.19f to 0.25f, 0.35f to 0.21f, 0.50f to 0.16f, 0.65f to 0.21f, 0.81f to 0.25f)
            tips.forEach { (tx, ty) ->
                canvas.drawCircle(x(tx), y(ty), size * 0.042f, fillPaint)
                canvas.drawCircle(x(tx), y(ty), size * 0.042f, strokePaint)
            }
            drawPedestal(topY = 0.77f, bottomY = 0.87f, inset = 0.20f)
        }

        PieceType.KING -> {
            // Cross
            canvas.drawLine(x(0.50f), y(0.10f), x(0.50f), y(0.27f), strokePaint)
            canvas.drawLine(x(0.42f), y(0.17f), x(0.58f), y(0.17f), strokePaint)

            path.moveTo(x(0.26f), y(0.77f))
            path.cubicTo(x(0.22f), y(0.56f), x(0.17f), y(0.40f), x(0.30f), y(0.33f))
            path.cubicTo(x(0.38f), y(0.28f), x(0.46f), y(0.33f), x(0.50f), y(0.39f))
            path.cubicTo(x(0.54f), y(0.33f), x(0.62f), y(0.28f), x(0.70f), y(0.33f))
            path.cubicTo(x(0.83f), y(0.40f), x(0.78f), y(0.56f), x(0.74f), y(0.77f))
            path.close()
            drawShape(path)

            canvas.drawLine(x(0.50f), y(0.41f), x(0.50f), y(0.70f), detailPaint)
            drawPedestal(topY = 0.77f, bottomY = 0.87f, inset = 0.20f)
        }
    }
}
