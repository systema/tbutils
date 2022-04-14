package com.systema.eia.iot.tb.persistence.search

import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.persistence.remove.TB_URL
import com.systema.eia.iot.tb.utils.RelationDirection
import org.junit.Assert
import org.junit.Test
import org.thingsboard.server.common.data.relation.EntityRelation
import org.thingsboard.server.common.data.relation.RelationTypeGroup

class FinderTest {

    val finder = TbFinder(TB_URL)
    val extRestClient = ExtRestClient(TB_URL)

    @Test
    fun `get all relations by entityId`() {

        val d1 = extRestClient.getOrCreateDevice("test device 1")!!
        val d2 = extRestClient.getOrCreateDevice("test device 2")!!
        val d3 = extRestClient.getOrCreateDevice("test device 3")!!

        val relationType = "test relation"


        extRestClient.saveRelation(EntityRelation(d1.id, d2.id, relationType))
        extRestClient.saveRelation(EntityRelation(d1.id, d3.id, relationType))


        val e1 = extRestClient.getRelation(d1.id, relationType, RelationTypeGroup.COMMON, d2.id).get()
        val e2 = extRestClient.getRelation(d1.id, relationType, RelationTypeGroup.COMMON, d3.id).get()

        val res = finder.relations.getAllByEntityId(d1.id, RelationDirection.BOTH)

        extRestClient.deleteDevice(d1.id)
        extRestClient.deleteDevice(d2.id)
        extRestClient.deleteDevice(d3.id)

        Assert.assertTrue(res.contains(e1))
        Assert.assertTrue(res.contains(e2))
    }

    @Test
    fun `get relations by source and target entityId`() {

        val d1 = extRestClient.getOrCreateDevice("test device 1")!!
        val d2 = extRestClient.getOrCreateDevice("test device 2")!!
        val d3 = extRestClient.getOrCreateDevice("test device 3")!!

        val relationType = "test relation"


        extRestClient.saveRelation(EntityRelation(d1.id, d2.id, relationType))
        extRestClient.saveRelation(EntityRelation(d1.id, d3.id, relationType))


        val e1 = extRestClient.getRelation(d1.id, relationType, RelationTypeGroup.COMMON, d2.id).get()
        val e2 = extRestClient.getRelation(d1.id, relationType, RelationTypeGroup.COMMON, d3.id).get()

        val resE1 = finder.relations.get(d1.id, d2.id, RelationDirection.FROM, relationType)
        val resE2 = finder.relations.get(d1.id, d3.id, RelationDirection.FROM, relationType)

        extRestClient.deleteDevice(d1.id)
        extRestClient.deleteDevice(d2.id)
        extRestClient.deleteDevice(d3.id)

        Assert.assertEquals(resE1, e1)
        Assert.assertEquals(resE2, e2)
    }

    @Test
    fun `get all relations between two entities`() {

        val d1 = extRestClient.getOrCreateDevice("test device 1")!!
        val d2 = extRestClient.getOrCreateDevice("test device 2")!!
        val d3 = extRestClient.getOrCreateDevice("test device 3")!!

        val relationType = "test relation"


        extRestClient.saveRelation(EntityRelation(d1.id, d2.id, relationType))
        extRestClient.saveRelation(EntityRelation(d1.id, d3.id, relationType))


        val e1 = extRestClient.getRelation(d1.id, relationType, RelationTypeGroup.COMMON, d2.id).get()
        val e2 = extRestClient.getRelation(d1.id, relationType, RelationTypeGroup.COMMON, d3.id).get()

        val res = finder.relations.getAllBetween(d1.id, d2.id)

        extRestClient.deleteDevice(d1.id)
        extRestClient.deleteDevice(d2.id)
        extRestClient.deleteDevice(d3.id)

        Assert.assertTrue(res.contains(e1))
        Assert.assertTrue(!res.contains(e2))
    }

    @Test
    fun `it should list all devices by profile type`() {

        val testDeviceProfileName = "test-device-profile"
        if (finder.deviceProfile.getByName(testDeviceProfileName) == null) {
            val deviceProfile = finder.deviceProfile.getByName("default")!!
            deviceProfile.id = null;
            deviceProfile.isDefault = false
            deviceProfile.name = testDeviceProfileName
            extRestClient.saveDeviceProfile(deviceProfile)
        }
        val expectedDevices = mutableListOf(
            extRestClient.getOrCreateDevice("test-device1", testDeviceProfileName)!!,
            extRestClient.getOrCreateDevice("test-device2", testDeviceProfileName)!!,
            extRestClient.getOrCreateDevice("test-device3", testDeviceProfileName)!!
        ).map { it.name }.sorted()

        val actualDevices =
            TbFinder(extRestClient).device.getAllByProfile(testDeviceProfileName).map { it.name }.sorted()
        Assert.assertArrayEquals(expectedDevices.toTypedArray(), actualDevices.toTypedArray())
    }
}