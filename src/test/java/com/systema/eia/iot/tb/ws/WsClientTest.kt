package com.systema.eia.iot.tb.ws

import com.systema.eia.iot.tb.TbTest
import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.persistence.remove.TB_URL
import com.systema.eia.iot.tb.utils.Scope
import com.systema.eia.iot.tb.utils.SubscriptionType
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.thingsboard.server.common.data.Device
import org.thingsboard.server.common.data.id.DeviceId
import java.lang.Thread.sleep
import java.util.*

class WsClientTest : TbTest() {

    @Test
    fun `it should allow to define a custom handler`() {
        val restClient = ExtRestClient(TB_URL);
        val device = restClient.getOrCreateDevice("ws_client_test_device_${UUID.randomUUID()}")


        var subSuccess = false

        restClient.subscribeToWS(device.id, SubscriptionType.LATEST_TELEMETRY, null) { attrUpdates ->
            if (attrUpdates.isEmpty()) return@subscribeToWS
            if (attrUpdates[0].key != "foo") return@subscribeToWS

            subSuccess = true
        }

        restClient.sendTelemetry(device.id, "foo", 3)
        sleep(1000)
        subSuccess shouldBe true
    }

    @Test
    fun `it should generate correct subscription string`() {
        val deviceId = DeviceId(UUID.randomUUID());
        val actualPair =
            buildWsSubscriptionJsonWithId(deviceId, SubscriptionType.SHARED_SCOPE, listOf("some", "key", "sequence"))
        println(actualPair.second.toString())
        var expected = "{\n" +
                "       \"attrSubCmds\": [\n" +
                "           {\n" +
                "               \"cmdId\": " + actualPair.first + ",\n" +
                "               \"entityType\": \"DEVICE\",\n" +
                "               \"keys\": \"some,key,sequence\",\n" +
                "               \"scope\": \"SHARED_SCOPE\",\n" +
                "               \"entityId\": \"" + deviceId.toString() + "\"\n" +
                "           }\n" +
                "       ],\n" +
                "       \"tsSubCmds\": [],\n" +
                "       \"historyCmds\": []\n" +
                "   }"
        expected = expected.replace(" ", "").replace("\n", "")
        actualPair.second.toString() shouldBe expected
    }

    private fun websocketMsgHandlerLogic(
        expectedMessages: MutableList<Pair<String, Any>>, changes:
        List<AttrUpdate>
    ) {
        changes.forEach {
            println("Expected messages: $expectedMessages")
            if (expectedMessages.remove(Pair(it.key, it.value ?: "null")))
                println("Received $it")
            else
                fail("Received unexpected message: $it")
        }
    }

    private fun sendMsgAndSaveToExpected(
        restClient: ExtRestClient, device: Device, expectedMessages: MutableList<Pair<String, Any>>, scope:
        SubscriptionType, key: String, value: Any
    ) {
        // values received via websocket are always Strings -> we need to expect only Strings
        expectedMessages.add(Pair(key, value.toString()))
        sendWithoutSaving(restClient, device, scope, key, value)
    }

    private fun sendWithoutSaving(
        restClient: ExtRestClient,
        device: Device,
        scope: SubscriptionType,
        key: String,
        value: Any
    ) {
        if (scope == SubscriptionType.LATEST_TELEMETRY)
            restClient.sendTelemetry(device.id, key, value)
        else
            restClient.saveAttribute(device.id, Scope.valueOf(scope.toString()), key, value)
    }

    @Test
    fun `it should receive websocket messages`() {
        val restClient = ExtRestClient(tbUrl)
        val device = restClient.getOrCreateDevice("ws-test-device-" + (0..10000).random())
        try {
            val expectedMessages = mutableListOf<Pair<String, Any>>()
            fun subscribe(scope: SubscriptionType) {
                restClient.subscribeToWS(device.id, scope) { changes ->
                    websocketMsgHandlerLogic(expectedMessages, changes)
                }
            }
            subscribe(SubscriptionType.SHARED_SCOPE)
            subscribe(SubscriptionType.CLIENT_SCOPE)
            subscribe(SubscriptionType.LATEST_TELEMETRY)

            fun send(scope: SubscriptionType, key: String, value: Any) {
                sendMsgAndSaveToExpected(restClient, device, expectedMessages, scope, key, value)
            }

            send(SubscriptionType.SHARED_SCOPE, "attr1", "val1")
            send(SubscriptionType.CLIENT_SCOPE, "attr2", 2)
            send(SubscriptionType.LATEST_TELEMETRY, "telem", true)

            sleep(2000)

            assert(expectedMessages.isEmpty()) { "Did not receive expected messages: $expectedMessages" }
        } finally {
            restClient.deleteDevice(device.id)
        }
    }

    @Test
    fun `it should receive websocket messages according to key filtering`() {
        val restClient = ExtRestClient(tbUrl)
        val device = restClient.getOrCreateDevice("ws-test-device-" + (0..10000).random())
        try {
            val expectedMessages = mutableListOf<Pair<String, Any>>()
            fun subscribe(scope: SubscriptionType, keys: List<String>) {
                restClient.subscribeToWS(device.id, scope, keys) { changes ->
                    websocketMsgHandlerLogic(expectedMessages, changes)
                }
            }
            subscribe(SubscriptionType.SHARED_SCOPE, listOf("attr1", "someOtherKey"))
            subscribe(SubscriptionType.CLIENT_SCOPE, listOf("attr2"))
            subscribe(SubscriptionType.LATEST_TELEMETRY, listOf("tele", "foo"))

            // 1st message when subscribing to specific telemetry keys is always the latest value
            expectedMessages.add(Pair("tele", "null"))
            expectedMessages.add(Pair("foo", "null"))

            fun sendExpected(scope: SubscriptionType, key: String, value: Any) {
                sendMsgAndSaveToExpected(restClient, device, expectedMessages, scope, key, value)
            }

            fun sendIgnored(scope: SubscriptionType, key: String, value: Any) {
                sendWithoutSaving(restClient, device, scope, key, value)
            }

            sendExpected(SubscriptionType.SHARED_SCOPE, "attr1", "val1")
            sendIgnored(SubscriptionType.SHARED_SCOPE, "ignoreMe", "Donald T.")
            sendExpected(SubscriptionType.CLIENT_SCOPE, "attr2", 2)
            sendIgnored(SubscriptionType.CLIENT_SCOPE, "trumpsTheBestPresidentEver", false)
            sendExpected(SubscriptionType.LATEST_TELEMETRY, "tele", true)
            sendIgnored(
                SubscriptionType.LATEST_TELEMETRY, "mrPresident", "you got more ice cream than me - youre " +
                        "fired!"
            )
            sendIgnored(SubscriptionType.LATEST_TELEMETRY, "donaldsIq", 47.11)

            sleep(2000)

            assert(expectedMessages.isEmpty()) { "Did not receive expected messages: $expectedMessages" }
        } finally {
            restClient.deleteDevice(device.id)
        }
    }
}