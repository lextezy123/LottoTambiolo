package com.lextezy.tambiolo

import com.lextezy.tambiolo.data.Combo
import com.lextezy.tambiolo.data.DIGIT_GAMES
import com.lextezy.tambiolo.data.DigitDraw
import com.lextezy.tambiolo.data.Draw
import com.lextezy.tambiolo.data.StatsPeriod
import com.lextezy.tambiolo.data.alertText
import com.lextezy.tambiolo.data.bestPastMatch
import com.lextezy.tambiolo.data.combinations
import com.lextezy.tambiolo.data.digitGameFor
import com.lextezy.tambiolo.data.findNewResults
import com.lextezy.tambiolo.data.formatDigits
import com.lextezy.tambiolo.data.formatHour
import com.lextezy.tambiolo.data.gameFor
import com.lextezy.tambiolo.data.nextDigitDraw
import com.lextezy.tambiolo.data.nextDrawDate
import com.lextezy.tambiolo.data.numberStats
import com.lextezy.tambiolo.data.parseHour
import com.lextezy.tambiolo.data.recentKeys
import com.lextezy.tambiolo.ui.i18n.EnStrings
import com.lextezy.tambiolo.ui.i18n.TlStrings
import com.lextezy.tambiolo.ui.simulator.DrumPhysics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.hypot

class LogicTest {

    // ---- odds ----

    @Test
    fun combinationsMatchTheRealOdds() {
        assertEquals(5_245_786L, combinations(42))
        assertEquals(8_145_060L, combinations(45))
        assertEquals(13_983_816L, combinations(49))
        assertEquals(28_989_675L, combinations(55))
        assertEquals(40_475_358L, combinations(58))
    }

    // ---- lotto schedule ----

    @Test
    fun nextDrawSkipsToTheNextDrawDay() {
        // Mon Nov 16, 2026. Ultra Lotto 6/58 draws Tue, Fri, Sun.
        assertEquals(LocalDate.of(2026, 11, 17), nextDrawDate(58, LocalDateTime.of(2026, 11, 16, 10, 0)))
        // Grand Lotto 6/55 draws Mon, Wed, Sat: before 9 PM on a draw day means tonight.
        assertEquals(LocalDate.of(2026, 11, 16), nextDrawDate(55, LocalDateTime.of(2026, 11, 16, 20, 59)))
        // From 9 PM on, tonight's draw is over.
        assertEquals(LocalDate.of(2026, 11, 18), nextDrawDate(55, LocalDateTime.of(2026, 11, 16, 21, 0)))
    }

    @Test
    fun unknownGamesNeverCrash() {
        assertEquals("Lotto", gameFor(99).name)
        assertEquals(0, Draw("3D", "2026-11-14", listOf(1, 2, 3)).max)
        assertEquals("XX", digitGameFor("XX").name)
    }

    // ---- stats ----

    private fun draw(game: String, date: String, vararg n: Int) = Draw(game, date, n.toList())

    @Test
    fun statsCountOnlyTheChosenGameAndPeriod() {
        val today = LocalDate.of(2026, 11, 16)
        val draws = listOf(
            draw("6/42", "2026-11-14", 1, 2, 3, 4, 5, 6),
            draw("6/42", "2026-11-12", 1, 2, 3, 10, 11, 12),
            draw("6/42", "2026-09-01", 1, 40, 41, 42, 38, 39),   // outside the month
            draw("6/55", "2026-11-14", 1, 50, 51, 52, 53, 54),   // another game
        )
        val month = numberStats(draws, 42, StatsPeriod.MONTH, today)
        assertEquals(2, month.drawCount)
        assertEquals(2, month.counts[1])
        assertEquals(0, month.counts[40])
        // 1, 2, 3 were drawn twice; then the most recent draw's numbers come first.
        assertEquals(listOf(1, 2, 3, 4, 5, 6), month.mostDrawn)

        val all = numberStats(draws, 42, StatsPeriod.ALL, today)
        assertEquals(3, all.drawCount)
        assertEquals(3, all.counts[1])

        val week = numberStats(draws, 42, StatsPeriod.WEEK, today)
        assertEquals(2, week.drawCount)
        assertEquals(6, week.leastDrawn.size)
    }

