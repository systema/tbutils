package com.systema.eia.iot.tb.examples


import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.utils.Scope
import org.thingsboard.server.common.data.id.DeviceId
import java.util.*

// short test to assess pub-sub of attribute changes (which need to be triggered manually via tb-ui here)

fun main() {
    val restClient = ExtRestClient(System.getenv("TB_URL") ?: "http://localhost:9090")

    // better use Finder.device.getByName or getById
    val deviceById = restClient.getDeviceById(DeviceId(UUID.fromString("9934f790-7ccc-11eb-aedb-57e8df9a6351"))).get()

    println(deviceById)

    // wait for attribute change
    val attributeKeys = restClient.getAttributeKeys(deviceById.id)
    println("attributes keys " + attributeKeys)
    println(
        "attributes keys " + restClient.getAttributesByScope(
            deviceById.id,
            Scope.SHARED_SCOPE.toString(),
            attributeKeys
        )
    )
    println("attributes keys " + restClient.getAttributeKvEntries(deviceById.id, attributeKeys))

    val attributeChange = restClient.waitForAttributeChanges(deviceById.id)
    println("attribute changed " + restClient.getAttributeKvEntries(deviceById.id, attributeKeys))
}