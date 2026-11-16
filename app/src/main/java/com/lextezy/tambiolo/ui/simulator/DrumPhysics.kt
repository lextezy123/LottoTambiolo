package com.lextezy.tambiolo.ui.simulator

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
        for (b in balls) {
            if (b.flying) continue
            b.vy += 0.35f // gravity
            if (mixing > 0f) {
                // Air blower: strong random lift only in the lower part of the drum,
                // so balls shoot up, arc, and fall back down instead of pinning to the top.
                val depth = (b.y - CY) / RADIUS // -1 = top, 1 = bottom
                if (depth > 0.35f) b.vy -= mixing * (1.2f + Random.nextFloat() * 1.6f)
                // Random jitter everywhere
                b.vx += (Random.nextFloat() * 2f - 1f) * 1.2f * mixing
                b.vy += (Random.nextFloat() * 2f - 1f) * 0.8f * mixing
                // Gentle swirl around the center
                val sx = b.x - CX; val sy = b.y - CY
                val sd = hypot(sx, sy).coerceAtLeast(1f)
                b.vx += -sy / sd * 0.25f * mixing
                b.vy += sx / sd * 0.25f * mixing
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
                    if (dot > 0) { b.vx -= 1.8f * dot * nx; b.vy -= 1.8f * dot * ny }
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
        /** 6 seconds with the blower off. */
        const val SETTLE_STEPS = 360
    }
}
