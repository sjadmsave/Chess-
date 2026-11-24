package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chess.BoardMaterial
import com.example.chess.BoardTheme
import com.example.chess.ChessBoardState
import com.example.chess.ChessMove
import com.example.chess.ChessPiece
import com.example.chess.LastMoveHighlightStyle
import com.example.chess.MultiPvLine
import com.example.chess.PieceAnimationStyle
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.data.UserPreferences
import com.example.ui.theme.formatArabicNumber

private val MARKER_SWATCHES = listOf(
    0xAA0D9488L, // Teal
    0xAA2563EBL, // Royal Blue
    0xAA81B64CL, // Leaf Green
    0xAAF5B041L, // Gold
    0xAA8B5CF6L, // Purple
    0xAAEC4899L, // Rose
    0x77769656L, // Olive
    0xCC0D9488L  // Deep Cyan
)

private data class QuickStylePreset(
    val id: String,
    val titleAr: String,
    val boardTheme: BoardTheme,
    val pieceStyle: PieceStyle,
    val material: BoardMaterial
)

private val QUICK_STYLE_PRESETS = listOf(
    QuickStylePreset("stone", "حجري", BoardTheme.STONE_SAND, PieceStyle.CHESSNUT, BoardMaterial.FLAT),
    QuickStylePreset("midnight", "ليلي", BoardTheme.MIDNIGHT_SLATE, PieceStyle.ALPHA, BoardMaterial.FLAT),
    QuickStylePreset("cadet", "أزرق", BoardTheme.CADET_BLUE, PieceStyle.CBURNETT, BoardMaterial.FLAT),
    QuickStylePreset("wood", "خشبي", BoardTheme.WOOD_MARKS, PieceStyle.STAUNTY, BoardMaterial.WOOD)
)

