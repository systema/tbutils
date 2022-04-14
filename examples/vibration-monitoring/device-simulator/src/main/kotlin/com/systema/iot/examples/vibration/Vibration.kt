package com.systema.iot.examples.vibration

import java.util.*
import kotlin.concurrent.schedule
import kotlin.random.Random

/**
 * Class modeling linearly increasing vibration, causing a breakdown eventually.
 */
class Vibration(val brokenTask: (broken: Boolean) -> Unit) {
    /**
     * Vibration level after maintenance.
     */
    private val START_VALUE = 3.0

    /**
     * Unhealthy vibration level.
     */
    private val BREAKAGE_LIMIT = 40.0

    /**
     * Time in seconds after exceeding the breakage limit before device breaks.
     */
    private val BREAKAGE_DELAY: Long = 5

    /**
     * Maximum vibration limit. Vibration will increase up to this level, but no further.
     */
    private val HIGH_LIMIT = 50.0

    private val NOISE_AMPLITUDE = 1.0

    /**
     * Value by which the vibration increases each time when querying the current value.
     */
    private val SLOPE = Random.nextDouble(0.05, 0.5)

    private val noise get() = Random.nextDouble(-NOISE_AMPLITUDE, NOISE_AMPLITUDE)

    var value: Double = START_VALUE
        set(value) {
            log.info { "Resetting vibration to $value" }
            field = value
        }
        get() {
            if(field <= HIGH_LIMIT)
                field += SLOPE
            if(field >= BREAKAGE_LIMIT && breakageTimer==null) {
                log.info { "Breakage vibration limit exceeded, breaking in ${BREAKAGE_DELAY}s" }

                breakageTimer= Timer().apply{
                    schedule(BREAKAGE_DELAY * 1000) {
                        broken = true
                        breakageTimer = null
                    }
                }
            }
            return field + noise
        }

    private var broken = false
        set(value) {
            log.info { "Device ${if(value) "is broken" else "was fixed"}" }
            field = value
            brokenTask(value)
        }

    private var breakageTimer: Timer? = null

    fun reset() {
        value = START_VALUE
        broken = false
        breakageTimer?.cancel()
        breakageTimer = null
    }
}