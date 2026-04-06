package com.example.superiorcontroller.input

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Adds temporal quantization on top of spatial quantization for assisted recording.
 *
 * Grid mode: state changes only committed at fixed grid intervals.
 * Pulse mode (right stick): continuous holds → discrete ON/OFF pulses.
 *
 * All intervals are multiples of 8ms to align with the HID report slot grid.
 */
class TemporalQuantizer(private val scope: CoroutineScope) {

    var leftMode = MODE_FREE
    var rightMode = MODE_FREE

    var onCommitLeft: ((Float, Float) -> Unit)? = null
    var onCommitRight: ((Float, Float) -> Unit)? = null

    var isActive: Boolean = false
        private set

    private var tickJob: Job? = null

    // ── Left stick state ──
    @Volatile private var leftPendingX = 0f
    @Volatile private var leftPendingY = 0f
    private var leftCommX = 0f
    private var leftCommY = 0f
    private var leftElapsed = 0L

    // ── Right stick state ──
    @Volatile private var rightPendingX = 0f
    @Volatile private var rightPendingY = 0f
    private var rightCommX = 0f
    private var rightCommY = 0f
    private var rightElapsed = 0L
    private var rightPulseOn = false
    private var rightPulseElapsed = 0L

    fun feedLeft(x: Float, y: Float) {
        leftPendingX = x
        leftPendingY = y
    }

    fun feedRight(x: Float, y: Float) {
        rightPendingX = x
        rightPendingY = y
    }

    fun start() {
        reset()
        isActive = true
        tickJob?.cancel()
        tickJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    fun stop() {
        isActive = false
        tickJob?.cancel()
        tickJob = null
    }

    private fun tick() {
        if (leftMode == MODE_GRID) tickLeftGrid()
        when (rightMode) {
            MODE_GRID -> tickRightGrid()
            MODE_PULSE -> tickRightPulse()
        }
    }

    // ── Left stick: grid ──

    private fun tickLeftGrid() {
        leftElapsed += TICK_MS
        val px = leftPendingX
        val py = leftPendingY
        val pendingNeutral = px == 0f && py == 0f
        val committedActive = leftCommX != 0f || leftCommY != 0f

        if (pendingNeutral && committedActive) {
            commitLeft(0f, 0f)
            leftElapsed = 0L
            return
        }
        if (!pendingNeutral && !committedActive) {
            commitLeft(px, py)
            leftElapsed = 0L
            return
        }
        if (leftElapsed >= LEFT_GRID_MS) {
            if (px != leftCommX || py != leftCommY) {
                commitLeft(px, py)
            }
            leftElapsed = 0L
        }
    }

    // ── Right stick: grid ──

    private fun tickRightGrid() {
        rightElapsed += TICK_MS
        val px = rightPendingX
        val py = rightPendingY
        val pendingNeutral = px == 0f && py == 0f
        val committedActive = rightCommX != 0f || rightCommY != 0f

        if (pendingNeutral && committedActive) {
            commitRight(0f, 0f)
            rightElapsed = 0L
            return
        }
        if (!pendingNeutral && !committedActive) {
            commitRight(px, py)
            rightElapsed = 0L
            return
        }
        if (rightElapsed >= RIGHT_GRID_MS) {
            if (px != rightCommX || py != rightCommY) {
                commitRight(px, py)
            }
            rightElapsed = 0L
        }
    }

    // ── Right stick: pulse ──

    private fun tickRightPulse() {
        rightPulseElapsed += TICK_MS
        val px = rightPendingX
        val py = rightPendingY
        val pendingNeutral = px == 0f && py == 0f

        if (pendingNeutral) {
            if (rightCommX != 0f || rightCommY != 0f) {
                commitRight(0f, 0f)
            }
            rightPulseOn = false
            rightPulseElapsed = 0L
            return
        }

        if (!rightPulseOn) {
            rightPulseOn = true
            commitRight(px, py)
            rightPulseElapsed = 0L
            return
        }

        val isEmitting = rightCommX != 0f || rightCommY != 0f
        if (isEmitting && rightPulseElapsed >= PULSE_ON_MS) {
            commitRight(0f, 0f)
            rightPulseElapsed = 0L
        } else if (!isEmitting && rightPulseElapsed >= PULSE_OFF_MS) {
            commitRight(rightPendingX, rightPendingY)
            rightPulseElapsed = 0L
        }
    }

    // ── Commit helpers ──

    private fun commitLeft(x: Float, y: Float) {
        leftCommX = x; leftCommY = y
        onCommitLeft?.invoke(x, y)
    }

    private fun commitRight(x: Float, y: Float) {
        rightCommX = x; rightCommY = y
        onCommitRight?.invoke(x, y)
    }

    private fun reset() {
        leftPendingX = 0f; leftPendingY = 0f
        leftCommX = 0f; leftCommY = 0f
        leftElapsed = 0L
        rightPendingX = 0f; rightPendingY = 0f
        rightCommX = 0f; rightCommY = 0f
        rightElapsed = 0L
        rightPulseOn = false
        rightPulseElapsed = 0L
    }

    companion object {
        const val MODE_FREE = "free"
        const val MODE_GRID = "grid"
        const val MODE_PULSE = "pulse"

        const val LEFT_GRID_MS = 48L
        const val RIGHT_GRID_MS = 96L
        const val PULSE_ON_MS = 80L
        const val PULSE_OFF_MS = 80L
        private const val TICK_MS = 8L
    }
}
