package com.example.ui.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.BoardMaterial
import com.example.chess.ChessBoardState
import com.example.chess.ChessMove
import com.example.chess.PieceStyle
import com.example.chess.Square
import com.example.data.UserPreferences

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
        useRings: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var pieceStyle by remember(prefs.pieceStyle) { mutableStateOf(prefs.pieceStyle) }
    var boardMaterial by remember(prefs.boardMaterial) { mutableStateOf(prefs.boardMaterial) }
    var selectedColorHex by remember { mutableLongStateOf(prefs.selectedColorHex) }
    var lastMoveColorHex by remember { mutableLongStateOf(prefs.lastMoveColorHex) }
    var legalMoveColorHex by remember { mutableLongStateOf(prefs.legalMoveColorHex) }
    var showLastMove by remember { mutableStateOf(prefs.showLastMove) }
    var showLegalMoves by remember { mutableStateOf(prefs.showLegalMoves) }
    var useRings by remember { mutableStateOf(prefs.useRingsInsteadOfDots) }

    // Interactive preview board (Italian-style opening position like screenshot)
    val previewBoard = remember {
        ChessBoardState.fromFen("rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3")
    }
    var previewSelected by remember { mutableStateOf<Square?>(Square.fromAlgebraic("f3")) }
    val previewLastMove = remember {
        ChessMove(Square.fromAlgebraic("g1")!!, Square.fromAlgebraic("f3")!!)
    }
    val previewLegal = remember(previewSelected) {
        if (previewSelected == null) emptyList()
        else previewBoard.generateLegalMoves().filter { it.from == previewSelected }
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

        // Interactive Mini Board Preview
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(188.dp)) {
                    ChessBoardView(
                        boardState = previewBoard,
                        boardTheme = prefs.boardTheme,
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
                        onSquareClick = { sq ->
                            previewSelected = if (previewSelected == sq) null else sq
                        }
                    )
                }
                Text(
                    text = "اختر قطعة في المعاينة لتجربة العلامات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Piece Sets (طقم القطع)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("طقم القطع", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PieceStyle.entries) { style ->
                        val isSelected = pieceStyle == style
                        Surface(
                            onClick = { pieceStyle = style },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = style.nameAr,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
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
                            shape = RoundedCornerShape(16.dp),
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

        // Marker Color Rows (علامات النقلات)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("علامات النقلات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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

        // 3 Marker Toggles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppearanceSwitchRow("إظهار آخر نقلة", showLastMove) { showLastMove = it }
                AppearanceSwitchRow("إظهار النقلات القانونية", showLegalMoves) { showLegalMoves = it }
                AppearanceSwitchRow("حلقات بدل نقاط النقلات", useRings) { useRings = it }
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
                            useRings
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
