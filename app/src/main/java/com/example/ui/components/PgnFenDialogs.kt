package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.chess.BoardTheme
import com.example.chess.ChessBoardState
import com.example.chess.ChessPiece
import com.example.chess.ParsedGameData
import com.example.chess.PgnFenCodec
import com.example.chess.PieceColor
import com.example.chess.PieceStyle
import com.example.chess.PieceType
import com.example.chess.Square
import com.example.ui.theme.DangerRed
import com.example.ui.theme.MonoFontFamily

@Composable
fun ImportPgnFenDialog(
    onImported: (ParsedGameData) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            }.onSuccess { content ->
                textInput = content
                errorMsg = null
            }.onFailure {
                errorMsg = "تعذرت قراءة الملف المختار"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("استيراد PGN أو FEN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ألصق نص PGN لمباراة كاملة أو نص FEN لوضعية محددة، أو اختر ملفاً من الجهاز.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { fileLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("فتح ملف PGN / FEN من الجهاز")
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = {
                        textInput = it
                        errorMsg = null
                    },
                    label = { Text("PGN / FEN") },
                    placeholder = { Text("1. e4 e5 2. Nf3 Nc6 ...") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_pgn_fen_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            textInput = "1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6 5. O-O Be7"
                            errorMsg = null
                        }
                    ) {
                        Text("مثال PGN")
                    }
                    TextButton(
                        onClick = {
                            textInput = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
                            errorMsg = null
                        }
                    ) {
                        Text("مثال FEN")
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
        confirmButton = {
            Button(
                onClick = {
                    val res = PgnFenCodec.importAutoDetect(textInput)
                    res.onSuccess { parsed ->
                        onImported(parsed)
                        onDismiss()
                    }.onFailure { err ->
                        errorMsg = err.message ?: "صيغة غير صالحة"
                    }
                },
                modifier = Modifier.testTag("confirm_import_pgn_btn")
            ) {
                Text("استيراد وفتح")
            }
        }
    )
}

@Composable
fun FenBoardSetupSheetContent(
    boardTheme: BoardTheme,
    pieceStyle: PieceStyle,
    onStartFromFen: (fen: String, playerColor: PieceColor) -> Unit,
    onDismiss: () -> Unit
) {
    var boardArray by remember {
        mutableStateOf(ChessBoardState.initial().board.copyOf())
    }
    var sideToMove by remember { mutableStateOf(PieceColor.WHITE) }
    var playerColor by remember { mutableStateOf(PieceColor.WHITE) }
    // null = erase piece
    var activeBrush by remember { mutableStateOf<ChessPiece?>(ChessPiece(PieceType.QUEEN, PieceColor.WHITE)) }
    var fenText by remember { mutableStateOf(ChessBoardState.INITIAL_FEN) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun rebuildFenFromArray(arr: Array<ChessPiece?>, turn: PieceColor): String {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "اللعب من وضعية FEN مخصصة",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Quick Presets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        boardArray = ChessBoardState.initial().board.copyOf()
                        sideToMove = PieceColor.WHITE
                        fenText = ChessBoardState.INITIAL_FEN
                        errorMsg = null
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("بداية قياسية", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = {
                        val endFen = "8/8/4k3/8/8/4K3/4Q3/8 w - - 0 1"
                        boardArray = ChessBoardState.fromFen(endFen).board.copyOf()
                        sideToMove = PieceColor.WHITE
                        fenText = endFen
                        errorMsg = null
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("نهاية وزير", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = {
                        val emptyArr = arrayOfNulls<ChessPiece>(64)
                        emptyArr[4] = ChessPiece(PieceType.KING, PieceColor.BLACK)
                        emptyArr[60] = ChessPiece(PieceType.KING, PieceColor.WHITE)
                        boardArray = emptyArr
                        fenText = rebuildFenFromArray(emptyArr, sideToMove)
                        errorMsg = null
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("ملكان فقط", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Interactive Setup Grid
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(
                        modifier = Modifier
                            .size(232.dp)
                            .border(2.dp, boardTheme.boardBorder, RoundedCornerShape(8.dp))
                    ) {
                        for (r in 0..7) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                for (c in 0..7) {
                                    val idx = r * 8 + c
                                    val isLight = (r + c) % 2 == 0
                                    val piece = boardArray[idx]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(29.dp)
                                            .background(if (isLight) boardTheme.lightSquare else boardTheme.darkSquare)
                                            .clickable {
                                                val copy = boardArray.copyOf()
                                                copy[idx] = if (copy[idx] == activeBrush) null else activeBrush
                                                boardArray = copy
                                                fenText = rebuildFenFromArray(copy, sideToMove)
                                                errorMsg = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (piece != null) {
                                            ChessPieceGraphic(piece = piece, pieceSize = 23.dp, pieceStyle = pieceStyle)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Piece Palette Brush Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("اختر قطعة لوضعها على الرقعة (أو الممحاة):", style = MaterialTheme.typography.labelLarge)
                listOf(PieceColor.WHITE, PieceColor.BLACK).forEach { color ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PieceType.entries.forEach { type ->
                            val candidate = ChessPiece(type, color)
                            val selected = activeBrush == candidate
                            Surface(
                                onClick = { activeBrush = candidate },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ChessPieceGraphic(piece = candidate, pieceSize = 24.dp, pieceStyle = pieceStyle)
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { activeBrush = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Backspace, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (activeBrush == null) "وضع الممحاة مُفعّل (انقر لإزالة قطعة)" else "ممحاة (مسح مربع)")
                }
            }
        }

        // Side to move & Player side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الدور:", style = MaterialTheme.typography.labelLarge)
                FilterChip(
                    selected = sideToMove == PieceColor.WHITE,
                    onClick = {
                        sideToMove = PieceColor.WHITE
                        fenText = rebuildFenFromArray(boardArray, PieceColor.WHITE)
                    },
                    label = { Text("الأبيض") }
                )
                FilterChip(
                    selected = sideToMove == PieceColor.BLACK,
                    onClick = {
                        sideToMove = PieceColor.BLACK
                        fenText = rebuildFenFromArray(boardArray, PieceColor.BLACK)
                    },
                    label = { Text("الأسود") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تلعب بـ:", style = MaterialTheme.typography.labelLarge)
                FilterChip(
                    selected = playerColor == PieceColor.WHITE,
                    onClick = { playerColor = PieceColor.WHITE },
                    label = { Text("الأبيض") }
                )
                FilterChip(
                    selected = playerColor == PieceColor.BLACK,
                    onClick = { playerColor = PieceColor.BLACK },
                    label = { Text("الأسود") }
                )
            }
        }

        // Direct FEN Edit Field
        item {
            OutlinedTextField(
                value = fenText,
                onValueChange = { txt ->
                    fenText = txt
                    errorMsg = null
                    PgnFenCodec.parseFenStrict(txt).onSuccess { parsed ->
                        boardArray = parsed.board.copyOf()
                        sideToMove = parsed.turn
                    }
                },
                label = { Text("FEN") },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                modifier = Modifier.fillMaxWidth()
            )
            if (errorMsg != null) {
                Text(errorMsg!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("إلغاء")
                }
                Button(
                    onClick = {
                        PgnFenCodec.parseFenStrict(fenText).onSuccess {
                            onStartFromFen(fenText.trim(), playerColor)
                            onDismiss()
                        }.onFailure { err ->
                            errorMsg = err.message ?: "وضعية FEN غير صالحة"
                        }
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("start_from_fen_btn")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("ابدأ اللعب من FEN")
                }
            }
        }
    }
}
