package com.lextezy.tambiolo.ui.results

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lextezy.tambiolo.data.Draw
import com.lextezy.tambiolo.data.GAMES
import com.lextezy.tambiolo.data.formatDrawDate
import com.lextezy.tambiolo.data.formatPeso
import com.lextezy.tambiolo.data.formatUpdated
import com.lextezy.tambiolo.data.gameFor
import com.lextezy.tambiolo.ui.components.BallRow
import com.lextezy.tambiolo.ui.components.CodeChip
import com.lextezy.tambiolo.ui.components.DrumLogo
import com.lextezy.tambiolo.ui.components.FinePrint
import com.lextezy.tambiolo.ui.i18n.S
import com.lextezy.tambiolo.ui.theme.Tambiolo

@Composable
fun ResultsScreen(state: ResultsUiState, onRefresh: () -> Unit, onOpenSettings: () -> Unit) {
    var openGame by rememberSaveable { mutableStateOf<Int?>(null) }
    val current = openGame
    if (current != null) {
        BackHandler { openGame = null }
        GameHistory(max = current, draws = state.history(current), onBack = { openGame = null })
    } else {
        LatestResults(state, onRefresh, onOpenSettings, onOpenGame = { openGame = it })
    }
}

@Composable
private fun LatestResults(
    state: ResultsUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGame: (Int) -> Unit,
) {
    val c = Tambiolo.colors
    val s = S.str
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DrumLogo()
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tambiolo", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                    Text(s.lottoResults, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.muted, letterSpacing = 0.8.sp)
                }
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = c.ink)
                    Spacer(Modifier.width(12.dp))
                } else {
                    IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = s.refresh, tint = c.ink) }
                }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = s.settings, tint = c.ink) }
            }
        }
        item {
            val updated = formatUpdated(state.updated)
            val status = when {
                state.offline && state.draws.isNotEmpty() -> s.offlineSaved
                state.offline -> s.cantLoad
                updated != null -> s.updated(updated)
                else -> s.loadingResults
            }
            Text(status, fontSize = 13.sp, color = if (state.offline) c.red else c.muted, modifier = Modifier.padding(bottom = 2.dp))
        }
        items(GAMES, key = { it.max }) { game ->
            val draw = state.latest(game.max)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.card, RoundedCornerShape(18.dp))
                    .clickable(enabled = draw != null) { onOpenGame(game.max) }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CodeChip(game.code)
                    Spacer(Modifier.width(8.dp))
                    Text(game.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.ink, modifier = Modifier.weight(1f))
                    if (draw != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.muted)
                }
                if (draw == null) {
                    Text(s.noResultsYet, fontSize = 14.sp, color = c.muted)
                } else {
                    BallRow(draw.numbers)
                    Row(Modifier.fillMaxWidth()) {
                        Text(s.jackpot, fontSize = 13.sp, color = c.muted)
                        Text(formatPeso(draw.jackpot), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.weight(1f))
                        Text(formatDrawDate(draw.localDate), fontSize = 13.sp, color = c.muted)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.size(4.dp))
            FinePrint(s.resultsDisclaimer)
        }
    }
}

@Composable
private fun GameHistory(max: Int, draws: List<Draw>, onBack: () -> Unit) {
    val c = Tambiolo.colors
    val game = gameFor(max)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = S.str.back, tint = c.ink) }
                Text(game.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
                Spacer(Modifier.width(8.dp))
                CodeChip(game.code)
            }
        }
        items(draws, key = { it.date }) { d ->
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .fillMaxWidth()
                    .background(c.card, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(formatDrawDate(d.localDate), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = c.ink, modifier = Modifier.weight(1f))
                    Text(formatPeso(d.jackpot), fontSize = 13.sp, color = c.muted)
                }
                BallRow(d.numbers, maxBall = 40.dp)
            }
        }
    }
}
