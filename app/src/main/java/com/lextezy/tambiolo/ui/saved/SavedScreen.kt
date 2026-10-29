package com.lextezy.tambiolo.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lextezy.tambiolo.data.Combo
import com.lextezy.tambiolo.data.Draw
import com.lextezy.tambiolo.data.formatDrawDate
import com.lextezy.tambiolo.data.gameFor
import com.lextezy.tambiolo.ui.components.BallRow
import com.lextezy.tambiolo.ui.components.CodeChip
import com.lextezy.tambiolo.ui.components.DrumIcon
import com.lextezy.tambiolo.ui.components.FinePrint
import com.lextezy.tambiolo.ui.components.GameChips
import com.lextezy.tambiolo.ui.components.NumberGrid
import com.lextezy.tambiolo.ui.i18n.S
import com.lextezy.tambiolo.ui.theme.Tambiolo
import java.security.SecureRandom

@Composable
fun SavedScreen(
    combos: List<Combo>,
    latestDraw: (Int) -> Draw?,
    onSave: (Combo) -> Unit,
    onDelete: (Long) -> Unit,
    onSimulate: (Combo) -> Unit,
) {
    val c = Tambiolo.colors
    val s = S.str
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Combo?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(s.savedTitle, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = c.ink, modifier = Modifier.weight(1f))
                Button(
                    onClick = { adding = true },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.selectedBg, contentColor = c.selectedInk),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.newCombo, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (combos.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(18.dp)).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(s.emptyTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = c.ink)
                    Text(s.emptyBody, fontSize = 14.sp, color = c.muted, lineHeight = 20.sp)
                }
            }
        }
        items(combos, key = { it.id }) { combo ->
            val game = gameFor(combo.max)
            val draw = latestDraw(combo.max)
            val matched = draw?.numbers?.count { it in combo.numbers }
            Column(
                Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(18.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(combo.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = c.ink, modifier = Modifier.weight(1f))
                    CodeChip(game.code)
                }
                BallRow(combo.numbers, maxBall = 44.dp, highlight = draw?.numbers?.toSet() ?: emptySet())
                Text(
                    if (draw == null) s.noResultsLoaded(game.code)
                    else s.latestMatched(game.code, formatDrawDate(draw.localDate), matched ?: 0),
                    fontSize = 13.sp, color = c.muted,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onSimulate(combo) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.navPill, contentColor = c.navPillInk),
                    ) {
                        DrumIcon(c.navPillInk)
                        Spacer(Modifier.width(6.dp))
                        Text(s.simulate, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { deleting = combo }) {
                        Icon(Icons.Filled.Delete, contentDescription = s.deleteLabel(combo.name), tint = c.muted)
                    }
                }
            }
        }
        item {
            FinePrint(s.savedNote)
        }
    }

    if (adding) {
        AddComboSheet(onDismiss = { adding = false }, onSave = { onSave(it); adding = false })
    }
    deleting?.let { combo ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(s.deleteTitle(combo.name)) },
            text = { Text(s.deleteBody) },
            confirmButton = { TextButton(onClick = { onDelete(combo.id); deleting = null }) { Text(s.delete, color = c.red) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(s.cancel, color = c.ink) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddComboSheet(onDismiss: () -> Unit, onSave: (Combo) -> Unit) {
    val c = Tambiolo.colors
    val s = S.str
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var max by remember { mutableIntStateOf(42) }
    var picks by remember { mutableStateOf(listOf<Int>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.bg) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(s.newCombo, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text(s.nameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            GameChips(selectedMax = max) { m -> max = m; picks = picks.filter { it <= m } }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(s.pick6, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.weight(1f))
                Text("${picks.size} / 6", fontSize = 13.sp, color = c.muted)
            }
            NumberGrid(max = max, picks = picks.toSet()) { n ->
                picks = when {
                    n in picks -> picks - n
                    picks.size < 6 -> (picks + n).sorted()
                    else -> picks
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val rnd = SecureRandom()
                        val pool = (1..max).toMutableList()
                        picks = List(6) { pool.removeAt(rnd.nextInt(pool.size)) }.sorted()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(s.quickPick, color = c.ink, fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = {
                        onSave(Combo(System.currentTimeMillis(), name.trim().ifEmpty { s.defaultComboName }, max, picks))
                    },
                    enabled = picks.size == 6,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.selectedBg, contentColor = c.selectedInk),
                ) { Text(s.save, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
