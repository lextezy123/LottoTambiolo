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

fun gameFor(max: Int): Game = GAMES.first { it.max == max }

@Serializable
data class Draw(
    val game: String,          // "6/55"
    val date: String,          // "2026-10-28"
    val numbers: List<Int>,
    val jackpot: Double? = null,
) {
    val max: Int get() = game.substringAfter("/").toInt()
    val localDate: LocalDate get() = LocalDate.parse(date)
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