    @Test
    fun bestPastMatchFindsTheBestAndLatestDraw() {
        val combo = Combo(1, "Birthdays", 42, listOf(1, 2, 3, 4, 5, 6))
        val draws = listOf(
            draw("6/42", "2026-11-14", 1, 2, 3, 20, 21, 22),
            draw("6/42", "2026-10-01", 1, 2, 3, 4, 30, 31),
            draw("6/42", "2026-09-01", 1, 2, 3, 4, 32, 33),
            draw("6/55", "2026-11-14", 1, 2, 3, 4, 5, 6),
        )
        val best = bestPastMatch(combo, draws)!!
        assertEquals(4, best.matched)
        assertEquals(LocalDate.of(2026, 10, 1), best.date)
        assertEquals(null, bestPastMatch(Combo(2, "x", 49, listOf(1, 2, 3, 4, 5, 6)), draws))
    }

    // ---- digit games ----

    @Test
    fun drawTimesParseAndFormat() {
        assertEquals(14, parseHour("2PM"))
        assertEquals(17, parseHour("5 PM"))
        assertEquals(21, parseHour("9pm"))
        assertEquals(11, parseHour("11AM"))
        assertEquals(12, parseHour("12PM"))
        assertEquals(0, parseHour("?"))
        assertEquals("2 PM", formatHour(14))
        assertEquals("9 PM", formatHour(21))
        assertEquals("11 AM", formatHour(11))
        assertEquals("12 PM", formatHour(12))
    }

    @Test
    fun digitsAreShownInDrawnOrder() {
        assertEquals("2 7 5", formatDigits(DigitDraw("3D", "2026-11-14", "9PM", listOf(2, 7, 5))))
        assertEquals("02-23", formatDigits(DigitDraw("2D", "2026-11-13", "5PM", listOf(2, 23))))
        assertEquals("1 1 8 6 3 9", formatDigits(DigitDraw("6D", "2026-11-14", "9PM", listOf(1, 1, 8, 6, 3, 9))))
    }

