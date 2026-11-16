package com.lextezy.tambiolo.ui.simulator

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Simple 2D physics for the tambiolo drum, in a virtual 600 x 640 space:
 * the drum is a circle (center 300,340 radius 270) with a chute at the top.
 *
 * Plain Kotlin with no Android code, so it can be unit tested.
 */
class Ball(val n: Int, var x: Float, var y: Float, val r: Float) {
    var vx = 0f
    var vy = 0f
    var flying = false
    var scale = 1f
}

class DrumPhysics {
    val balls = mutableListOf<Ball>()

    /** 0 = balls rest at the bottom, 1 = full air jet mixing. */
    var mixing = 0f
        set(value) {
            field = value
            wake()
        }

    /** True once the balls have settled; nothing is simulated or redrawn until something changes. */
    var asleep = false
        private set

    private var carry = 0f
    private var clock = 0f
    private var jetPhase = 0f
    private var swirlPhase = 0f
    private var quietSteps = 0
    private var lastCount = 0

    fun wake() {
        asleep = false
        quietSteps = 0
    }

    fun reset(max: Int) {
        balls.clear()
        val r = (520f / sqrt(max * 3.2f)).coerceIn(13f, 20f)
        for (n in 1..max) {
            val a = Random.nextFloat() * 6.2832f
            val d = Random.nextFloat() * (RADIUS - r - 20f)
            balls += Ball(n, CX + cos(a) * d, CY + sin(a) * d * 0.5f + 80f, r)
        }
        lastCount = balls.size
        carry = 0f
        // Every draw starts its jet and swirl at a different point, so no two mixes look alike.
        jetPhase = Random.nextFloat() * 6.2832f
        swirlPhase = Random.nextFloat() * 6.2832f
        wake()
    }

    /**
     * Moves the simulation forward by real time, in fixed 1/60 s steps.
     * Returns true if anything may have moved (so the view should redraw).
     */
    fun advance(dtSeconds: Float): Boolean {
        if (balls.size != lastCount) {
            lastCount = balls.size
            wake()
        }
        if (balls.any { it.flying }) wake()
        if (asleep) return false
        carry += dtSeconds.coerceIn(0f, 0.1f)
        var steps = 0
        while (carry >= STEP && steps < MAX_STEPS_PER_FRAME) {
            step()
            carry -= STEP
            steps++
        }
        if (steps == MAX_STEPS_PER_FRAME) carry = 0f
        return true
    }

