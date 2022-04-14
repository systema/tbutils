package com.systema.eia.iot.tb.stats

import java.time.Duration
import java.time.Instant

private data class TimedMeasurement(val timestamp: Instant, val value: Double)

/** Calculate a moving average over a defined time range. If the number of records falls below `minSamples` null will
 * be returned. The API is opinionated, as it assumed Instant.now to be the current time. Records are timed accordingly
 * at ingest time. */

class MovingAverage(var timeWindowSec: Int, var minSamples: Int = 1) {

    private var mutableList = mutableListOf<TimedMeasurement>()

    @JvmOverloads
    fun addMeasurement(value: Double, timestamp: Instant = Instant.now()) {
        reduceHistory(timestamp)
        mutableList.add(TimedMeasurement(timestamp, value))
    }

    @JvmOverloads
    fun currentAverage(now: Instant = Instant.now()): Double? {
        reduceHistory(now)
        return if (mutableList.size < minSamples) null else mutableList.map { it.value }.average()
    }

    private fun reduceHistory(now: Instant = Instant.now()) {
        mutableList.removeIf {
            Duration.between(it.timestamp, now).toSeconds() > timeWindowSec
        }
    }
}
