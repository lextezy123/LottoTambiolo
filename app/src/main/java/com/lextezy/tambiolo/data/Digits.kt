package com.lextezy.tambiolo.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Digit games (Swertres, EZ2, 4-Digit, 6-Digit). Unlike the 6-number games,
 * the result is a sequence of digits in drawn order.
 * [hours] are the draw times (24h); [days] is null for games drawn every day.
 */
data class DigitGame(val code: String, val name: String, val hours: List<Int>, val days: List<DayOfWeek>?)

val DIGIT_GAMES = listOf(
    DigitGame("3D", "Swertres", listOf(14, 17, 21), null),
    DigitGame("2D", "EZ2", listOf(14, 17, 21), null),
    DigitGame("4D", "4-Digit", listOf(21), listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
    DigitGame("6D", "6-Digit", listOf(21), listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)),
)

fun digitGameFor(code: String): DigitGame =
    DIGIT_GAMES.firstOrNull { it.code == code } ?: DigitGame(code, code, listOf(21), null)

@Serializable
data class DigitDraw(
    val game: String,          // "3D"
    val date: String,          // "2026-11-14"
    val time: String = "9PM",  // "2PM", "5PM" or "9PM"
    val numbers: List<Int>,
    val prize: Double? = null,
) {
    val localDate: LocalDate get() = LocalDate.parse(date)
    /** 14 for "2PM", 21 for "9PM". Used to put a day's draws in order. */
    val hour: Int get() = parseHour(time)
    val key: String get() = "$game|$date|$time"
}

@Serializable
data class DigitsFile(
    val updated: String = "",
    val draws: List<DigitDraw> = emptyList(),
)

private val hourPattern = Regex("""(\d{1,2})\s*([AaPp])""")

fun parseHour(time: String): Int {
    val m = hourPattern.find(time) ?: return 0
    val h = m.groupValues[1].toInt() % 12
    return if (m.groupValues[2].equals("p", ignoreCase = true)) h + 12 else h
}

/** 14 -> "2 PM", 21 -> "9 PM". */
fun formatHour(hour: Int): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "$h ${if (hour >= 12) "PM" else "AM"}"
}

/** "13-17" for EZ2 (two numbers from 1 to 31), "2 7 5" for the others. */
fun formatDigits(draw: DigitDraw): String =
    if (draw.game == "2D") draw.numbers.joinToString("-") { it.toString().padStart(2, '0') }
    else draw.numbers.joinToString(" ")

/** When the next draw of a digit game happens (Manila time). */
fun nextDigitDraw(game: DigitGame, now: LocalDateTime = manilaNow()): LocalDateTime {
    var day = now.toLocalDate()
    repeat(8) {
        if (game.days == null || day.dayOfWeek in game.days) {
            for (h in game.hours.sorted()) {
                val at = day.atTime(h, 0)
                if (at.isAfter(now)) return at
            }
        }
        day = day.plusDays(1)
    }
    return now
}
