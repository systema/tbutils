package com.systema.eia.iot.tb.stats

import com.systema.eia.iot.tb.stats.MovingAverage
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.lang.Thread.sleep

class MATests {

    @Test
    fun `it should calculate the moving average correctly`() {
        val ma = MovingAverage(4)

        ma.currentAverage() shouldBe null

        ma.addMeasurement(4.0)
        sleep(1000)

        ma.currentAverage() shouldBe 4.0


        ma.addMeasurement(4.0)
        sleep(1000)
        ma.addMeasurement(2.0)
        sleep(1000)
        ma.addMeasurement(2.0)
        sleep(1000)

        ma.currentAverage() shouldBe 3.0


        repeat(5){
            ma.addMeasurement(1.0)
            sleep(1000)
        }

        ma.currentAverage() shouldBe 1.0
    }

}