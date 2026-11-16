package com.lextezy.tambiolo.data

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One of the five 6-number games. [max] is the highest ball number. */
data class Game(val max: Int, val name: String) {
    val code: String get() = "6/$max"
}

/** Ordered as they appear on the Results screen: biggest game first. */
val GAMES = listOf(
    Game(58, "Ultra Lotto"),
    Game(55, "Grand Lotto"),
    Game(49, "Super Lotto"),
    Game(45, "Mega Lotto"),
    Game(42, "Lotto"),
)

fun gameFor(max: Int): Game = GAMES.firstOrNull { it.max == max } ?: Game(max, "Lotto")

@Serializable
data class Draw(
    val game: String,          // "6/55"
    val date: String,          // "2026-10-28"
    val numbers: List<Int>,
    val jackpot: Double? = null,
) {
    /** Highest ball number, or 0 if the game code is not a 6/NN game. */
    val max: Int get() = game.substringAfter("/").toIntOrNull() ?: 0
    val localDate: LocalDate get() = LocalDate.parse(date)
    val key: String get() = "$game|$date"
}

@Serializable
data class ResultsFile(
    val updated: String = "",
    val draws: List<Draw> = emptyList(),
)

@Serializable
data class Combo(
    val id: Long,
    val name: String,
    val max: Int,
    val numbers: List<Int>,
)

private val shortDate = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
private val updatedFormat = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH)
private val manila = ZoneId.of("Asia/Manila")

fun formatDrawDate(date: LocalDate): String = date.format(shortDate)

fun formatUpdated(iso: String): String? =
    runCatching { Instant.parse(iso).atZone(manila).format(updatedFormat) }.getOrNull()

fun formatPeso(amount: Double?): String = when {
    amount == null -> "—"
    amount >= 1_000_000 -> String.format(Locale.US, "₱%.1fM", amount / 1_000_000)
    else -> String.format(Locale.US, "₱%,.0f", amount)
}

/** Number of possible 6-number tickets for a game, e.g. 28,989,675 for 6/58. */
fun combinations(max: Int, pick: Int = 6): Long {
    var r = 1L
    for (i in 1..pick) r = r * (max - pick + i) / i
    return r
}

// ---- Draw schedule ----
// All five games draw at 9 PM, three nights a week (checked against a year of results).
val DRAW_DAYS: Map<Int, List<java.time.DayOfWeek>> = mapOf(
    58 to listOf(java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SUNDAY),
    55 to listOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.SATURDAY),
    49 to listOf(java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.SUNDAY),
    45 to listOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY),
    42 to listOf(java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.SATURDAY),
)

const val DRAW_HOUR = 21

fun manilaNow(): java.time.LocalDateTime = java.time.LocalDateTime.now(ZoneId.of("Asia/Manila"))

/** Date of the next draw for a game, counting today only if 9 PM hasn't passed yet. */
fun nextDrawDate(max: Int, now: java.time.LocalDateTime = manilaNow()): LocalDate {
    val days = DRAW_DAYS[max] ?: return now.toLocalDate()
    var d = now.toLocalDate()
    if (now.hour >= DRAW_HOUR) d = d.plusDays(1)
    while (d.dayOfWeek !in days) d = d.plusDays(1)
    return d
}

// ---- Number statistics ----
enum class StatsPeriod(val days: Long?) { WEEK(7), MONTH(30), YEAR(365), ALL(null) }

/**
 * How often each number came up in a set of draws.
 * [lastSeen] is the epoch day a number last appeared (0 = never), used to break ties:
 * between two numbers drawn equally often, the one drawn more recently ranks higher.
 */
class NumberStats(val max: Int, val drawCount: Int, val counts: IntArray, val lastSeen: LongArray) {
    val ranked: List<Int> = (1..max).sortedWith(compareBy({ -counts[it] }, { -lastSeen[it] }, { it }))
    /** The six most drawn numbers ("hot"). */
    val mostDrawn: List<Int> get() = ranked.take(6)
    /** The six least drawn numbers ("cold"), coldest first. */
    val leastDrawn: List<Int> get() = ranked.takeLast(6).reversed()
    val topCount: Int get() = counts.maxOrNull() ?: 0
}

fun numberStats(draws: List<Draw>, max: Int, period: StatsPeriod, today: LocalDate = manilaNow().toLocalDate()): NumberStats {
    val from = period.days?.let { today.minusDays(it - 1) }
    val inRange = draws.filter { it.max == max && (from == null || !it.localDate.isBefore(from)) }
    val counts = IntArray(max + 1)
    val lastSeen = LongArray(max + 1)
    inRange.forEach { d ->
        val day = d.localDate.toEpochDay()
        d.numbers.forEach { n ->
            if (n in 1..max) {
                counts[n]++
                if (day > lastSeen[n]) lastSeen[n] = day
            }
        }
    }
    return NumberStats(max, inRange.size, counts, lastSeen)
}

/** A combo's best result in past draws of its game: how many numbers matched, and when. */
class PastMatch(val matched: Int, val date: LocalDate)

fun bestPastMatch(combo: Combo, draws: List<Draw>): PastMatch? {
    val picks = combo.numbers.toSet()
    var best: PastMatch? = null
    for (d in draws) {
        if (d.max != combo.max) continue
        val m = d.numbers.count { it in picks }
        val b = best
        if (b == null || m > b.matched || (m == b.matched && d.localDate.isAfter(b.date))) best = PastMatch(m, d.localDate)
    }
    return best
}