@Composable
fun BoardCustomizationSheetContent(
    prefs: UserPreferences,
    onSave: (
        pieceStyle: PieceStyle,
        boardMaterial: BoardMaterial,
        selectedColorHex: Long,
        lastMoveColorHex: Long,
        legalMoveColorHex: Long,
        showLastMove: Boolean,
        showLegalMoves: Boolean,
        useRings: Boolean,
        lastMoveHighlightStyle: LastMoveHighlightStyle,
        pieceAnimationStyle: PieceAnimationStyle,
        showCoordinates: Boolean,
        boardTheme: BoardTheme
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var boardTheme by remember(prefs.boardTheme) { mutableStateOf(prefs.boardTheme) }
    var pieceStyle by remember(prefs.pieceStyle) { mutableStateOf(prefs.pieceStyle) }
    var boardMaterial by remember(prefs.boardMaterial) { mutableStateOf(prefs.boardMaterial) }
    var selectedColorHex by remember { mutableLongStateOf(prefs.selectedColorHex) }
    var lastMoveColorHex by remember { mutableLongStateOf(prefs.lastMoveColorHex) }
    var legalMoveColorHex by remember { mutableLongStateOf(prefs.legalMoveColorHex) }
    var showLastMove by remember { mutableStateOf(prefs.showLastMove) }
    var showLegalMoves by remember { mutableStateOf(prefs.showLegalMoves) }
    var useRings by remember { mutableStateOf(prefs.useRingsInsteadOfDots) }
    var lastMoveHighlightStyle by remember(prefs.lastMoveHighlightStyle) {
        mutableStateOf(prefs.lastMoveHighlightStyle)
    }
    var pieceAnimationStyle by remember(prefs.pieceAnimationStyle) {
        mutableStateOf(prefs.pieceAnimationStyle)
    }
    var showCoordinates by remember(prefs.showCoordinates) { mutableStateOf(prefs.showCoordinates) }

    // Interactive preview board (Italian opening sample with engine arrow & legal targets)
    val previewBoard = remember {
        ChessBoardState.fromFen("r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4")
    }
    var previewSelected by remember { mutableStateOf<Square?>(Square.fromAlgebraic("f3")) }
    val previewLastMove = remember {
        ChessMove(Square.fromAlgebraic("f1")!!, Square.fromAlgebraic("c4")!!)
    }
    val previewLegal = remember(previewSelected) {
        if (previewSelected == null) emptyList()
        else previewBoard.generateLegalMoves().filter { it.from == previewSelected }
    }
    val previewArrows = remember {
        listOf(
            MultiPvLine(
                rank = 1,
                move = ChessMove(Square.fromAlgebraic("c2")!!, Square.fromAlgebraic("c3")!!),
                san = "c3",
                evalWhiteCp = 35
            )
        )
    }

    var activeColorTarget by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "مظهر الرقعة والقطع",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Interactive Live Preview Board
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(204.dp)) {
                    ChessBoardView(
                        boardState = previewBoard,
                        boardTheme = boardTheme,
                        pieceStyle = pieceStyle,
                        boardMaterial = boardMaterial,
                        selectedSquare = previewSelected,
                        legalMovesForSelected = previewLegal,
                        lastMove = previewLastMove,
                        hintMove = null,
                        flipped = false,
                        interactive = true,
                        showLastMove = showLastMove,
                        showLegalMoves = showLegalMoves,
                        useRingsInsteadOfDots = useRings,
                        selectedHighlightColor = Color(selectedColorHex),
                        lastMoveHighlightColor = Color(lastMoveColorHex),
                        legalMoveIndicatorColor = Color(legalMoveColorHex),
                        lastMoveHighlightStyle = lastMoveHighlightStyle,
                        showCoordinates = showCoordinates,
                        analysisLines = previewArrows,
                        showEngineArrowsOnBoard = true,
                        showMultiPvArrowNumbers = false,
                        pieceAnimationStyle = pieceAnimationStyle,
                        onSquareClick = { sq ->
                            previewSelected = if (previewSelected == sq) null else sq
                        }
                    )
                }
                Text(
                    text = "اضغط على أي قطعة في المعاينة لتجربة ألوان التحديد والأسهم",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Preset Styles (أنماط جاهزة)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("أنماط جاهزة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QUICK_STYLE_PRESETS, key = { it.id }) { preset ->
                        val isActive = boardTheme == preset.boardTheme && pieceStyle == preset.pieceStyle
                        Surface(
                            onClick = {
                                boardTheme = preset.boardTheme
                                pieceStyle = preset.pieceStyle
                                boardMaterial = preset.material
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isActive) 1.5.dp else 1.dp,
                                if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SplitBoardPreviewBadge(
                                    theme = preset.boardTheme,
                                    pieceStyle = preset.pieceStyle,
                                    sizeDp = 44
                                )
                                Text(
                                    text = preset.titleAr,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Piece Sets (16 أطقم قطع متضمنة أطقم PyChess العالمية)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "طقم القطع (${formatArabicNumber(PieceStyle.entries.size)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PieceStyle.entries, key = { it.id }) { style ->
                        val isSelected = pieceStyle == style
                        Surface(
                            onClick = { pieceStyle = style },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.testTag("piece_set_${style.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(86.dp)
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(boardTheme.darkSquare.copy(alpha = 0.28f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ChessPieceGraphic(
                                        piece = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE),
                                        pieceStyle = style,
                                        pieceSize = 36.dp
                                    )
                                }
                                Text(
                                    text = style.nameAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Board Color Themes (12 ثيم رقعة)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ألوان الرقعة (${formatArabicNumber(BoardTheme.entries.size)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BoardTheme.entries, key = { it.id }) { theme ->
                        val isSelected = boardTheme == theme
                        Surface(
                            onClick = { boardTheme = theme },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.testTag("board_theme_${theme.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(88.dp)
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SplitBoardPreviewBadge(
                                    theme = theme,
                                    pieceStyle = pieceStyle,
                                    sizeDp = 42
                                )
                                Text(
                                    text = theme.nameAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Board Material (خامة الرقعة)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("خامة الرقعة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardMaterial.entries.forEach { mat ->
                        val isSelected = boardMaterial == mat
                        Surface(
                            onClick = { boardMaterial = mat },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mat.nameAr,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // Last Move Highlight Style (نمط تمييز آخر نقلة)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تمييز آخر نقلة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LastMoveHighlightStyle.entries.forEach { lmStyle ->
                        val isSelected = lastMoveHighlightStyle == lmStyle
                        Surface(
                            onClick = {
                                lastMoveHighlightStyle = lmStyle
                                showLastMove = lmStyle != LastMoveHighlightStyle.NONE
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lmStyle.nameAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Piece Animation Style (حركة القطع)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("حركة القطع", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PieceAnimationStyle.entries.forEach { anim ->
                        val isSelected = pieceAnimationStyle == anim
                        Surface(
                            onClick = { pieceAnimationStyle = anim },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = anim.nameAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Marker Color Rows (علامات النقلات)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ألوان العلامات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                MarkerColorRow(
                    title = "لون تحديد القطعة",
                    colorHex = selectedColorHex,
                    expanded = activeColorTarget == "selected",
                    onToggleExpand = {
                        activeColorTarget = if (activeColorTarget == "selected") null else "selected"
                    },
                    onPickColor = { selectedColorHex = it }
                )
                MarkerColorRow(
                    title = "لون آخر نقلة",
                    colorHex = lastMoveColorHex,
                    expanded = activeColorTarget == "last",
                    onToggleExpand = {
                        activeColorTarget = if (activeColorTarget == "last") null else "last"
                    },
                    onPickColor = { lastMoveColorHex = it }
                )
                MarkerColorRow(
                    title = "لون النقلات القانونية",
                    colorHex = legalMoveColorHex,
                    expanded = activeColorTarget == "legal",
                    onToggleExpand = {
                        activeColorTarget = if (activeColorTarget == "legal") null else "legal"
                    },
                    onPickColor = { legalMoveColorHex = it }
                )
            }
        }

        // Toggles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppearanceSwitchRow("إظهار آخر نقلة", showLastMove) { showLastMove = it }
                AppearanceSwitchRow("إظهار النقلات القانونية", showLegalMoves) { showLegalMoves = it }
                AppearanceSwitchRow("حلقات بدل نقاط النقلات", useRings) { useRings = it }
                AppearanceSwitchRow("عرض إحداثيات الرقعة (a-h / 1-8)", showCoordinates) { showCoordinates = it }
            }
        }

        // Save / Cancel Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء")
                }
                Button(
                    onClick = {
                        onSave(
                            pieceStyle,
                            boardMaterial,
                            selectedColorHex,
                            lastMoveColorHex,
                            legalMoveColorHex,
                            showLastMove,
                            showLegalMoves,
                            useRings,
                            lastMoveHighlightStyle,
                            pieceAnimationStyle,
                            showCoordinates,
                            boardTheme
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_board_appearance_btn")
                ) {
                    Text("حفظ")
                }
            }
        }
    }
}

@Composable
private fun SplitBoardPreviewBadge(
    theme: BoardTheme,
    pieceStyle: PieceStyle,
    sizeDp: Int
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, theme.boardBorder, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = theme.lightSquare)
            val path = Path().apply {
                moveTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, color = theme.darkSquare)
        }
        ChessPieceGraphic(
            piece = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE),
            pieceStyle = pieceStyle,
            pieceSize = (sizeDp * 0.72f).dp
        )
    }
}

@Composable
private fun MarkerColorRow(
    title: String,
    colorHex: Long,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onPickColor: (Long) -> Unit
) {
    Surface(
        onClick = onToggleExpand,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(colorHex))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MARKER_SWATCHES.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (hex == colorHex) 2.5.dp else 1.dp,
                                    color = if (hex == colorHex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onPickColor(hex) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
