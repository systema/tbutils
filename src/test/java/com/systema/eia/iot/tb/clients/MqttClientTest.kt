package com.systema.eia.iot.tb.clients

import com.systema.eia.iot.tb.persistence.remove.TB_MQTT_HP
import com.systema.eia.iot.tb.persistence.remove.TB_URL
import com.systema.eia.iot.tb.utils.Scope
import org.junit.Assert
import org.junit.Test
import org.thingsboard.server.common.data.Device
import java.util.*
import java.util.concurrent.TimeUnit

class MqttClientTest {


    val restClient = ExtRestClient(TB_URL);
    val deviceMqttClient: DeviceMqttClient

    val testDeviceName: String = "Test Device A1"
    val testDeviceToken: String
    val testDevice: Device

    init {
        testDevice = restClient.getOrCreateDevice(testDeviceName)
        testDeviceToken = restClient.getDeviceTokenByDeviceId(testDevice.id)!!

        deviceMqttClient = DeviceMqttClient(TB_MQTT_HP, DeviceToken(testDeviceToken))

        println("Test device Name '$testDeviceName'")
        println("Test device Token '$testDeviceToken'")
    }

    @Test
    fun `send telemetry`() {
        val random = UUID.randomUUID().toString()
        deviceMqttClient.sendTelemetry(mapOf(Pair("test", random)))
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        val entry = restClient.getLatestTimeseries(testDevice.id, listOf("test")).first()
        Assert.assertEquals(random, entry.value)
        Assert.assertEquals("test", entry.key)
    }

    @Test
    fun `send client attribute`() {
        val random = UUID.randomUUID().toString()
        deviceMqttClient.sendClientAttribute(mapOf(Pair("testClientAttr", random)))
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        val entry = restClient.getAttributeKvEntries(testDevice.id, listOf("testClientAttr")).first()
        Assert.assertEquals(random, entry.value)
        Assert.assertEquals("testClientAttr", entry.key)
    }


    @Test
    fun `subscribe shared attributes`() {
        val random = UUID.randomUUID().toString()
        var isOk = false;

        deviceMqttClient.subscribeSharedAttributes {
            println(it)
            assert(it.containsKey("testSharedAttr"))
            Assert.assertEquals(random, it["testSharedAttr"])
            isOk = true;
        }
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        restClient.saveAttributes(testDevice.id, mapOf(Pair("testSharedAttr", random)), Scope.SHARED_SCOPE)

        Thread.sleep(TimeUnit.SECONDS.toMillis(2))
        deviceMqttClient.mqttClient.disconnect(true)

        assert(isOk)
    }

    @Test
    fun `request client or shared attributes`() {

        val random = UUID.randomUUID().toString()
        var isOk = false;

        restClient.saveAttributes(testDevice.id, mapOf(Pair("testSharedAttr", random)), Scope.SHARED_SCOPE)
        deviceMqttClient.sendClientAttribute(mapOf(Pair("testClientAttr", random)))
        // restClient.sendAttribute(mapOf(Pair("testClientAttr", random)), Scope.CLIENT_SCOPE, testDevice.id)
        Thread.sleep(TimeUnit.SECONDS.toMillis(2))

        deviceMqttClient.requestClientOrShareAttributes(
            1,
            listOf("testClientAttr"),
            listOf("testSharedAttr")
        )
        { client, shared ->
            println(shared)
            println(client)
            assert(shared.containsKey("testSharedAttr"))
            assert(client.containsKey("testClientAttr"))
            Assert.assertEquals(random, shared["testSharedAttr"])
            Assert.assertEquals(random, client["testClientAttr"])
            isOk = true;
        }

        Thread.sleep(TimeUnit.SECONDS.toMillis(1))
        assert(isOk)

    }


}