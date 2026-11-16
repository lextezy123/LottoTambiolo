package com.lextezy.tambiolo.ui.results

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lextezy.tambiolo.data.DIGIT_GAMES
import com.lextezy.tambiolo.data.DRAW_DAYS
import com.lextezy.tambiolo.data.DigitDraw
import com.lextezy.tambiolo.data.DigitGame
import com.lextezy.tambiolo.data.digitGameFor
import com.lextezy.tambiolo.data.formatHour
import com.lextezy.tambiolo.data.nextDigitDraw
import com.lextezy.tambiolo.data.Draw
import com.lextezy.tambiolo.data.manilaNow
import com.lextezy.tambiolo.data.nextDrawDate
import com.lextezy.tambiolo.data.GAMES
import com.lextezy.tambiolo.data.formatDrawDate
import com.lextezy.tambiolo.data.formatPeso
import com.lextezy.tambiolo.data.formatUpdated
import com.lextezy.tambiolo.data.gameFor
import com.lextezy.tambiolo.ui.components.BallRow
import com.lextezy.tambiolo.ui.components.CodeChip
import com.lextezy.tambiolo.ui.components.DigitChips
import com.lextezy.tambiolo.ui.components.DrumLogo
import com.lextezy.tambiolo.ui.components.FinePrint
import com.lextezy.tambiolo.ui.i18n.S
import com.lextezy.tambiolo.ui.theme.Tambiolo

@Composable
fun ResultsScreen(
    state: ResultsUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    showAlertsCard: Boolean = false,
    onEnableAlerts: () -> Unit = {},
    onDismissAlerts: () -> Unit = {},
) {
    var openGame by rememberSaveable { mutableStateOf<Int?>(null) }
    var openDigit by rememberSaveable { mutableStateOf<String?>(null) }
    // 0 = the 6-number lotto games, 1 = Swertres and the other digit games
    var section by rememberSaveable { mutableIntStateOf(0) }
    val current = openGame
    val currentDigit = openDigit
    if (current != null) {
        BackHandler { openGame = null }
        GameHistory(max = current, draws = state.history(current), onBack = { openGame = null })
    } else if (currentDigit != null) {
        BackHandler { openDigit = null }
        DigitHistory(game = digitGameFor(currentDigit), draws = state.digitHistory(currentDigit), onBack = { openDigit = null })
    } else {
        LatestResults(
            state, onRefresh, onOpenSettings,
            onOpenGame = { openGame = it },
            section = section,
            onSection = { section = it },
            onOpenDigit = { openDigit = it },
            showAlertsCard = showAlertsCard,
            onEnableAlerts = onEnableAlerts,
            onDismissAlerts = onDismissAlerts,
        )
    }
}

@Composable
private fun LatestResults(
    state: ResultsUiState,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGame: (Int) -> Unit,
    section: Int,
    onSection: (Int) -> Unit,
    onOpenDigit: (String) -> Unit,
    showAlertsCard: Boolean,
    onEnableAlerts: () -> Unit,
    onDismissAlerts: () -> Unit,
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
        item { SectionTabs(section, onSection) }
        if (showAlertsCard) {
            item { AlertsCard(onEnableAlerts, onDismissAlerts) }
        }
        if (section == 0) {
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
                    BallRow(draw.numbers.sorted())
                    Row(Modifier.fillMaxWidth()) {
                        Text(s.jackpot, fontSize = 13.sp, color = c.muted)
                        Text(formatPeso(draw.jackpot), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.weight(1f))
                        Text(formatDrawDate(draw.localDate), fontSize = 13.sp, color = c.muted)
                    }
                }
                HorizontalDivider(color = c.line)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, tint = c.muted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.nextDraw(nextDrawLabel(game.max)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, modifier = Modifier.weight(1f))
                    Text(drawDaysLabel(game.max), fontSize = 13.sp, color = c.muted)
                }
            }
        }
        item { ScheduleCard() }
        } else {
            items(DIGIT_GAMES, key = { it.code }) { game ->
                DigitGameCard(game, state.digitHistory(game.code), onOpen = { onOpenDigit(game.code) })
            }
            item { DigitScheduleCard() }
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
                BallRow(d.numbers.sorted(), maxBall = 40.dp)
            }
        }
    }
}

/** "Tonight, 9 PM", "Tomorrow, 9 PM" or "Fri, 9 PM" in the chosen language. */
@Composable
fun nextDrawLabel(max: Int): String {
    val s = S.str
    val now = manilaNow()
    val next = nextDrawDate(max, now)
    val day = when (next) {
        now.toLocalDate() -> return s.tonight9pm
        now.toLocalDate().plusDays(1) -> s.tomorrow
        else -> s.dayShort[next.dayOfWeek.value - 1]
    }
    return "$day, 9 PM"
}

@Composable
fun drawDaysLabel(max: Int): String {
    val names = S.str.dayShort
    return DRAW_DAYS[max].orEmpty().joinToString(" · ") { names[it.value - 1] }
}

@Composable
private fun ScheduleCard() {
    val c = Tambiolo.colors
    val s = S.str
    Column(
        Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.drawSchedule, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.ink)
        }
        GAMES.forEach { game ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CodeChip(game.code)
                Spacer(Modifier.width(8.dp))
                Text(drawDaysLabel(game.max), fontSize = 14.sp, color = c.ink, modifier = Modifier.weight(1f))
            }
        }
        Text(s.scheduleNote, fontSize = 12.sp, color = c.muted, lineHeight = 17.sp)
    }
}

