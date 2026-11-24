package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.ChessBoardState
import com.example.chess.ChessPuzzle
import com.example.chess.PieceColor
import com.example.chess.PuzzleCategory
import com.example.chess.PuzzleRepository
import com.example.chess.Square
import com.example.data.PuzzleRecordEntity
import com.example.data.UserPreferences
import com.example.ui.components.ChessBoardView
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.MoveBlunderColor
import com.example.ui.theme.formatArabicNumber
import com.example.viewmodel.ActivePuzzleUiState

@Composable
fun PuzzlesScreen(
    puzzleState: ActivePuzzleUiState,
    prefs: UserPreferences,
    solvedRecords: List<PuzzleRecordEntity>,
    onSquareClick: (Square) -> Unit,
    onShowHint: () -> Unit,
    onResetPuzzle: () -> Unit,
    onSelectPuzzle: (ChessPuzzle, Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableStateOf<PuzzleCategory?>(null) }
    var unsolvedOnly by remember { mutableStateOf(false) }
    val solvedIds = remember(solvedRecords) { solvedRecords.map { it.puzzleId }.toSet() }
    val dailyPuzzle = remember(prefs.playerElo) { PuzzleRepository.dailyPuzzleForToday(prefs.playerElo) }
    val puzzleInitialTurn = remember(puzzleState.puzzle.id) {
        ChessBoardState.fromFen(puzzleState.puzzle.fen).turn
    }
    val categoryCounts = remember {
        PuzzleCategory.entries.associateWith { cat -> PuzzleRepository.puzzles.count { it.category == cat } }
    }

    val filteredPuzzles = remember(selectedCategory, unsolvedOnly, solvedIds) {
        PuzzleRepository.puzzles.filter { p ->
            (selectedCategory == null || p.category == selectedCategory) &&
                (!unsolvedOnly || p.id !in solvedIds)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الألغاز والتحدي اليومي", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "تم حل ${formatArabicNumber(solvedIds.size)} من ${formatArabicNumber(PuzzleRepository.puzzles.size)} لغز تكتيكي",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "سلسلة ${formatArabicNumber(prefs.dailyStreak)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Active Puzzle Card + Board
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = puzzleState.puzzle.titleAr,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (puzzleState.isDaily) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "تحدي اليوم",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                             )
                                        }
                                    }
                                }
                                Text(
                                    text = "${puzzleState.puzzle.category.labelAr} • ${formatArabicNumber(puzzleState.puzzle.rating)} ELO • الدور ${if (puzzleInitialTurn == PieceColor.WHITE) "للأبيض" else "للأسود"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        ChessBoardView(
                            boardState = puzzleState.boardState,
                            boardTheme = prefs.boardTheme,
                            pieceStyle = prefs.pieceStyle,
                            selectedSquare = puzzleState.selectedSquare,
                            legalMovesForSelected = puzzleState.legalMovesForSelected,
                            lastMove = null,
                            hintMove = puzzleState.hintArrowMove,
                            flipped = puzzleInitialTurn == PieceColor.BLACK,
                            interactive = !puzzleState.isSolved,
                            boardMaterial = prefs.boardMaterial,
                            showLastMove = prefs.showLastMove,
                            showLegalMoves = prefs.showLegalMoves,
                            useRingsInsteadOfDots = prefs.useRingsInsteadOfDots,
                            selectedHighlightColor = Color(prefs.selectedColorHex),
                            lastMoveHighlightColor = Color(prefs.lastMoveColorHex),
                            legalMoveIndicatorColor = Color(prefs.legalMoveColorHex),
                            lastMoveHighlightStyle = prefs.lastMoveHighlightStyle,
                            showCoordinates = prefs.showCoordinates,
                            pieceAnimationStyle = prefs.pieceAnimationStyle,
                            enlargeOnDrag = prefs.enlargeOnDrag,
                            boardScalePercent = prefs.boardScalePercent,
                            onSquareClick = { sq ->
                                if (prefs.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSquareClick(sq)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Status / Feedback banner
                        val statusColor = when {
                            puzzleState.isSolved -> MaterialTheme.colorScheme.primary
                            puzzleState.isErrorFeedback -> MoveBlunderColor
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = statusColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = puzzleState.statusMessageAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (puzzleState.showHintText && !puzzleState.isSolved) {
                                    Text(
                                        text = "تلميح: ${puzzleState.puzzle.hintAr}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onShowHint,
                                enabled = !puzzleState.isSolved,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("puzzle_hint_button")
                            ) {
                                Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تلميح")
                            }
                            OutlinedButton(
                                onClick = onResetPuzzle,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("إعادة")
                            }
                            if (puzzleState.isSolved) {
                                val nextIdx = (PuzzleRepository.puzzles.indexOfFirst { it.id == puzzleState.puzzle.id } + 1) %
                                    PuzzleRepository.puzzles.size
                                Button(
                                    onClick = { onSelectPuzzle(PuzzleRepository.puzzles[nextIdx], false) },
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text("التالي")
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Chips + Unsolved Only Toggle
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = unsolvedOnly,
                            onClick = { unsolvedOnly = !unsolvedOnly },
                            label = { Text("غير المحلولة فقط") },
                            modifier = Modifier.testTag("puzzles_unsolved_filter")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("الكل (${formatArabicNumber(PuzzleRepository.puzzles.size)})") }
                        )
                    }
                    items(PuzzleCategory.entries) { cat ->
                        val count = categoryCounts[cat] ?: 0
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text("${cat.labelAr} (${formatArabicNumber(count)})") }
                        )
                    }
                }
            }

            // Puzzle Ladder List
            items(filteredPuzzles, key = { it.id }) { puzzle ->
                val isSolved = puzzle.id in solvedIds
                val isCurrent = puzzle.id == puzzleState.puzzle.id
                val isDaily = puzzle.id == dailyPuzzle.id

                Card(
                    onClick = { onSelectPuzzle(puzzle, isDaily) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(puzzle.titleAr, style = MaterialTheme.typography.titleSmall)
                                if (isDaily) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "يومي",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = puzzle.category.labelAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${puzzle.rating}",
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isSolved) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "محلول",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
