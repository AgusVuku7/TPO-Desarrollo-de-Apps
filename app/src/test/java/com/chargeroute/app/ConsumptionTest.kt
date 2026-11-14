package com.chargeroute.app

import com.chargeroute.app.domain.model.Consumption
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsumptionTest {

    @Test
    fun withAcConsumesTenPercentMore() {
        // 20 * 1,10 = 22
        assertEquals(22.0, Consumption.effectiveKwhPer100Km(20.0, true), 0.001)
    }

    @Test
    fun withoutAcConsumptionIsTheBase() {
        assertEquals(20.0, Consumption.effectiveKwhPer100Km(20.0, false), 0.001)
    }
}