@Composable
private fun SectionTabs(section: Int, onSection: (Int) -> Unit) {
    val c = Tambiolo.colors
    val s = S.str
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(s.sectionLotto, s.sectionDigits).forEachIndexed { i, label ->
            val on = i == section
            Box(
                Modifier.weight(1f).height(44.dp)
                    .background(if (on) c.selectedBg else c.card, RoundedCornerShape(22.dp))
                    .border(1.5.dp, if (on) c.selectedBg else c.line, RoundedCornerShape(22.dp))
                    .clickable { onSection(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.selectedInk else c.ink, maxLines = 1)
            }
        }
    }
}

/** One-time offer to turn on result alerts. */
@Composable
private fun AlertsCard(onTurnOn: () -> Unit, onNotNow: () -> Unit) {
    val c = Tambiolo.colors
    val s = S.str
    Column(
        Modifier.fillMaxWidth().background(c.navPill, RoundedCornerShape(18.dp)).padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = c.navPillInk, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.alertsCardTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.navPillInk)
        }
        Text(s.alertsCardBody, fontSize = 13.sp, color = c.navPillInk, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp, end = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onNotNow) { Text(s.notNow, color = c.navPillInk) }
            TextButton(onClick = onTurnOn) { Text(s.turnOn, color = c.navPillInk, fontWeight = FontWeight.Bold) }
        }
    }
}

/** "Today, 5 PM", "Tomorrow, 2 PM" or "Fri, 9 PM" for a digit game's next draw. */
@Composable
private fun digitNextLabel(game: DigitGame): String {
    val s = S.str
    val now = manilaNow()
    val next = nextDigitDraw(game, now)
    val time = formatHour(next.hour)
    return when (next.toLocalDate()) {
        now.toLocalDate() -> s.laterToday(time, next.hour >= 18)
        now.toLocalDate().plusDays(1) -> "${s.tomorrow}, $time"
        else -> "${s.dayShort[next.dayOfWeek.value - 1]}, $time"
    }
}

@Composable
private fun digitDaysLabel(game: DigitGame): String {
    val names = S.str.dayShort
    val days = game.days ?: return game.hours.joinToString(" · ") { formatHour(it) }
    return days.joinToString(" · ") { names[it.value - 1] }
}

/** One draw: its time (for games drawn several times a day) and its digits. */
@Composable
private fun DigitDrawRow(game: DigitGame, draw: DigitDraw) {
    val c = Tambiolo.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (game.hours.size > 1) {
            Text(formatHour(draw.hour), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.muted, modifier = Modifier.width(52.dp))
        }
        DigitChips(draw.numbers, twoDigit = game.code == "2D")
    }
}

/** Latest day of results for one digit game. [draws] is that game's history, newest first. */
@Composable
private fun DigitGameCard(game: DigitGame, draws: List<DigitDraw>, onOpen: () -> Unit) {
    val c = Tambiolo.colors
    val s = S.str
    val latestDate = draws.firstOrNull()?.date
    val latest = draws.filter { it.date == latestDate }.sortedBy { it.hour }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.card, RoundedCornerShape(18.dp))
            .clickable(enabled = draws.isNotEmpty(), onClick = onOpen)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CodeChip(game.code)
            Spacer(Modifier.width(8.dp))
            Text(game.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.ink, modifier = Modifier.weight(1f))
            if (latest.isNotEmpty()) {
                Text(formatDrawDate(latest.first().localDate), fontSize = 13.sp, color = c.muted)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.muted)
            }
        }
        if (latest.isEmpty()) {
            Text(s.noResultsYet, fontSize = 14.sp, color = c.muted)
        } else {
            latest.forEach { d -> DigitDrawRow(game, d) }
            val prize = latest.last().prize
            if (game.hours.size == 1 && prize != null) {
                Row {
                    Text(s.prize, fontSize = 13.sp, color = c.muted)
                    Text(formatPeso(prize), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.ink)
                }
            }
        }
        HorizontalDivider(color = c.line)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = c.muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.nextDraw(digitNextLabel(game)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink, modifier = Modifier.weight(1f))
            Text(digitDaysLabel(game), fontSize = 13.sp, color = c.muted)
        }
    }
}

@Composable
private fun DigitHistory(game: DigitGame, draws: List<DigitDraw>, onBack: () -> Unit) {
    val c = Tambiolo.colors
    // One card per day, newest day first; within a day, 2 PM before 9 PM.
    val days = draws.groupBy { it.date }.map { (date, list) -> date to list.sortedBy { it.hour } }
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
        items(days, key = { it.first }) { (_, list) ->
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .fillMaxWidth()
                    .background(c.card, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(formatDrawDate(list.first().localDate), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = c.ink)
                list.forEach { d -> DigitDrawRow(game, d) }
            }
        }
    }
}

@Composable
private fun DigitScheduleCard() {
    val c = Tambiolo.colors
    val s = S.str
    Column(
        Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = c.ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.drawSchedule, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.ink)
        }
        DIGIT_GAMES.forEach { game ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CodeChip(game.code)
                Spacer(Modifier.width(8.dp))
                val days = game.days
                Text(
                    if (days == null) s.digitScheduleDaily
                    else days.joinToString(" · ") { s.dayShort[it.value - 1] } + ", 9 PM",
                    fontSize = 14.sp, color = c.ink, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