    @Test
    fun nextDigitDrawFollowsEachGamesSchedule() {
        val swertres = DIGIT_GAMES.first { it.code == "3D" }
        val fourD = DIGIT_GAMES.first { it.code == "4D" }
        val sixD = DIGIT_GAMES.first { it.code == "6D" }
        // Mon Nov 16, 2026
        assertEquals(LocalDateTime.of(2026, 11, 16, 17, 0), nextDigitDraw(swertres, LocalDateTime.of(2026, 11, 16, 15, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 17, 14, 0), nextDigitDraw(swertres, LocalDateTime.of(2026, 11, 16, 21, 30)))
        // 4D draws Mon, Wed, Fri at 9 PM; 6D draws Tue, Thu, Sat.
        assertEquals(LocalDateTime.of(2026, 11, 16, 21, 0), nextDigitDraw(fourD, LocalDateTime.of(2026, 11, 16, 15, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 18, 21, 0), nextDigitDraw(fourD, LocalDateTime.of(2026, 11, 16, 21, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 17, 21, 0), nextDigitDraw(sixD, LocalDateTime.of(2026, 11, 16, 15, 0)))
    }

    // ---- result alerts ----

    @Test
    fun alertsAnnounceOnlyNewRecentDrawsForChosenGames() {
        val today = LocalDate.of(2026, 11, 16)
        val lotto = listOf(
            draw("6/55", "2026-11-14", 4, 28, 30, 31, 41, 44),
            draw("6/42", "2026-11-14", 4, 18, 22, 28, 31, 38),
            draw("6/58", "2026-11-13", 34, 40, 50, 51, 54, 57),
            draw("6/55", "2026-10-01", 1, 2, 3, 4, 5, 6),          // too old to announce
        )
        val digits = listOf(DigitDraw("3D", "2026-11-14", "9PM", listOf(2, 7, 5)))
        val seen = setOf("6/58|2026-11-13")
        val games = setOf("6/55", "6/42", "6/58")                   // no digit games chosen

        val fresh = findNewResults(lotto, digits, seen, games, today)
        assertEquals(listOf("6/55", "6/42"), fresh.lotto.map { it.game })
        assertTrue(fresh.digits.isEmpty())

        // Once recorded as seen, nothing is announced twice.
        val after = findNewResults(lotto, digits, recentKeys(lotto, digits, today), games + "3D", today)
        assertTrue(after.isEmpty)

        // With Swertres chosen, the digit draw is announced too.
        assertEquals(1, findNewResults(lotto, digits, seen, games + "3D", today).digits.size)
    }

    @Test
    fun alertTextListsNumbersAndMatchingCombos() {
        val today = LocalDate.of(2026, 11, 16)
        val lotto = listOf(draw("6/55", "2026-11-14", 30, 28, 41, 44, 4, 31))
        val digits = listOf(DigitDraw("3D", "2026-11-14", "9PM", listOf(2, 7, 5)))
        val combos = listOf(
            Combo(1, "Birthdays", 55, listOf(4, 28, 30, 1, 2, 3)),   // 3 matches
            Combo(2, "Other", 55, listOf(4, 5, 6, 7, 8, 9)),          // 1 match: not mentioned
        )
        val one = alertText(findNewResults(lotto, emptyList(), emptySet(), setOf("6/55"), today), combos, EnStrings)
        assertEquals("Grand Lotto 6/55 results are out", one.title)
        assertEquals(listOf("Grand Lotto 6/55: 04 28 30 31 41 44", "Your \"Birthdays\" matched 3 of 6"), one.lines)

        val both = alertText(findNewResults(lotto, digits, emptySet(), setOf("6/55", "3D"), today), emptyList(), TlStrings)
        assertEquals("Lumabas na ang resulta ng lotto", both.title)
        assertEquals(listOf("Grand Lotto 6/55: 04 28 30 31 41 44", "Swertres 9 PM: 2 7 5"), both.lines)
    }

    // ---- drum physics ----

    private fun allInsideDrum(p: DrumPhysics): Boolean = p.balls.all {
        hypot(it.x - DrumPhysics.CX, it.y - DrumPhysics.CY) <= DrumPhysics.RADIUS - it.r + 0.5f
    }

    @Test
    fun ballsStayInsideTheDrumWhileMixing() {
        val p = DrumPhysics()
        p.reset(58)
        p.mixing = 1f
        repeat(900) { p.step() }
        assertEquals(58, p.balls.size)
        assertTrue(allInsideDrum(p))
    }

    @Test
    fun drumSpeedDoesNotDependOnScreenRefreshRate() {
        // One second of real time must run the same number of steps at 60 Hz and at 120 Hz.
        fun stepsInOneSecond(fps: Int): Int {
            val p = DrumPhysics()
            p.reset(42)
            p.mixing = 1f
            // The blower lifts balls only when they are low, so count steps through a ball that never moves by itself.
            var steps = 0
            val before = p.balls.map { it.y }
            repeat(fps) {
                val y0 = p.balls.map { it.y }
                p.advance(1f / fps)
                if (p.balls.map { it.y } != y0) steps++
            }
            assertTrue(p.balls.map { it.y } != before)
            return steps
        }
        // At 60 Hz every frame runs one step; at 120 Hz only every second frame does.
        val at60 = stepsInOneSecond(60)
        val at120 = stepsInOneSecond(120)
        assertTrue("60 Hz ran $at60 stepping frames", at60 in 58..60)
        assertTrue("120 Hz ran $at120 stepping frames", at120 in 58..61)
    }

    @Test
    fun drumSleepsWhenSettledAndWakesWhenNeeded() {
        val p = DrumPhysics()
        p.reset(55)
        var frames = 0
        while (!p.asleep && frames < 60 * 30) {
            p.advance(1f / 60f)
            frames++
        }
        assertTrue("drum should settle and sleep", p.asleep)
        assertFalse("nothing is redrawn while asleep", p.advance(1f / 60f))
        assertTrue(allInsideDrum(p))

        p.mixing = 1f
        assertFalse(p.asleep)
        assertTrue(p.advance(1f / 60f))

        // Removing a drawn ball also wakes the drum so the pile can settle again.
        p.mixing = 0f
        while (!p.asleep) p.advance(1f / 60f)
        p.balls.removeAt(0)
        assertTrue(p.advance(1f / 60f))
    }
}
