package com.systema.eia.iot.tb.persistence.remove

import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.persistence.remove.TbRemover
import com.systema.eia.iot.tb.persistence.search.TbFinder
import org.junit.Assert
import org.junit.Test
import org.springframework.web.client.HttpClientErrorException
import org.thingsboard.server.common.data.id.DeviceId
import java.net.MalformedURLException
import java.net.URL
import java.util.*

val TB_URL = try {
    URL(System.getenv("TB_URL") ?: "http://localhost:9090")
} catch (e: MalformedURLException) {
    URL("http://" + System.getenv("TB_URL"))
}
val TB_MQTT_HP = System.getenv("TB_MQTT_HP") ?: "localhost:1884";

class RemoveTest {

    val remover = TbRemover(TB_URL)
    val finder = TbFinder(TB_URL)

    val extRestClient = ExtRestClient(TB_URL)


    @Test(expected = HttpClientErrorException::class)
    fun `remove not existed device with API throws HttpClientErrorException$NotFound`() {
        remover.restClient.deleteDevice(DeviceId(UUID.randomUUID()))
    }

    @Test
    fun `remove device profile cascade`() {
        val testDeviceProfileName = "test-device-profile"

        if (finder.deviceProfile.getByName(testDeviceProfileName) == null) {
            val deviceProfile = finder.deviceProfile.getByName("default")!!
            deviceProfile.id = null
            deviceProfile.isDefault = false
            deviceProfile.name = testDeviceProfileName
            extRestClient.saveDeviceProfile(deviceProfile)
        }

        val expectedDeviceProfile = finder.deviceProfile.getByName(testDeviceProfileName)!!
        val expectedDevices = mutableListOf(
            extRestClient.getOrCreateDevice("test-device1", testDeviceProfileName),
            extRestClient.getOrCreateDevice("test-device2", testDeviceProfileName),
            extRestClient.getOrCreateDevice("test-device3", testDeviceProfileName)
        ).map { it.name }

        val actualData = remover.deviceProfile.removeIfExistCascadeById(expectedDeviceProfile.id.toString())

        Assert.assertNotNull(actualData)

        val actualDevices = actualData!!.devices.map { it.name }
        val actualDeviceProfile = actualData.deviceProfile

        Assert.assertArrayEquals(expectedDevices.toTypedArray(), actualDevices.toTypedArray())
        Assert.assertEquals(expectedDeviceProfile, actualDeviceProfile)

        val removedDevices = finder.device.getAll().filter { actualDevices.contains(it.name) }
        val removedDeviceProfile = finder.deviceProfile.getByName(testDeviceProfileName)

        Assert.assertTrue(removedDevices.isEmpty())
        Assert.assertNull(removedDeviceProfile)
    }
}