package com.example.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.ChessHapticEvent
import com.example.audio.ChessSound
import com.example.audio.HapticsManager
import com.example.audio.SoundManager
import com.example.chess.ChessBoardState
import com.example.chess.ChessMove
import com.example.chess.ChessPiece
import com.example.chess.EnginePersonality
import com.example.chess.GameResult
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.chess.StockfishUciEngine
import com.example.chess.StrengthTier
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.ui.theme.formatArabicNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Floating Chess Overlay Service adhering to Ruqaa Specification Section 11:
 * - TYPE_APPLICATION_OVERLAY, FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_IN_SCREEN
 * - Default position x = 12 dp, y = 72 dp, persisted after drag
 * - Width 180..560 dp (default 270 dp) with smooth corner resize grip + quick step controls
 * - Opacity 65..100% (default 100%), Frame shapes (12/7/0 dp), Bubble size (44/52/64 dp)
 * - Notification channel "النافذة العائمة" with IMPORTANCE_LOW
 * - Synced to app theme (frosted Light / Dark) with crisp Canvas vector icons & pieces
 */
class FloatingChessService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var hapticsManager: HapticsManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val soundManager = SoundManager()

    private var rootView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var prefs: UserPreferences = UserPreferences()

    private enum class OverlayMode {
        BOARD, MENU, DETAILS, RESIZE, WIZARD_STEP1, WIZARD_STEP2, EDITOR, BUBBLE
    }

    private var mode = OverlayMode.BOARD
    private var boardState = ChessBoardState.initial()
    private val moveStack = mutableListOf<Pair<ChessBoardState, ChessMove>>()
    private var playerColor = PieceColor.WHITE
    private var flipped = false
    private var selectedSquare: Square? = null
    private var legalForSelected: List<ChessMove> = emptyList()
    private var lastMove: ChessMove? = null
    private var isThinking = false

    // In-overlay Wizard config
    private var wizardColor = PieceColor.WHITE
    private var wizardElo = 700
    private var wizardPersonality = EnginePersonality.CLASSIC

    // In-overlay Position Editor state
    private var editorBoard: Array<ChessPiece?> = arrayOfNulls(64)
    private var editorTurn: PieceColor = PieceColor.WHITE
    private var editorBrush: ChessPiece? = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefsRepo = UserPreferencesRepository(applicationContext)
        hapticsManager = HapticsManager(applicationContext)
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!canDrawOverlay(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val fenArg = intent?.getStringExtra(EXTRA_FEN)
        val colorArg = intent?.getStringExtra(EXTRA_PLAYER_COLOR)

        serviceScope.launch {
            prefs = prefsRepo.preferencesFlow.first()
            soundManager.enabled = prefs.soundEnabled
            soundManager.profile = prefs.soundProfile
            soundManager.volumePercent = prefs.soundVolume
            hapticsManager.enabled = prefs.hapticsEnabled
            hapticsManager.style = prefs.vibrationStyle
            wizardElo = prefs.effectiveEngineElo
            wizardPersonality = prefs.enginePersonality

            if (!fenArg.isNullOrBlank()) {
                boardState = ChessBoardState.fromFen(fenArg)
                moveStack.clear()
                lastMove = null
                selectedSquare = null
                legalForSelected = emptyList()
            }
            if (!colorArg.isNullOrBlank()) {
                playerColor = runCatching { PieceColor.valueOf(colorArg) }.getOrDefault(PieceColor.WHITE)
                flipped = playerColor == PieceColor.BLACK
            }

            if (rootView == null) {
                attachOverlayWindow()
            } else {
                rebuildContent()
            }

            if (boardState.turn != playerColor && boardState.evaluateGameResult() == GameResult.ONGOING) {
                triggerEngineReply()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        StockfishUciEngine.closeSession()
        removeOverlayWindow()
        serviceScope.cancel()
    }

    private fun startForegroundNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "النافذة العائمة",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingChessService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("رُقعة • نافذة عائمة نشطة")
            .setContentText("اضغط للعودة إلى رُقعة أو لإغلاق النافذة")
            .setContentIntent(openAppIntent)
            .addAction(0, "فتح رُقعة", openAppIntent)
            .addAction(0, "إغلاق", stopIntent)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()

    private fun dpF(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        resources.displayMetrics
    )

    private fun pxToDp(px: Int): Int {
        val density = resources.displayMetrics.density
        return if (density <= 0f) px else (px / density).roundToInt()
    }

    private fun maxAllowedWidthDp(): Int {
        val screenDp = pxToDp(resources.displayMetrics.widthPixels)
        return (screenDp - 16).coerceIn(220, 560)
    }

    private fun clampedWidthDp(requested: Int = prefs.floatingWidthDp): Int =
        requested.coerceIn(180, maxAllowedWidthDp())

    private fun attachOverlayWindow() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(prefs.floatingX)
            y = dp(prefs.floatingY)
        }

        val container = FrameLayout(this)
        rootView = container
        layoutParams = params
        rebuildContent()
        windowManager.addView(container, params)
    }

    private fun removeOverlayWindow() {
        rootView?.let {
            runCatching { windowManager.removeView(it) }
        }
        rootView = null
    }

    private fun rebuildContent() {
        val container = rootView as? FrameLayout ?: return
        container.removeAllViews()

        val palette = OverlayPalette.resolve(this, prefs.themeMode, prefs.floatingOpacityPercent)

        if (mode == OverlayMode.BUBBLE) {
            buildBubbleOverlayUi(container, palette)
            return
        }

        val widthDp = clampedWidthDp()
        val widthPx = dp(widthDp)
        val cornerPx = dpF(prefs.floatingFrameShape.cornerDp.toFloat())
        val pad = dp(6)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = GradientDrawable().apply {
                setColor(palette.cardBg)
                cornerRadius = cornerPx
                setStroke(dp(1), palette.cardStroke)
            }
            setPadding(pad, pad, pad, pad)
            elevation = dpF(10f)
        }

        when (mode) {
            OverlayMode.BOARD -> buildBoardOverlayUi(card, widthPx, palette)
            OverlayMode.MENU -> buildMenuOverlayUi(card, widthPx, palette)
            OverlayMode.DETAILS -> buildDetailsOverlayUi(card, widthPx, palette)
            OverlayMode.RESIZE -> buildResizeOverlayUi(card, widthPx, palette)
            OverlayMode.WIZARD_STEP1 -> buildWizardStep1Ui(card, widthPx, palette)
            OverlayMode.WIZARD_STEP2 -> buildWizardStep2Ui(card, widthPx, palette)
            OverlayMode.EDITOR -> buildEditorOverlayUi(card, widthPx, palette)
            OverlayMode.BUBBLE -> Unit
        }

        container.addView(
            card,
            FrameLayout.LayoutParams(widthPx, FrameLayout.LayoutParams.WRAP_CONTENT)
        )
    }

    private fun buildBubbleOverlayUi(container: FrameLayout, palette: OverlayPalette) {
        val bubbleDp = prefs.floatingBubbleSize.sizeDp
        val bubblePx = dp(bubbleDp)

        val bubble = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.parseColor("#007D9E"),
                    Color.parseColor("#005570")
                )
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), if (palette.isDark) Color.parseColor("#72D9EF") else Color.WHITE)
            }
            elevation = dpF(10f)
        }

        val iconView = OverlayVectorIconView(this, OverlayIconType.BUBBLE_KNIGHT, Color.WHITE)
        val iconPad = (bubblePx * 0.16f).roundToInt()
        bubble.addView(
            iconView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(iconPad, iconPad, iconPad, iconPad)
            }
        )

        if (isThinking) {
            val dotPx = dp(10)
            val thinkingDot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#FACC15"))
                    setStroke(dp(1), Color.WHITE)
                }
            }
            bubble.addView(
                thinkingDot,
                FrameLayout.LayoutParams(dotPx, dotPx).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = dp(3)
                    marginEnd = dp(3)
                }
            )
        }

        attachDragAndClick(bubble, onClick = {
            mode = OverlayMode.BOARD
            rebuildContent()
        })
        container.addView(bubble, FrameLayout.LayoutParams(bubblePx, bubblePx))
    }

    private fun pillIconButton(
        iconType: OverlayIconType,
        palette: OverlayPalette,
        enabled: Boolean = true,
        accentTint: Boolean = false,
        heightDp: Int = 30,
        onClick: (() -> Unit)? = null
    ): FrameLayout {
        val fill = if (enabled) palette.surfacePill else palette.surfaceDisabled
        val tint = when {
            !enabled -> palette.textDisabled
            accentTint -> palette.accent
            else -> palette.textPrimary
        }
        val frame = FrameLayout(this).apply {
            background = createPillDrawable(
                fillColor = fill,
                strokeColor = palette.surfacePillStroke,
                cornerRadiusPx = dpF(heightDp / 2f),
                strokeWidthPx = dp(1)
            )
            if (enabled && onClick != null) {
                setOnClickListener { onClick() }
            }
        }
        val iconSz = dp(17)
        val iconView = OverlayVectorIconView(this, iconType, tint)
        frame.addView(
            iconView,
            FrameLayout.LayoutParams(iconSz, iconSz).apply {
                gravity = Gravity.CENTER
            }
        )
        return frame
    }

    private fun pillActionButton(
        label: String,
        palette: OverlayPalette,
        isPrimary: Boolean = false,
        iconType: OverlayIconType? = null,
        heightDp: Int = 40,
        onClick: () -> Unit
    ): LinearLayout {
        val fill = if (isPrimary) palette.accentContainer else palette.surfacePill
        val stroke = if (isPrimary) palette.accentContainer else palette.surfacePillStroke
        val contentColor = if (isPrimary) palette.onAccent else palette.textPrimary
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = createPillDrawable(fill, stroke, dpF(16f), dp(1))
            setPadding(dp(10), 0, dp(10), 0)
            minimumHeight = dp(heightDp)
            setOnClickListener { onClick() }

            val tv = TextView(this@FloatingChessService).apply {
                text = label
                textSize = 12.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(contentColor)
                gravity = Gravity.CENTER
            }
            addView(tv)

            if (iconType != null) {
                val icon = OverlayVectorIconView(this@FloatingChessService, iconType, contentColor)
                addView(
                    icon,
                    LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                        marginStart = dp(6)
                    }
                )
            }
        }
    }

    private fun buildTopBar(card: LinearLayout, palette: OverlayPalette) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(5))
        }

        val menuBtn = pillIconButton(
            iconType = OverlayIconType.MENU,
            palette = palette,
            accentTint = mode == OverlayMode.MENU
        ) {
            mode = if (mode == OverlayMode.MENU) OverlayMode.BOARD else OverlayMode.MENU
            rebuildContent()
        }
        val editBtn = pillIconButton(
            iconType = OverlayIconType.EDIT_BOARD,
            palette = palette,
            accentTint = true
        ) {
            editorBoard = boardState.board.copyOf()
            editorTurn = boardState.turn
            mode = OverlayMode.EDITOR
            rebuildContent()
        }
        val undoBtn = pillIconButton(
            iconType = OverlayIconType.UNDO,
            palette = palette,
            enabled = moveStack.isNotEmpty() && !isThinking,
            accentTint = moveStack.isNotEmpty() && !isThinking
        ) {
            undoOverlayMoves()
        }
        val flipBtn = pillIconButton(
            iconType = OverlayIconType.FLIP,
            palette = palette
        ) {
            flipped = !flipped
            rebuildContent()
        }
        val dragHandle = pillIconButton(
            iconType = OverlayIconType.DRAG_GRIP,
            palette = palette,
            accentTint = true
        )
        attachDragAndClick(dragHandle, onClick = null)

        val buttons = listOf(menuBtn, editBtn, undoBtn, flipBtn, dragHandle)
        val btnH = dp(31)
        buttons.forEachIndexed { idx, v ->
            val lp = LinearLayout.LayoutParams(0, btnH, 1f).apply {
                if (idx < buttons.lastIndex) marginEnd = dp(5)
            }
            bar.addView(v, lp)
        }
        card.addView(bar)
    }

    private fun buildBoardOverlayUi(card: LinearLayout, cardWidthPx: Int, palette: OverlayPalette) {
        buildTopBar(card, palette)

        val innerPad = dp(12)
        val boardPx = (cardWidthPx - innerPad).coerceAtLeast(dp(150))
        val boardCorner = (dpF(prefs.floatingFrameShape.cornerDp.toFloat()) - dpF(3f)).coerceAtLeast(0f)

        val boardView = FloatingBoardCanvasView(this).apply {
            updateState(
                state = boardState,
                theme = prefs.boardTheme,
                pieceStyle = prefs.pieceStyle,
                flipped = flipped,
                selected = selectedSquare,
                legalTargets = legalForSelected.map { it.to }.toSet(),
                lastMove = lastMove,
                showLastMove = prefs.showLastMove,
                showLegalMoves = prefs.showLegalMoves,
                useRings = prefs.useRingsInsteadOfDots,
                legalColorInt = prefs.legalMoveColorHex.toInt(),
                selectedColorInt = prefs.selectedColorHex.toInt(),
                cornerRadiusPx = boardCorner
            )
            onSquareTapped = { sq -> handleOverlaySquareTap(sq) }
        }
        card.addView(boardView, LinearLayout.LayoutParams(boardPx, boardPx))

        // Sleek bottom control footer directly BELOW the chessboard — zero obstruction of h1!
        val footerHeight = dp(30)
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            background = createPillDrawable(
                fillColor = palette.footerBg,
                strokeColor = palette.surfacePillStroke,
                cornerRadiusPx = dpF(14f),
                strokeWidthPx = dp(1)
            )
            setPadding(dp(4), 0, dp(4), 0)
        }

        // Minimize-to-Bubble compact button
        val bubbleBtn = FrameLayout(this).apply {
            background = createPillDrawable(
                fillColor = palette.surfacePill,
                strokeColor = palette.surfacePillStroke,
                cornerRadiusPx = dpF(11f),
                strokeWidthPx = dp(1)
            )
            setOnClickListener {
                mode = OverlayMode.BUBBLE
                rebuildContent()
            }
        }
        bubbleBtn.addView(
            OverlayVectorIconView(this, OverlayIconType.MINIMIZE_BUBBLE, palette.accent),
            FrameLayout.LayoutParams(dp(15), dp(15), Gravity.CENTER)
        )
        footer.addView(bubbleBtn, LinearLayout.LayoutParams(dp(28), dp(22)).apply { marginEnd = dp(4) })

        // Quick Shrink (-) & Expand (+) step buttons
        val shrinkBtn = FrameLayout(this).apply {
            val canShrink = clampedWidthDp() > 195
            background = createPillDrawable(
                fillColor = if (canShrink) palette.surfacePill else palette.surfaceDisabled,
                strokeColor = palette.surfacePillStroke,
                cornerRadiusPx = dpF(11f),
                strokeWidthPx = dp(1)
            )
            if (canShrink) {
                setOnClickListener { stepOverlayWidth(delta = -45) }
            }
        }
        shrinkBtn.addView(
            OverlayVectorIconView(
                this,
                OverlayIconType.SHRINK_WINDOW,
                if (clampedWidthDp() > 195) palette.textPrimary else palette.textDisabled
            ),
            FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER)
        )
        footer.addView(shrinkBtn, LinearLayout.LayoutParams(dp(26), dp(22)).apply { marginEnd = dp(3) })

        val expandBtn = FrameLayout(this).apply {
            val canExpand = clampedWidthDp() < maxAllowedWidthDp() - 10
            background = createPillDrawable(
                fillColor = if (canExpand) palette.surfacePill else palette.surfaceDisabled,
                strokeColor = palette.surfacePillStroke,
                cornerRadiusPx = dpF(11f),
                strokeWidthPx = dp(1)
            )
            if (canExpand) {
                setOnClickListener { stepOverlayWidth(delta = +45) }
            }
        }
        expandBtn.addView(
            OverlayVectorIconView(
                this,
                OverlayIconType.EXPAND_WINDOW,
                if (clampedWidthDp() < maxAllowedWidthDp() - 10) palette.accent else palette.textDisabled
            ),
            FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER)
        )
        footer.addView(expandBtn, LinearLayout.LayoutParams(dp(26), dp(22)))

        // Center Turn Status Pill
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val turnDot = View(this).apply {
            val dotColor = if (boardState.turn == PieceColor.WHITE) Color.WHITE else Color.parseColor("#1E293B")
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(dotColor)
                setStroke(dp(1), palette.accent)
            }
        }
        val statusLabel = TextView(this).apply {
            val res = boardState.evaluateGameResult()
            text = when {
                res != GameResult.ONGOING -> res.labelAr
                isThinking -> "يفكر..."
                boardState.turn == playerColor -> "دورك"
                else -> "دور المحرك"
            }
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(palette.textPrimary)
            maxLines = 1
        }
        statusRow.addView(turnDot, LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(5) })
        statusRow.addView(statusLabel)
        footer.addView(statusRow, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Corner Drag-Resize Grip (sleek diagonal lines, generous touch area, NO ugly dark overlay on board!)
        val resizeGrip = FrameLayout(this)
        resizeGrip.addView(
            OverlayVectorIconView(this, OverlayIconType.CORNER_RESIZE, palette.accent),
            FrameLayout.LayoutParams(dp(16), dp(16), Gravity.CENTER)
        )
        attachCornerResizeHandle(resizeGrip, card, boardView, footer, innerPad)
        footer.addView(resizeGrip, LinearLayout.LayoutParams(dp(34), dp(28)))

        card.addView(footer, LinearLayout.LayoutParams(boardPx, footerHeight).apply {
            topMargin = dp(5)
        })
    }

    private fun stepOverlayWidth(delta: Int) {
        val next = (clampedWidthDp() + delta).coerceIn(180, maxAllowedWidthDp())
        prefs = prefs.copy(floatingWidthDp = next)
        rebuildContent()
        serviceScope.launch {
            prefsRepo.saveFloatingCustomization(
                next,
                prefs.floatingFrameShape,
                prefs.floatingOpacityPercent,
                prefs.floatingBubbleSize
            )
        }
    }

    private fun buildMenuOverlayUi(card: LinearLayout, cardWidthPx: Int, palette: OverlayPalette) {
        buildTopBar(card, palette)

        val statusBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = createPillDrawable(palette.surfacePill, palette.surfacePillStroke, dpF(16f), dp(1))
            setPadding(dp(12), dp(7), dp(12), dp(7))
        }
        val statusText = TextView(this).apply {
            val res = boardState.evaluateGameResult()
            text = when {
                res != GameResult.ONGOING -> res.labelAr
                isThinking -> "المحرك يفكر..."
                boardState.turn == playerColor -> "دورك"
                else -> "دور المحرك"
            }
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(palette.textPrimary)
        }
        val turnCircle = View(this).apply {
            val c = if (boardState.turn == PieceColor.WHITE) Color.WHITE else Color.parseColor("#1E293B")
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(c)
                setStroke(dp(1), palette.accent)
            }
        }
        statusBar.addView(statusText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            marginEnd = dp(8)
        })
        statusBar.addView(turnCircle, LinearLayout.LayoutParams(dp(11), dp(11)))
        card.addView(statusBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(6)
        })

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun menuItem(
            title: String,
            iconType: OverlayIconType,
            isDanger: Boolean = false,
            enabled: Boolean = true,
            onClick: () -> Unit
        ) {
            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                gravity = Gravity.CENTER_VERTICAL
                background = createPillDrawable(
                    fillColor = if (enabled) palette.surfacePill else palette.surfaceDisabled,
                    strokeColor = palette.surfacePillStroke,
                    cornerRadiusPx = dpF(16f),
                    strokeWidthPx = dp(1)
                )
                setPadding(dp(12), dp(10), dp(12), dp(10))
                if (enabled) setOnClickListener { onClick() }
            }
            val titleTv = TextView(this).apply {
                text = title
                textSize = 12.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(
                    when {
                        !enabled -> palette.textDisabled
                        isDanger -> palette.danger
                        else -> palette.textPrimary
                    }
                )
                gravity = Gravity.CENTER
            }
            val iconColor = when {
                !enabled -> palette.textDisabled
                isDanger -> palette.danger
                else -> palette.accent
            }
            val iconView = OverlayVectorIconView(this, iconType, iconColor)
            btn.addView(titleTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            btn.addView(iconView, LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginStart = dp(8) })
            list.addView(
                btn,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(6)
                }
            )
        }

        menuItem("عرض الرقعة", OverlayIconType.BOARD_GRID) {
            mode = OverlayMode.BOARD
            rebuildContent()
        }
        menuItem("تراجع عن النقلة", OverlayIconType.UNDO, enabled = moveStack.isNotEmpty() && !isThinking) {
            undoOverlayMoves()
            mode = OverlayMode.BOARD
            rebuildContent()
        }
        menuItem("تعديل الوضعية", OverlayIconType.EDIT_BOARD) {
            editorBoard = boardState.board.copyOf()
            editorTurn = boardState.turn
            mode = OverlayMode.EDITOR
            rebuildContent()
        }
        menuItem("حجم النافذة (${formatArabicNumber(clampedWidthDp())} dp)", OverlayIconType.RESIZE_SLIDERS) {
            mode = OverlayMode.RESIZE
            rebuildContent()
        }
        menuItem("تفاصيل المباراة", OverlayIconType.INFO) {
            mode = OverlayMode.DETAILS
            rebuildContent()
        }
        menuItem("مباراة جديدة", OverlayIconType.NEW_MATCH) {
            mode = OverlayMode.WIZARD_STEP1
            rebuildContent()
        }
        menuItem("تصغير إلى فقاعة", OverlayIconType.MINIMIZE_BUBBLE) {
            mode = OverlayMode.BUBBLE
            rebuildContent()
        }
        menuItem("فتح رُقعة", OverlayIconType.OPEN_APP) {
            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_FEN, boardState.toFen())
                putExtra(EXTRA_PLAYER_COLOR, playerColor.name)
            }
            startActivity(openIntent)
            stopSelf()
        }
        menuItem("إغلاق النافذة", OverlayIconType.CLOSE_X, isDanger = true) {
            stopSelf()
        }

        scroll.addView(list)
        card.addView(scroll, LinearLayout.LayoutParams(cardWidthPx - dp(12), dp(248)))
    }

    private fun buildDetailsOverlayUi(card: LinearLayout, cardWidthPx: Int, palette: OverlayPalette) {
        buildWizardHeader(card, palette, "تفاصيل المباراة")
        val eloLabel = if (wizardElo <= 0) "بلا حد (أقصى قوة)" else "نحو ${formatArabicNumber(wizardElo)} إيلو"
        val details = TextView(this).apply {
            text = buildString {
                appendLine("• القوة: $eloLabel")
                appendLine("• الشخصية: ${wizardPersonality.nameAr}")
                appendLine("• لونك: ${if (playerColor == PieceColor.WHITE) "الأبيض" else "الأسود"}")
                appendLine("• النقلات المسجّلة: ${formatArabicNumber(moveStack.size)}")
                appendLine("• النتيجة: ${boardState.evaluateGameResult().labelAr}")
                append("• FEN: ${boardState.toFen()}")
            }
            textSize = 12f
            setTextColor(palette.textSecondary)
            gravity = Gravity.END
            background = createPillDrawable(palette.surfacePill, palette.surfacePillStroke, dpF(14f), dp(1))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        card.addView(details, LinearLayout.LayoutParams(cardWidthPx - dp(12), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })

        val backBtn = pillActionButton("العودة للرقعة", palette, isPrimary = true) {
            mode = OverlayMode.BOARD
            rebuildContent()
        }
        card.addView(backBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42)))
    }

    private fun buildResizeOverlayUi(card: LinearLayout, cardWidthPx: Int, palette: OverlayPalette) {
        buildWizardHeader(card, palette, "حجم النافذة")
        val maxW = maxAllowedWidthDp()
        var pendingWidth = clampedWidthDp()

        val sizeTv = TextView(this).apply {
            text = "العرض: ${formatArabicNumber(pendingWidth)} dp"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(palette.accent)
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(6))
        }
        card.addView(sizeTv)

        val presetsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, 0, 0, dp(8))
        }

        val seek = SeekBar(this).apply {
            max = (maxW - 180).coerceAtLeast(40)
            progress = (pendingWidth - 180).coerceIn(0, max)
            progressTintList = ColorStateList.valueOf(palette.accent)
            thumbTintList = ColorStateList.valueOf(palette.accent)
        }

        fun syncWidthUi(newWidth: Int) {
            pendingWidth = newWidth.coerceIn(180, maxW)
            sizeTv.text = "العرض: ${formatArabicNumber(pendingWidth)} dp"
            val targetProgress = (pendingWidth - 180).coerceIn(0, seek.max)
            if (seek.progress != targetProgress) {
                seek.progress = targetProgress
            }
            prefs = prefs.copy(floatingWidthDp = pendingWidth)
            val lp = card.layoutParams
            if (lp != null) {
                lp.width = dp(pendingWidth)
                card.layoutParams = lp
                rootView?.let { windowManager.updateViewLayout(it, layoutParams) }
            }
        }

        val presets = listOf("صغير" to 210, "متوسط" to 270, "كبير" to 350, "عريض" to min(440, maxW))
        presets.forEachIndexed { idx, (label, targetW) ->
            val selected = abs(pendingWidth - targetW) <= 22
            val pill = TextView(this).apply {
                text = label
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(if (selected) palette.onAccent else palette.textPrimary)
                background = createPillDrawable(
                    fillColor = if (selected) palette.accentContainer else palette.surfacePill,
                    strokeColor = if (selected) palette.accentContainer else palette.surfacePillStroke,
                    cornerRadiusPx = dpF(12f),
                    strokeWidthPx = dp(1)
                )
                setPadding(dp(4), dp(6), dp(4), dp(6))
                setOnClickListener {
                    syncWidthUi(targetW)
                }
            }
            presetsRow.addView(
                pill,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (idx < presets.lastIndex) marginEnd = dp(4)
                }
            )
        }
        card.addView(presetsRow)

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) {
                    syncWidthUi(p + 180)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        card.addView(seek, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // -  range  + controls matching reference screenshot style
        val stepRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(8))
        }
        val minusBtn = pillIconButton(OverlayIconType.MINUS, palette, heightDp = 34) {
            syncWidthUi(pendingWidth - 20)
        }
        val rangeLabel = TextView(this).apply {
            text = "${formatArabicNumber(180)} — ${formatArabicNumber(maxW)} dp"
            textSize = 11f
            setTextColor(palette.textSecondary)
            gravity = Gravity.CENTER
        }
        val plusBtn = pillIconButton(OverlayIconType.PLUS, palette, heightDp = 34) {
            syncWidthUi(pendingWidth + 20)
        }
        stepRow.addView(minusBtn, LinearLayout.LayoutParams(dp(42), dp(34)))
        stepRow.addView(rangeLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        stepRow.addView(plusBtn, LinearLayout.LayoutParams(dp(42), dp(34)))
        card.addView(stepRow)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val applyBtn = pillActionButton("تطبيق", palette, isPrimary = true, heightDp = 40) {
            prefs = prefs.copy(floatingWidthDp = pendingWidth)
            serviceScope.launch {
                prefsRepo.saveFloatingCustomization(
                    pendingWidth,
                    prefs.floatingFrameShape,
                    prefs.floatingOpacityPercent,
                    prefs.floatingBubbleSize
                )
            }
            mode = OverlayMode.BOARD
            rebuildContent()
        }
        val backBtn = pillActionButton("رجوع", palette, isPrimary = false, heightDp = 40) {
            mode = OverlayMode.BOARD
            rebuildContent()
        }
        actionRow.addView(applyBtn, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(6) })
        actionRow.addView(backBtn, LinearLayout.LayoutParams(0, dp(40), 1f))
        card.addView(actionRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun buildEditorOverlayUi(card: LinearLayout, cardWidthPx: Int, palette: OverlayPalette) {
        buildWizardHeader(card, palette, "محرر الوضعية")
        val boardPx = (cardWidthPx - dp(12)).coerceAtMost(dp(240))
        val boardCorner = (dpF(prefs.floatingFrameShape.cornerDp.toFloat()) - dpF(4f)).coerceAtLeast(0f)

        val editorView = FloatingEditorBoardView(this).apply {
            updateBoard(editorBoard, prefs.boardTheme, prefs.pieceStyle, boardCorner)
            onSquareTapped = { sq ->
                val idx = sq.index
                editorBoard[idx] = if (editorBoard[idx] == editorBrush) null else editorBrush
                updateBoard(editorBoard, prefs.boardTheme, prefs.pieceStyle, boardCorner)
            }
        }
        card.addView(editorView, LinearLayout.LayoutParams(boardPx, boardPx).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })

        val paletteRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, dp(5), 0, dp(2))
        }
        val paletteRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, dp(2), 0, dp(5))
        }
        val brushes1 = listOf(
            "♔" to ChessPiece(PieceType.KING, PieceColor.WHITE),
            "♕" to ChessPiece(PieceType.QUEEN, PieceColor.WHITE),
            "♖" to ChessPiece(PieceType.ROOK, PieceColor.WHITE),
            "♗" to ChessPiece(PieceType.BISHOP, PieceColor.WHITE),
            "♘" to ChessPiece(PieceType.KNIGHT, PieceColor.WHITE),
            "♙" to ChessPiece(PieceType.PAWN, PieceColor.WHITE)
        )
        val brushes2 = listOf(
            "♚" to ChessPiece(PieceType.KING, PieceColor.BLACK),
            "♛" to ChessPiece(PieceType.QUEEN, PieceColor.BLACK),
            "♜" to ChessPiece(PieceType.ROOK, PieceColor.BLACK),
            "♝" to ChessPiece(PieceType.BISHOP, PieceColor.BLACK),
            "♞" to ChessPiece(PieceType.KNIGHT, PieceColor.BLACK),
            "✕" to null
        )

        fun populatePalette(row: LinearLayout, items: List<Pair<String, ChessPiece?>>) {
            items.forEachIndexed { idx, (glyph, piece) ->
                val active = editorBrush == piece
                val tv = TextView(this).apply {
                    text = glyph
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setTextColor(if (active) palette.onAccent else palette.textPrimary)
                    background = createPillDrawable(
                        fillColor = if (active) palette.accentContainer else palette.surfacePill,
                        strokeColor = if (active) palette.accentContainer else palette.surfacePillStroke,
                        cornerRadiusPx = dpF(8f),
                        strokeWidthPx = dp(1)
                    )
                    setPadding(dp(2), dp(4), dp(2), dp(4))
                    setOnClickListener {
                        editorBrush = piece
                        rebuildContent()
                    }
                }
                row.addView(
                    tv,
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (idx < items.lastIndex) marginEnd = dp(3)
                    }
                )
            }
        }
        populatePalette(paletteRow1, brushes1)
        populatePalette(paletteRow2, brushes2)
        card.addView(paletteRow1)
        card.addView(paletteRow2)

        val turnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val turnBtn = pillActionButton(
            label = if (editorTurn == PieceColor.WHITE) "الدور: الأبيض" else "الدور: الأسود",
            palette = palette,
            isPrimary = false,
            heightDp = 38
        ) {
            editorTurn = editorTurn.opposite()
            rebuildContent()
        }
        val applyBtn = pillActionButton("تطبيق واللعب", palette, isPrimary = true, heightDp = 38) {
            val fen = buildFenFromArray(editorBoard, editorTurn)
            val parsed = ChessBoardState.fromFen(fen)
            if (parsed.findKing(PieceColor.WHITE) != null && parsed.findKing(PieceColor.BLACK) != null) {
                boardState = parsed
                moveStack.clear()
                selectedSquare = null
                legalForSelected = emptyList()
                lastMove = null
                mode = OverlayMode.BOARD
                rebuildContent()
                if (boardState.turn != playerColor && boardState.evaluateGameResult() == GameResult.ONGOING) {
                    triggerEngineReply()
                }
            }
        }
        turnRow.addView(turnBtn, LinearLayout.LayoutParams(0, dp(38), 1f).apply { marginEnd = dp(5) })
        turnRow.addView(applyBtn, LinearLayout.LayoutParams(0, dp(38), 1f))
        card.addView(turnRow)
    }

    private fun buildWizardHeader(card: LinearLayout, palette: OverlayPalette, heading: String = "مباراة عائمة") {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(8))
        }
        val closeBtn = pillIconButton(OverlayIconType.CLOSE_X, palette, heightDp = 32) {
            mode = OverlayMode.BOARD
            rebuildContent()
        }
        val title = TextView(this).apply {
            text = heading
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(palette.textPrimary)
            gravity = Gravity.CENTER
        }
        val drag = FrameLayout(this).apply {
            addView(
                OverlayVectorIconView(this@FloatingChessService, OverlayIconType.DRAG_GRIP, palette.accent),
                FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
            )
        }
        attachDragAndClick(drag, onClick = null)

        header.addView(closeBtn, LinearLayout.LayoutParams(dp(44), dp(32)))
        header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(drag, LinearLayout.LayoutParams(dp(36), dp(32)))
        card.addView(header)
    }

    private fun buildWizardStep1Ui(card: LinearLayout, cardWidthPx: Int, palette: OverlayPalette) {
        buildWizardHeader(card, palette)
        val stepTv = TextView(this).apply {
            text = "الخطوة ١ من ٢"
            textSize = 11f
            setTextColor(palette.accent)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }
        val titleTv = TextView(this).apply {
            text = "اختر لونك"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(palette.textPrimary)
            gravity = Gravity.END
            setPadding(0, dp(2), 0, dp(4))
        }
        val descTv = TextView(this).apply {
            text = "تبدأ المباراة من النافذة العائمة."
            textSize = 12f
            setTextColor(palette.textSecondary)
            gravity = Gravity.END
            setPadding(0, 0, 0, dp(10))
        }
        card.addView(stepTv)
        card.addView(titleTv)
        card.addView(descTv)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER
        }
        val blackBtn = pillActionButton(
            label = "أسود",
            palette = palette,
            isPrimary = false,
            iconType = OverlayIconType.KING_OUTLINE,
            heightDp = 44
        ) {
            wizardColor = PieceColor.BLACK
            mode = OverlayMode.WIZARD_STEP2
            rebuildContent()
        }
        val whiteBtn = pillActionButton(
            label = "أبيض",
            palette = palette,
            isPrimary = true,
            iconType = OverlayIconType.KING_OUTLINE,
            heightDp = 44
        ) {
            wizardColor = PieceColor.WHITE
            mode = OverlayMode.WIZARD_STEP2
            rebuildContent()
        }
        row.addView(blackBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(6) })
        row.addView(whiteBtn, LinearLayout.LayoutParams(0, dp(44), 1f))
        card.addView(row, LinearLayout.LayoutParams(cardWidthPx - dp(12), LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun buildWizardStep2Ui(card: LinearLayout, cardWidthPx: Int, palette: OverlayPalette) {
        buildWizardHeader(card, palette)
        val stepTv = TextView(this).apply {
            text = "الخطوة ٢ من ٢"
            textSize = 11f
            setTextColor(palette.accent)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }
        val headingTv = TextView(this).apply {
            text = "اضبط الخصم"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(palette.textPrimary)
            gravity = Gravity.END
        }
        val sideNoteTv = TextView(this).apply {
            text = "ستُلعب المباراة ب${if (wizardColor == PieceColor.WHITE) "الأبيض" else "الأسود"}"
            textSize = 12f
            setTextColor(palette.textSecondary)
            gravity = Gravity.END
            setPadding(0, dp(2), 0, dp(8))
        }
        card.addView(stepTv)
        card.addView(headingTv)
        card.addView(sideNoteTv)

        val personalityBtn = pillActionButton(wizardPersonality.nameAr, palette, isPrimary = false, heightDp = 40) {
            val all = EnginePersonality.entries
            val nextIdx = (all.indexOf(wizardPersonality) + 1) % all.size
            wizardPersonality = all[nextIdx]
            rebuildContent()
        }
        card.addView(personalityBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)).apply {
            bottomMargin = dp(8)
        })

        fun eloLabel(): String = if (wizardElo <= 0) "أقصى • بلا حد" else "${StrengthTier.fromElo(wizardElo).nameAr} • نحو ${formatArabicNumber(wizardElo)} إيلو"

        val eloTv = TextView(this).apply {
            text = eloLabel()
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(palette.accent)
            gravity = Gravity.END
            setPadding(0, 0, 0, dp(4))
        }
        card.addView(eloTv)

        val seek = SeekBar(this).apply {
            max = 2850
            progress = if (wizardElo <= 0) 2850 else (wizardElo - 200).coerceIn(0, 2800)
            progressTintList = ColorStateList.valueOf(palette.accent)
            thumbTintList = ColorStateList.valueOf(palette.accent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    wizardElo = if (p > 2800) -1 else StrengthTier.snapElo(p + 200)
                    eloTv.text = eloLabel()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        card.addView(seek)

        val stepRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(8))
        }
        val minusBtn = pillIconButton(OverlayIconType.MINUS, palette, heightDp = 34) {
            val cur = if (wizardElo <= 0) 3000 else wizardElo
            wizardElo = StrengthTier.snapElo((cur - 100).coerceAtLeast(200))
            seek.progress = (wizardElo - 200).coerceIn(0, 2800)
            eloTv.text = eloLabel()
        }
        val rangeTv = TextView(this).apply {
            text = "${formatArabicNumber(200)} — ${formatArabicNumber(3000)}"
            textSize = 11f
            setTextColor(palette.textSecondary)
            gravity = Gravity.CENTER
        }
        val plusBtn = pillIconButton(OverlayIconType.PLUS, palette, heightDp = 34) {
            if (wizardElo in 1 until 3000) {
                wizardElo = StrengthTier.snapElo((wizardElo + 100).coerceAtMost(3000))
                seek.progress = (wizardElo - 200).coerceIn(0, 2800)
            } else {
                wizardElo = -1
                seek.progress = 2850
            }
            eloTv.text = eloLabel()
        }
        stepRow.addView(minusBtn, LinearLayout.LayoutParams(dp(44), dp(34)))
        stepRow.addView(rangeTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        stepRow.addView(plusBtn, LinearLayout.LayoutParams(dp(44), dp(34)))
        card.addView(stepRow)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val startBtn = pillActionButton("ابدأ المباراة", palette, isPrimary = true, heightDp = 42) {
            playerColor = wizardColor
            flipped = playerColor == PieceColor.BLACK
            boardState = ChessBoardState.initial()
            moveStack.clear()
            selectedSquare = null
            legalForSelected = emptyList()
            lastMove = null
            mode = OverlayMode.BOARD
            serviceScope.launch {
                if (wizardElo <= 0) {
                    prefsRepo.setStrengthTier(StrengthTier.MAX)
                } else {
                    prefsRepo.setCustomEngineElo(wizardElo)
                }
                prefsRepo.setEnginePersonality(wizardPersonality)
            }
            rebuildContent()
            if (playerColor == PieceColor.BLACK) {
                triggerEngineReply()
            }
        }
        val backBtn = pillActionButton("رجوع", palette, isPrimary = false, heightDp = 42) {
            mode = OverlayMode.WIZARD_STEP1
            rebuildContent()
        }
        actionRow.addView(startBtn, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(6) })
        actionRow.addView(backBtn, LinearLayout.LayoutParams(0, dp(42), 1f))
        card.addView(actionRow, LinearLayout.LayoutParams(cardWidthPx - dp(12), LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun handleOverlaySquareTap(sq: Square) {
        if (isThinking || boardState.turn != playerColor) return
        if (boardState.evaluateGameResult() != GameResult.ONGOING) return

        if (selectedSquare != null) {
            val matching = legalForSelected.filter { it.to == sq }
            if (matching.isNotEmpty()) {
                val chosen = matching.find { it.promotion == PieceType.QUEEN } ?: matching.first()
                moveStack.add(boardState to chosen)
                boardState = boardState.copyWithMove(chosen)
                lastMove = chosen
                selectedSquare = null
                legalForSelected = emptyList()
                emitMoveFeedback(chosen, boardState)
                rebuildContent()

                if (boardState.evaluateGameResult() == GameResult.ONGOING) {
                    triggerEngineReply()
                }
                return
            }
        }

        val p = boardState.pieceAt(sq)
        if (p != null && p.color == playerColor) {
            selectedSquare = sq
            legalForSelected = boardState.generateLegalMoves().filter { it.from == sq }
        } else {
            selectedSquare = null
            legalForSelected = emptyList()
        }
        rebuildContent()
    }

    private fun triggerEngineReply() {
        isThinking = true
        serviceScope.launch {
            delay(220)
            val result = StockfishUciEngine.computeMove(
                context = applicationContext,
                state = boardState,
                backendPreference = prefs.engineBackendPreference,
                targetElo = wizardElo,
                personality = wizardPersonality,
                humanStyle = prefs.humanStyleEnabled,
                moveTimeMs = prefs.stockfishMoveTimeMs,
                threads = prefs.stockfishThreads,
                personalityIntensity = prefs.personalityIntensity,
                personalityRisk = prefs.personalityRisk
            )
            val move = result.bestMove
            if (move != null) {
                moveStack.add(boardState to move)
                boardState = boardState.copyWithMove(move)
                lastMove = move
                emitMoveFeedback(move, boardState)
            }
            isThinking = false
            rebuildContent()
        }
    }

    private fun emitMoveFeedback(move: ChessMove, after: ChessBoardState) {
        val res = after.evaluateGameResult()
        when {
            res != GameResult.ONGOING -> {
                val won = (res == GameResult.WHITE_WINS_CHECKMATE && playerColor == PieceColor.WHITE) ||
                    (res == GameResult.BLACK_WINS_CHECKMATE && playerColor == PieceColor.BLACK)
                soundManager.play(if (won) ChessSound.VICTORY else if (res.isDraw) ChessSound.MOVE else ChessSound.DEFEAT)
                hapticsManager.trigger(ChessHapticEvent.GAME_END)
            }
            after.isInCheck(after.turn) -> {
                soundManager.play(ChessSound.CHECK)
                hapticsManager.trigger(ChessHapticEvent.CHECK)
            }
            move.isCastleKingside || move.isCastleQueenside -> {
                soundManager.play(ChessSound.CASTLE)
                hapticsManager.trigger(ChessHapticEvent.CASTLE)
            }
            move.capturedPiece != null || move.isEnPassant -> {
                soundManager.play(ChessSound.CAPTURE)
                hapticsManager.trigger(ChessHapticEvent.CAPTURE)
            }
            else -> {
                soundManager.play(ChessSound.MOVE)
                hapticsManager.trigger(ChessHapticEvent.MOVE)
            }
        }
    }

    private fun undoOverlayMoves() {
        if (moveStack.isEmpty() || isThinking) return
        val dropCount = if (boardState.turn == playerColor && moveStack.size >= 2) 2 else 1
        repeat(dropCount.coerceAtMost(moveStack.size)) {
            val prev = moveStack.removeAt(moveStack.lastIndex)
            boardState = prev.first
        }
        lastMove = moveStack.lastOrNull()?.second
        selectedSquare = null
        legalForSelected = emptyList()
        rebuildContent()
    }

    private fun attachDragAndClick(view: View, onClick: (() -> Unit)?) {
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        view.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchStartX).toInt()
                    params.y = initialY + (event.rawY - touchStartY).toInt()
                    rootView?.let { windowManager.updateViewLayout(it, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = abs(event.rawX - touchStartX)
                    val dy = abs(event.rawY - touchStartY)
                    if (dx < 12f && dy < 12f && onClick != null) {
                        onClick()
                    } else {
                        val savedX = pxToDp(params.x)
                        val savedY = pxToDp(params.y)
                        prefs = prefs.copy(floatingX = savedX, floatingY = savedY)
                        serviceScope.launch {
                            prefsRepo.saveFloatingPosition(savedX, savedY)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Smooth flicker-free corner resize: updates card and chessboard dimensions directly
     * during ACTION_MOVE without recreating the view hierarchy, persisting on ACTION_UP.
     */
    private fun attachCornerResizeHandle(
        handle: View,
        card: LinearLayout,
        boardView: View,
        footer: View,
        innerPadPx: Int
    ) {
        var startWidthDp = clampedWidthDp()
        var touchStartX = 0f
        var touchStartY = 0f
        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startWidthDp = clampedWidthDp()
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaPx = ((event.rawX - touchStartX) + (event.rawY - touchStartY)) * 0.55f
                    val deltaDp = (deltaPx / resources.displayMetrics.density).roundToInt()
                    val nextWidth = (startWidthDp + deltaDp).coerceIn(180, maxAllowedWidthDp())
                    if (nextWidth != prefs.floatingWidthDp) {
                        prefs = prefs.copy(floatingWidthDp = nextWidth)
                        val nextCardPx = dp(nextWidth)
                        val nextBoardPx = (nextCardPx - innerPadPx).coerceAtLeast(dp(150))
                        card.layoutParams = (card.layoutParams as FrameLayout.LayoutParams).apply {
                            width = nextCardPx
                        }
                        boardView.layoutParams = (boardView.layoutParams as LinearLayout.LayoutParams).apply {
                            width = nextBoardPx
                            height = nextBoardPx
                        }
                        footer.layoutParams = (footer.layoutParams as LinearLayout.LayoutParams).apply {
                            width = nextBoardPx
                        }
                        rootView?.let { rv ->
                            layoutParams?.let { lp -> windowManager.updateViewLayout(rv, lp) }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val finalWidth = clampedWidthDp()
                    rebuildContent()
                    serviceScope.launch {
                        prefsRepo.saveFloatingCustomization(
                            finalWidth,
                            prefs.floatingFrameShape,
                            prefs.floatingOpacityPercent,
                            prefs.floatingBubbleSize
                        )
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun buildFenFromArray(arr: Array<ChessPiece?>, turn: PieceColor): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            var empty = 0
            for (c in 0..7) {
                val p = arr[r * 8 + c]
                if (p == null) empty++
                else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    sb.append(p.toFenChar())
                }
            }
            if (empty > 0) sb.append(empty)
            if (r < 7) sb.append('/')
        }
        sb.append(if (turn == PieceColor.WHITE) " w - - 0 1" else " b - - 0 1")
        return sb.toString()
    }

    companion object {
        const val ACTION_STOP = "com.example.overlay.STOP"
        const val EXTRA_FEN = "extra_fen"
        const val EXTRA_PLAYER_COLOR = "extra_player_color"
        private const val CHANNEL_ID = "floating_chess_overlay_channel"
        private const val NOTIF_ID = 4102

        fun canDrawOverlay(context: Context): Boolean =
            Settings.canDrawOverlays(context)

        fun launch(context: Context, fen: String? = null, playerColor: PieceColor? = null) {
            val intent = Intent(context, FloatingChessService::class.java).apply {
                if (fen != null) putExtra(EXTRA_FEN, fen)
                if (playerColor != null) putExtra(EXTRA_PLAYER_COLOR, playerColor.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
