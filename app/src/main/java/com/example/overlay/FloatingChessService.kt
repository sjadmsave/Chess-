package com.example.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.ChessHapticEvent
import com.example.audio.ChessSound
import com.example.audio.HapticsManager
import com.example.audio.SoundManager
import com.example.chess.BoardTheme
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
import kotlin.math.roundToInt

/**
 * Floating Chess Overlay Service adhering to Ruqaa Specification Section 11:
 * - TYPE_APPLICATION_OVERLAY, FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_IN_SCREEN
 * - Default position x = 12 dp, y = 72 dp, persisted after drag
 * - Width 180..560 dp (default 270 dp) with 44 dp corner resize handle + persisted width
 * - Opacity 65..100% (default 100%), Frame shapes (12/7/0 dp), Bubble size (44/52/64 dp)
 * - Notification channel "النافذة العائمة" with IMPORTANCE_LOW
 * - Full tools: Menu (Details, Resize, Bubble, Open Ruqaa, New Game Wizard, Position Editor)
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

    private fun pxToDp(px: Int): Int {
        val density = resources.displayMetrics.density
        return if (density <= 0f) px else (px / density).roundToInt()
    }

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

        if (mode == OverlayMode.BUBBLE) {
            val bubblePx = dp(prefs.floatingBubbleSize.sizeDp)
            val bubble = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#006A87"))
                    setStroke(dp(2), Color.parseColor("#72D9EF"))
                }
                elevation = dp(8).toFloat()
            }
            val iconText = TextView(this).apply {
                text = "♜"
                setTextColor(Color.WHITE)
                textSize = (prefs.floatingBubbleSize.sizeDp * 0.44f)
                gravity = Gravity.CENTER
            }
            bubble.addView(
                iconText,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            attachDragAndClick(bubble, onClick = {
                mode = OverlayMode.BOARD
                rebuildContent()
            })
            container.addView(bubble, FrameLayout.LayoutParams(bubblePx, bubblePx))
            return
        }

        val widthDp = prefs.floatingWidthDp.coerceIn(180, 560)
        val widthPx = dp(widthDp)
        val alphaInt = ((prefs.floatingOpacityPercent.coerceIn(65, 100) / 100f) * 255).toInt()
        val cardBg = Color.argb(alphaInt, 21, 34, 56)
        val strokeColor = Color.parseColor("#2A3D5E")
        val cornerPx = dp(prefs.floatingFrameShape.cornerDp).toFloat()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBg)
                cornerRadius = cornerPx
                setStroke(dp(1), strokeColor)
            }
            setPadding(dp(6), dp(6), dp(6), dp(6))
            elevation = dp(10).toFloat()
        }

        when (mode) {
            OverlayMode.BOARD -> buildBoardOverlayUi(card, widthPx)
            OverlayMode.MENU -> buildMenuOverlayUi(card, widthPx)
            OverlayMode.DETAILS -> buildDetailsOverlayUi(card, widthPx)
            OverlayMode.RESIZE -> buildResizeOverlayUi(card, widthPx)
            OverlayMode.WIZARD_STEP1 -> buildWizardStep1Ui(card, widthPx)
            OverlayMode.WIZARD_STEP2 -> buildWizardStep2Ui(card, widthPx)
            OverlayMode.EDITOR -> buildEditorOverlayUi(card, widthPx)
            OverlayMode.BUBBLE -> Unit
        }

        container.addView(
            card,
            FrameLayout.LayoutParams(widthPx, FrameLayout.LayoutParams.WRAP_CONTENT)
        )
    }

    private fun buildTopBar(card: LinearLayout) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(5))
        }

        fun iconBtn(symbol: String, enabled: Boolean = true, onClick: () -> Unit): TextView {
            return TextView(this).apply {
                text = symbol
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(if (enabled) Color.parseColor("#F1F5FA") else Color.parseColor("#64748B"))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E2E4A"))
                    cornerRadius = dp(10).toFloat()
                    setStroke(dp(1), Color.parseColor("#2E4366"))
                }
                setPadding(dp(6), dp(4), dp(6), dp(4))
                if (enabled) setOnClickListener { onClick() }
            }
        }

        val menuBtn = iconBtn("•••") {
            mode = if (mode == OverlayMode.MENU) OverlayMode.BOARD else OverlayMode.MENU
            rebuildContent()
        }
        val editBtn = iconBtn("✎") {
            editorBoard = boardState.board.copyOf()
            editorTurn = boardState.turn
            mode = OverlayMode.EDITOR
            rebuildContent()
        }
        val undoBtn = iconBtn("↩", enabled = moveStack.isNotEmpty() && !isThinking) {
            undoOverlayMoves()
        }
        val flipBtn = iconBtn("⇅") {
            flipped = !flipped
            rebuildContent()
        }
        val bubbleBtn = iconBtn("—") {
            mode = OverlayMode.BUBBLE
            rebuildContent()
        }
        val dragHandle = TextView(this).apply {
            text = "⋮⋮"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#72D9EF"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#006A87"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#72D9EF"))
            }
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        attachDragAndClick(dragHandle, onClick = null)

        val buttons = listOf(menuBtn, editBtn, undoBtn, flipBtn, bubbleBtn, dragHandle)
        buttons.forEachIndexed { idx, v ->
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (idx < buttons.lastIndex) marginEnd = dp(4)
            }
            bar.addView(v, lp)
        }
        card.addView(bar)
    }

    private fun buildBoardOverlayUi(card: LinearLayout, cardWidthPx: Int) {
        buildTopBar(card)
        val boardPx = cardWidthPx - dp(12)
        val boardFrame = FrameLayout(this)

        val boardView = FloatingBoardCanvasView(this).apply {
            updateState(
                state = boardState,
                theme = prefs.boardTheme,
                flipped = flipped,
                selected = selectedSquare,
                legalTargets = legalForSelected.map { it.to }.toSet(),
                lastMove = lastMove,
                useRings = prefs.useRingsInsteadOfDots,
                legalColorInt = prefs.legalMoveColorHex.toInt(),
                selectedColorInt = prefs.selectedColorHex.toInt()
            )
            onSquareTapped = { sq ->
                handleOverlaySquareTap(sq)
            }
        }
        boardFrame.addView(boardView, FrameLayout.LayoutParams(boardPx, boardPx))

        // Ruqaa Section 11: 44dp corner resize handle
        val handlePx = dp(44)
        val resizeHandle = TextView(this).apply {
            text = "⤢"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#72D9EF"))
            background = GradientDrawable().apply {
                setColor(Color.argb(190, 11, 19, 36))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#72D9EF"))
            }
        }
        attachCornerResizeHandle(resizeHandle)
        val handleLp = FrameLayout.LayoutParams(handlePx, handlePx).apply {
            gravity = Gravity.BOTTOM or Gravity.END
        }
        boardFrame.addView(resizeHandle, handleLp)

        card.addView(boardFrame, LinearLayout.LayoutParams(boardPx, boardPx))
    }

    private fun buildMenuOverlayUi(card: LinearLayout, cardWidthPx: Int) {
        buildTopBar(card)

        val statusText = TextView(this).apply {
            val res = boardState.evaluateGameResult()
            text = when {
                res != GameResult.ONGOING -> res.labelAr
                isThinking -> "المحرك يفكر..."
                boardState.turn == playerColor -> "دورك"
                else -> "دور المحرك"
            }
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F1F5FA"))
            gravity = Gravity.END
            setPadding(dp(6), dp(2), dp(6), dp(6))
        }
        card.addView(statusText, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun menuItem(title: String, icon: String, isDanger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E2E4A"))
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), Color.parseColor("#2E4366"))
                }
                setPadding(dp(10), dp(9), dp(10), dp(9))
                if (enabled) setOnClickListener { onClick() }
            }
            val iconTv = TextView(this).apply {
                text = icon
                textSize = 14f
                setTextColor(if (isDanger) Color.parseColor("#F87171") else Color.parseColor("#72D9EF"))
                setPadding(0, 0, dp(8), 0)
            }
            val titleTv = TextView(this).apply {
                text = title
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(
                    when {
                        !enabled -> Color.parseColor("#64748B")
                        isDanger -> Color.parseColor("#F87171")
                        else -> Color.parseColor("#F1F5FA")
                    }
                )
                gravity = Gravity.END
            }
            btn.addView(iconTv)
            btn.addView(titleTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(6)
            }
            list.addView(btn, lp)
        }

        menuItem("عرض الرقعة", "♟") {
            mode = OverlayMode.BOARD
            rebuildContent()
        }
        menuItem("تفاصيل الجلسة والوضعية", "ⓘ") {
            mode = OverlayMode.DETAILS
            rebuildContent()
        }
        menuItem("تغيير الحجم (${formatArabicNumber(prefs.floatingWidthDp)} dp)", "⤢") {
            mode = OverlayMode.RESIZE
            rebuildContent()
        }
        menuItem("تراجع عن النقلة", "↩", enabled = moveStack.isNotEmpty() && !isThinking) {
            undoOverlayMoves()
            mode = OverlayMode.BOARD
            rebuildContent()
        }
        menuItem("محرر الوضعية", "✎") {
            editorBoard = boardState.board.copyOf()
            editorTurn = boardState.turn
            mode = OverlayMode.EDITOR
            rebuildContent()
        }
        menuItem("إعداد مباراة جديدة", "✦") {
            mode = OverlayMode.WIZARD_STEP1
            rebuildContent()
        }
        menuItem("تصغير إلى فقاعة", "—") {
            mode = OverlayMode.BUBBLE
            rebuildContent()
        }
        menuItem("فتح رُقعة", "↗") {
            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_FEN, boardState.toFen())
                putExtra(EXTRA_PLAYER_COLOR, playerColor.name)
            }
            startActivity(openIntent)
            stopSelf()
        }
        menuItem("إغلاق النافذة", "✕", isDanger = true) {
            stopSelf()
        }

        scroll.addView(list)
        card.addView(scroll, LinearLayout.LayoutParams(cardWidthPx - dp(12), dp(235)))
    }

    private fun buildDetailsOverlayUi(card: LinearLayout, cardWidthPx: Int) {
        buildWizardHeader(card, "تفاصيل المباراة")
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
            setTextColor(Color.parseColor("#CBD5E1"))
            gravity = Gravity.END
            setPadding(dp(6), dp(4), dp(6), dp(10))
        }
        card.addView(details, LinearLayout.LayoutParams(cardWidthPx - dp(12), LinearLayout.LayoutParams.WRAP_CONTENT))

        val backBtn = Button(this).apply {
            text = "العودة للرقعة"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#006A87"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                mode = OverlayMode.BOARD
                rebuildContent()
            }
        }
        card.addView(backBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)))
    }

    private fun buildResizeOverlayUi(card: LinearLayout, cardWidthPx: Int) {
        buildWizardHeader(card, "تغيير حجم النافذة")
        var pendingWidth = prefs.floatingWidthDp.coerceIn(180, 560)
        val sizeTv = TextView(this).apply {
            text = "العرض: ${formatArabicNumber(pendingWidth)} dp"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#72D9EF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(6))
        }
        card.addView(sizeTv)

        val seek = SeekBar(this).apply {
            max = 380 // 180..560
            progress = (pendingWidth - 180).coerceIn(0, 380)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    pendingWidth = (p + 180).coerceIn(180, 560)
                    sizeTv.text = "العرض: ${formatArabicNumber(pendingWidth)} dp"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        card.addView(seek)

        val applyBtn = Button(this).apply {
            text = "تطبيق العرض"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#006A87"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
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
        }
        card.addView(applyBtn, LinearLayout.LayoutParams(cardWidthPx - dp(12), dp(44)).apply { topMargin = dp(8) })
    }

    private fun buildEditorOverlayUi(card: LinearLayout, cardWidthPx: Int) {
        buildWizardHeader(card, "محرر الوضعية")
        val boardPx = (cardWidthPx - dp(12)).coerceAtMost(dp(240))

        val editorView = FloatingEditorBoardView(this).apply {
            updateBoard(editorBoard, prefs.boardTheme)
            onSquareTapped = { sq ->
                val idx = sq.index
                editorBoard[idx] = if (editorBoard[idx] == editorBrush) null else editorBrush
                updateBoard(editorBoard, prefs.boardTheme)
            }
        }
        card.addView(editorView, LinearLayout.LayoutParams(boardPx, boardPx).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })

        // Brush strip (White Q,R,B,N,P / Black Q,R,B,N,P / K / Eraser)
        val paletteRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(2))
        }
        val paletteRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(2), 0, dp(4))
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
            items.forEach { (glyph, piece) ->
                val active = editorBrush == piece
                val tv = TextView(this).apply {
                    text = glyph
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        setColor(if (active) Color.parseColor("#006A87") else Color.parseColor("#1E2E4A"))
                        cornerRadius = dp(8).toFloat()
                    }
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    setOnClickListener {
                        editorBrush = piece
                        rebuildContent()
                    }
                }
                row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(2) })
            }
        }
        populatePalette(paletteRow1, brushes1)
        populatePalette(paletteRow2, brushes2)
        card.addView(paletteRow1)
        card.addView(paletteRow2)

        val turnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(4))
        }
        val turnBtn = Button(this).apply {
            text = if (editorTurn == PieceColor.WHITE) "الدور: الأبيض" else "الدور: الأسود"
            isAllCaps = false
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { setColor(Color.parseColor("#1E2E4A")); cornerRadius = dp(10).toFloat() }
            setOnClickListener {
                editorTurn = editorTurn.opposite()
                rebuildContent()
            }
        }
        val applyBtn = Button(this).apply {
            text = "تطبيق واللعب"
            isAllCaps = false
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { setColor(Color.parseColor("#006A87")); cornerRadius = dp(10).toFloat() }
            setOnClickListener {
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
        }
        turnRow.addView(turnBtn, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(4) })
        turnRow.addView(applyBtn, LinearLayout.LayoutParams(0, dp(40), 1f))
        card.addView(turnRow)
    }

    private fun buildWizardHeader(card: LinearLayout, heading: String = "مباراة عائمة") {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(8))
        }
        val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#F1F5FA"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E2E4A"))
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                mode = OverlayMode.BOARD
                rebuildContent()
            }
        }
        val title = TextView(this).apply {
            text = heading
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F1F5FA"))
            gravity = Gravity.CENTER
        }
        val drag = TextView(this).apply {
            text = "⋮⋮"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#72D9EF"))
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        attachDragAndClick(drag, onClick = null)

        header.addView(closeBtn)
        header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(drag)
        card.addView(header)
    }

    private fun buildWizardStep1Ui(card: LinearLayout, cardWidthPx: Int) {
        buildWizardHeader(card)
        val stepTv = TextView(this).apply {
            text = "الخطوة ١ من ٢"
            textSize = 11f
            setTextColor(Color.parseColor("#72D9EF"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }
        val titleTv = TextView(this).apply {
            text = "اختر لونك"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F1F5FA"))
            gravity = Gravity.END
            setPadding(0, dp(2), 0, dp(6))
        }
        card.addView(stepTv)
        card.addView(titleTv)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun colorChoice(label: String, icon: String, selected: Boolean, onClick: () -> Unit): Button {
            return Button(this).apply {
                text = "$label  $icon"
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(if (selected) Color.parseColor("#006A87") else Color.parseColor("#1E2E4A"))
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), Color.parseColor("#72D9EF"))
                }
                setOnClickListener { onClick() }
            }
        }
        val blackBtn = colorChoice("أسود", "♚", wizardColor == PieceColor.BLACK) {
            wizardColor = PieceColor.BLACK
            mode = OverlayMode.WIZARD_STEP2
            rebuildContent()
        }
        val whiteBtn = colorChoice("أبيض", "♔", wizardColor == PieceColor.WHITE) {
            wizardColor = PieceColor.WHITE
            mode = OverlayMode.WIZARD_STEP2
            rebuildContent()
        }
        row.addView(blackBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(6) })
        row.addView(whiteBtn, LinearLayout.LayoutParams(0, dp(44), 1f))
        card.addView(row, LinearLayout.LayoutParams(cardWidthPx - dp(12), LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun buildWizardStep2Ui(card: LinearLayout, cardWidthPx: Int) {
        buildWizardHeader(card)
        val stepTv = TextView(this).apply {
            text = "الخطوة ٢ من ٢"
            textSize = 11f
            setTextColor(Color.parseColor("#72D9EF"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }
        val sideNoteTv = TextView(this).apply {
            text = "ستُلعب المباراة ب${if (wizardColor == PieceColor.WHITE) "الأبيض" else "الأسود"}"
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            gravity = Gravity.END
            setPadding(0, dp(2), 0, dp(6))
        }
        card.addView(stepTv)
        card.addView(sideNoteTv)

        val personalityBtn = Button(this).apply {
            text = "الشخصية: ${wizardPersonality.nameAr}"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E2E4A"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                val all = EnginePersonality.entries
                val nextIdx = (all.indexOf(wizardPersonality) + 1) % all.size
                wizardPersonality = all[nextIdx]
                rebuildContent()
            }
        }
        card.addView(personalityBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
            bottomMargin = dp(6)
        })

        fun eloLabel(): String = if (wizardElo <= 0) "أقصى • بلا حد" else "نحو ${formatArabicNumber(wizardElo)} إيلو"

        val eloTv = TextView(this).apply {
            text = eloLabel()
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#72D9EF"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        card.addView(eloTv)

        val seek = SeekBar(this).apply {
            max = 2850 // 200..3000 + 3050 => unlimited
            progress = if (wizardElo <= 0) 2850 else (wizardElo - 200).coerceIn(0, 2800)
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

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }
        val startBtn = Button(this).apply {
            text = "ابدأ المباراة"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#006A87"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
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
        }
        val backBtn = Button(this).apply {
            text = "رجوع"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E2E4A"))
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                mode = OverlayMode.WIZARD_STEP1
                rebuildContent()
            }
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

    private fun attachCornerResizeHandle(handle: View) {
        var startWidthDp = prefs.floatingWidthDp
        var touchStartX = 0f
        var touchStartY = 0f
        handle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startWidthDp = prefs.floatingWidthDp
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaPx = ((event.rawX - touchStartX) + (event.rawY - touchStartY)) / 2f
                    val deltaDp = (deltaPx / resources.displayMetrics.density).roundToInt()
                    val nextWidth = (startWidthDp + deltaDp).coerceIn(180, 560)
                    if (abs(nextWidth - prefs.floatingWidthDp) >= 6) {
                        prefs = prefs.copy(floatingWidthDp = nextWidth)
                        rebuildContent()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val finalWidth = prefs.floatingWidthDp.coerceIn(180, 560)
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

private class FloatingBoardCanvasView(context: Context) : View(context) {
    private var boardState: ChessBoardState = ChessBoardState.initial()
    private var boardTheme: BoardTheme = BoardTheme.TOURNAMENT_EMERALD
    private var flipped: Boolean = false
    private var selected: Square? = null
    private var legalTargets: Set<Square> = emptySet()
    private var lastMove: ChessMove? = null
    private var useRings: Boolean = false
    private var legalColorInt: Int = Color.parseColor("#72D9EF")
    private var selectedColorInt: Int = Color.parseColor("#AA006A87")

    var onSquareTapped: ((Square) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val strokeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
    }

    fun updateState(
        state: ChessBoardState,
        theme: BoardTheme,
        flipped: Boolean,
        selected: Square?,
        legalTargets: Set<Square>,
        lastMove: ChessMove?,
        useRings: Boolean,
        legalColorInt: Int,
        selectedColorInt: Int
    ) {
        this.boardState = state
        this.boardTheme = theme
        this.flipped = flipped
        this.selected = selected
        this.legalTargets = legalTargets
        this.lastMove = lastMove
        this.useRings = useRings
        this.legalColorInt = legalColorInt
        this.selectedColorInt = selectedColorInt
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = width.coerceAtMost(height).toFloat()
        if (size <= 0f) return
        val cell = size / 8f

        val lm = lastMove
        val lightColor = boardTheme.lightSquare.toArgb()
        val darkColor = boardTheme.darkSquare.toArgb()
        val lastMoveColor = boardTheme.lastMoveHighlight.toArgb()

        for (vr in 0..7) {
            val r = if (flipped) 7 - vr else vr
            for (vc in 0..7) {
                val c = if (flipped) 7 - vc else vc
                val sq = Square(r, c)
                val isLight = (r + c) % 2 == 0
                val left = vc * cell
                val top = vr * cell

                paint.color = if (isLight) lightColor else darkColor
                canvas.drawRect(left, top, left + cell, top + cell, paint)

                if (selected == sq) {
                    paint.color = selectedColorInt
                    canvas.drawRect(left, top, left + cell, top + cell, paint)
                } else if (lm != null && (lm.from == sq || lm.to == sq)) {
                    paint.color = lastMoveColor
                    canvas.drawRect(left, top, left + cell, top + cell, paint)
                }

                if (vc == 0) {
                    paint.color = if (isLight) darkColor else lightColor
                    paint.textSize = cell * 0.22f
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText("${8 - r}", left + cell * 0.06f, top + cell * 0.25f, paint)
                }
                if (vr == 7) {
                    paint.color = if (isLight) darkColor else lightColor
                    paint.textSize = cell * 0.22f
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("${'a' + c}", left + cell * 0.94f, top + cell * 0.94f, paint)
                }

                val piece = boardState.pieceAt(sq)
                if (piece != null) {
                    val glyph = unicodePiece(piece)
                    val cx = left + cell * 0.5f
                    val cy = top + cell * 0.74f
                    val pSize = cell * 0.76f
                    strokeTextPaint.textSize = pSize
                    strokeTextPaint.strokeWidth = cell * 0.06f
                    strokeTextPaint.color = if (piece.color == PieceColor.WHITE) Color.parseColor("#1E293B") else Color.WHITE
                    canvas.drawText(glyph, cx, cy, strokeTextPaint)

                    textPaint.textSize = pSize
                    textPaint.color = if (piece.color == PieceColor.WHITE) Color.WHITE else Color.parseColor("#0F172A")
                    canvas.drawText(glyph, cx, cy, textPaint)
                }

                if (sq in legalTargets) {
                    val cx = left + cell * 0.5f
                    val cy = top + cell * 0.5f
                    if (piece != null) {
                        ringPaint.color = legalColorInt
                        ringPaint.strokeWidth = cell * 0.10f
                        canvas.drawCircle(cx, cy, cell * 0.42f, ringPaint)
                    } else if (useRings) {
                        ringPaint.color = legalColorInt
                        ringPaint.strokeWidth = cell * 0.08f
                        canvas.drawCircle(cx, cy, cell * 0.22f, ringPaint)
                    } else {
                        paint.color = legalColorInt
                        canvas.drawCircle(cx, cy, cell * 0.16f, paint)
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cell = width / 8f
            if (cell > 0f) {
                val vc = (event.x / cell).toInt().coerceIn(0, 7)
                val vr = (event.y / cell).toInt().coerceIn(0, 7)
                val c = if (flipped) 7 - vc else vc
                val r = if (flipped) 7 - vr else vr
                onSquareTapped?.invoke(Square(r, c))
                performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    private fun unicodePiece(p: ChessPiece): String = when (p.type) {
        PieceType.KING -> "♚"
        PieceType.QUEEN -> "♛"
        PieceType.ROOK -> "♜"
        PieceType.BISHOP -> "♝"
        PieceType.KNIGHT -> "♞"
        PieceType.PAWN -> "♟"
    }
}

private class FloatingEditorBoardView(context: Context) : View(context) {
    private var board: Array<ChessPiece?> = arrayOfNulls(64)
    private var theme: BoardTheme = BoardTheme.TOURNAMENT_EMERALD
    var onSquareTapped: ((Square) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    fun updateBoard(arr: Array<ChessPiece?>, boardTheme: BoardTheme) {
        this.board = arr
        this.theme = boardTheme
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = width.coerceAtMost(height).toFloat()
        if (size <= 0f) return
        val cell = size / 8f
        val lightColor = theme.lightSquare.toArgb()
        val darkColor = theme.darkSquare.toArgb()

        for (r in 0..7) {
            for (c in 0..7) {
                val left = c * cell
                val top = r * cell
                paint.color = if ((r + c) % 2 == 0) lightColor else darkColor
                canvas.drawRect(left, top, left + cell, top + cell, paint)
                val piece = board[r * 8 + c]
                if (piece != null) {
                    val glyph = when (piece.type) {
                        PieceType.KING -> "♚"
                        PieceType.QUEEN -> "♛"
                        PieceType.ROOK -> "♜"
                        PieceType.BISHOP -> "♝"
                        PieceType.KNIGHT -> "♞"
                        PieceType.PAWN -> "♟"
                    }
                    textPaint.textSize = cell * 0.72f
                    textPaint.color = if (piece.color == PieceColor.WHITE) Color.WHITE else Color.parseColor("#0F172A")
                    canvas.drawText(glyph, left + cell * 0.5f, top + cell * 0.74f, textPaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cell = width / 8f
            if (cell > 0f) {
                val c = (event.x / cell).toInt().coerceIn(0, 7)
                val r = (event.y / cell).toInt().coerceIn(0, 7)
                onSquareTapped?.invoke(Square(r, c))
                performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
