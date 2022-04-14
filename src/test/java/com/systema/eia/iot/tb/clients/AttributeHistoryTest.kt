package com.systema.eia.iot.tb.clients

import com.systema.eia.iot.tb.persistence.remove.TB_URL
import com.systema.eia.iot.tb.utils.Scope
import com.systema.eia.iot.tb.utils.SubscriptionType
import com.systema.eia.iot.tb.ws.saveAttributeChanges
import com.systema.eia.iot.tb.ws.subscribeToWS
import com.systema.eia.iot.tb.ws.telemetryAttribute
import org.junit.Assert
import org.junit.Test
import java.lang.Thread.sleep
import java.util.*

class AttributeHistoryTest {


    @Test
    fun `it should save attribute changes as telemetry`() {
        val restClient = ExtRestClient(TB_URL)

        val device = restClient.getOrCreateDevice("attr_history_${UUID.randomUUID()}")

        restClient.saveAttributeChanges(device.id)

        sleep(1000)

        val testAttribute = "foo"
        val testValue = "bar"
        val attributeScope = Scope.SHARED_SCOPE

        var attrTelemetry = false
        restClient.subscribeToWS(device.id, SubscriptionType.LATEST_TELEMETRY) { attrUpdates ->

            if (attrUpdates.any { it.key == telemetryAttribute(attributeScope, testAttribute) }) {
                attrTelemetry = true
            }
        }

        sleep(1000)

        restClient.saveAttribute(device.id, attributeScope, testAttribute, testValue)

        sleep(3000)


        // cleanup
        restClient.deleteDevice(device.id)

        Assert.assertTrue(attrTelemetry)
    }
}