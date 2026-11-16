package com.lextezy.tambiolo.data

import com.lextezy.tambiolo.ui.i18n.Strings
import java.time.LocalDate

/** Game codes that get result alerts unless the user changes them. */
val DEFAULT_ALERT_GAMES: Set<String> = GAMES.map { it.code }.toSet()

/** Only draws this recent can trigger an alert, so a history backfill never floods the phone. */
const val ALERT_WINDOW_DAYS = 4L

class NewResults(val lotto: List<Draw>, val digits: List<DigitDraw>) {
    val count: Int get() = lotto.size + digits.size
    val isEmpty: Boolean get() = count == 0
}

/** Keys of all recent draws; saved so the same result is never announced twice. */
fun recentKeys(lotto: List<Draw>, digits: List<DigitDraw>, today: LocalDate, days: Long = 14): Set<String> {
    val from = today.minusDays(days)
    val keys = HashSet<String>()
    lotto.forEach { if (!it.localDate.isBefore(from)) keys += it.key }
    digits.forEach { if (!it.localDate.isBefore(from)) keys += it.key }
    return keys
}

/** Recent draws the phone hasn't seen yet, for the games the user wants alerts for. */
fun findNewResults(
    lotto: List<Draw>,
    digits: List<DigitDraw>,
    seen: Set<String>,
    games: Set<String>,
    today: LocalDate,
): NewResults {
    val from = today.minusDays(ALERT_WINDOW_DAYS)
    val newLotto = lotto
        .filter { it.game in games && !it.localDate.isBefore(from) && it.key !in seen }
        .sortedWith(compareByDescending<Draw> { it.date }.thenByDescending { it.max })
    val newDigits = digits
        .filter { it.game in games && !it.localDate.isBefore(from) && it.key !in seen }
        .sortedWith(compareByDescending<DigitDraw> { it.date }.thenByDescending { it.hour }.thenBy { it.game })
    return NewResults(newLotto, newDigits)
}

class AlertText(val title: String, val lines: List<String>)

/** Builds the notification text: one line per new draw, plus any saved combo that matched 3 or more. */
fun alertText(new: NewResults, combos: List<Combo>, s: Strings): AlertText {
    val lines = ArrayList<String>()
    for (d in new.lotto) {
        val numbers = d.numbers.sorted().joinToString(" ") { it.toString().padStart(2, '0') }
        lines += "${gameFor(d.max).name} ${d.game}: $numbers"
        combos.filter { it.max == d.max }
            .map { c -> c to d.numbers.count { it in c.numbers } }
            .filter { it.second >= 3 }
            .sortedByDescending { it.second }
            .forEach { (c, m) -> lines += s.alertComboMatched(c.name, m) }
    }
    for (d in new.digits) {
        lines += "${digitGameFor(d.game).name} ${formatHour(d.hour)}: ${formatDigits(d)}"
    }
    val title = when {
        new.count == 1 && new.lotto.size == 1 -> s.alertTitleOne("${gameFor(new.lotto[0].max).name} ${new.lotto[0].game}")
        new.count == 1 -> s.alertTitleOne("${digitGameFor(new.digits[0].game).name} ${formatHour(new.digits[0].hour)}")
        else -> s.alertTitle
    }
    return AlertText(title, lines)
}