    /** One fixed step of 1/60 s. */
    fun step() {
        clock += STEP
        val jetX = CX + sin(clock * JET_SWEEP_RATE + jetPhase) * JET_SWEEP
        val swirl = SWIRL * sin(clock * SWIRL_RATE + swirlPhase)
        for (b in balls) {
            if (b.flying) continue
            b.vy += 0.35f // gravity
            if (mixing > 0f) {
                // Air blower: lift only in the lower part of the drum, so balls shoot up,
                // arc, and fall back down instead of pinning to the top. The strong jet
                // wanders left and right, so the fountain keeps moving and balls outside
                // it sink back into the pile.
                val depth = (b.y - CY) / RADIUS // -1 = top, 1 = bottom
                if (depth > JET_DEPTH) {
                    val lift = if (abs(b.x - jetX) < JET_HALF_WIDTH) JET_LIFT + Random.nextFloat() * JET_LIFT_EXTRA
                    else SIDE_LIFT + Random.nextFloat() * SIDE_LIFT_EXTRA
                    b.vy -= mixing * lift
                }
                // Random jitter everywhere
                b.vx += (Random.nextFloat() * 2f - 1f) * 1.2f * mixing
                b.vy += (Random.nextFloat() * 2f - 1f) * 0.8f * mixing
                // Gusts: now and then a ball gets knocked in a completely random direction.
                if (Random.nextFloat() < GUST_CHANCE * mixing) {
                    val a = Random.nextFloat() * 6.2832f
                    val k = GUST_MIN + Random.nextFloat() * GUST_EXTRA
                    b.vx += cos(a) * k; b.vy += sin(a) * k
                }
                // Swirl around the center that slowly changes strength and direction.
                val sx = b.x - CX; val sy = b.y - CY
                val sd = hypot(sx, sy).coerceAtLeast(1f)
                b.vx += -sy / sd * swirl * mixing
                b.vy += sx / sd * swirl * mixing
            }
            val speed = hypot(b.vx, b.vy)
            if (speed > MAX_SPEED) { b.vx *= MAX_SPEED / speed; b.vy *= MAX_SPEED / speed }
            b.vx *= 0.99f; b.vy *= 0.99f
            b.x += b.vx; b.y += b.vy
        }
        // Push overlapping balls apart and keep every ball inside the drum. A few passes
        // keep the resting pile from sinking into itself or poking through the rim.
        for (pass in 0 until SOLVER_PASSES) {
            for (i in balls.indices) for (j in i + 1 until balls.size) {
                val a = balls[i]; val b = balls[j]
                if (a.flying || b.flying) continue
                val dx = b.x - a.x; val dy = b.y - a.y
                val d = hypot(dx, dy); val m = a.r + b.r
                if (d > 0f && d < m) {
                    val nx = dx / d; val ny = dy / d; val o = (m - d) / 2f
                    a.x -= nx * o; a.y -= ny * o; b.x += nx * o; b.y += ny * o
                    val rv = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
                    if (rv < 0) {
                        val k = -0.9f * rv
                        a.vx -= k * nx; a.vy -= k * ny; b.vx += k * nx; b.vy += k * ny
                    }
                }
            }
            for (b in balls) {
                if (b.flying) continue
                val dx = b.x - CX; val dy = b.y - CY
                val d = hypot(dx, dy); val lim = RADIUS - b.r
                if (d > lim) {
                    val nx = dx / d; val ny = dy / d
                    b.x = CX + nx * lim; b.y = CY + ny * lim
                    val dot = b.vx * nx + b.vy * ny
                    if (dot > 0) {
                        b.vx -= 1.8f * dot * nx; b.vy -= 1.8f * dot * ny
                        if (mixing > 0f) {
                            // The wall isn't a perfect mirror: each bounce leaves at a slightly random angle.
                            val a = (Random.nextFloat() * 2f - 1f) * BOUNCE_SCATTER * mixing
                            val ca = cos(a); val sa = sin(a)
                            val vx = b.vx * ca - b.vy * sa
                            b.vy = b.vx * sa + b.vy * ca
                            b.vx = vx
                        }
                    }
                }
            }
        }
        // With the blower off, the balls settle within a few seconds; after that, stop simulating.
        if (mixing == 0f) {
            quietSteps++
            if (quietSteps >= SETTLE_STEPS) asleep = true
        } else {
            quietSteps = 0
        }
    }

    companion object {
        const val W = 600f
        const val H = 640f
        const val CX = 300f
        const val CY = 340f
        const val RADIUS = 270f
        const val CHUTE_HALF = 26f
        const val MAX_SPEED = 16f
        /** Where a picked ball flies to (top of the chute). */
        const val EXIT_Y = 24f
        const val STEP = 1f / 60f
        const val MAX_STEPS_PER_FRAME = 4
        const val SOLVER_PASSES = 4

        // Mixing feel. All speeds are in drum units per step.
        const val JET_DEPTH = 0.2f          // the blower reaches balls below this height (0 = center, 1 = bottom)
        const val JET_HALF_WIDTH = 95f      // half the width of the strong jet
        const val JET_SWEEP = 150f          // how far the jet wanders from the center
        const val JET_SWEEP_RATE = 1.7f     // how fast it wanders
        const val JET_LIFT = 1.8f
        const val JET_LIFT_EXTRA = 2.4f
        const val SIDE_LIFT = 0.15f         // weak lift outside the jet, about the same as gravity
        const val SIDE_LIFT_EXTRA = 0.6f
        const val GUST_CHANCE = 0.03f       // chance per ball per step of a random knock
        const val GUST_MIN = 3f
        const val GUST_EXTRA = 5f
        const val SWIRL = 0.3f
        const val SWIRL_RATE = 0.5f
        const val BOUNCE_SCATTER = 0.45f    // radians, about 26 degrees either way
        /** 6 seconds with the blower off. */
        const val SETTLE_STEPS = 360
    }
}
