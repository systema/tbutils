package com.systema.eia.iot.tb.examples

import com.fasterxml.jackson.databind.ObjectMapper
import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.utils.SubscriptionType
import com.systema.eia.iot.tb.utils.json
import com.systema.eia.iot.tb.ws.subscribeToWS

// related to https://progress.systemagmbh.de/browse/SYSTEMA_IOT-228
// for actual implementations simply use AttributeHistory
fun main() {

    val restClient = ExtRestClient(System.getenv("TB_URL"), System.getenv("TB_USER"), System.getenv("TB_PW"))

    // better use Finder.device.getByName or getById
//    val deviceById = restClient.getDeviceById(DeviceId(UUID.fromString("9934f790-7ccc-11eb-aedb-57e8df9a6351"))).get()
    val deviceById = restClient.getOrCreateDevice("attribute-tester")

    println(deviceById)

    val mapper = ObjectMapper()

    val attrScope = SubscriptionType.SERVER_SCOPE
//    val attrTelemetryPrefix = "attrhist"
    restClient.subscribeToWS(deviceById.id, attrScope) { attrChanges ->

//        val sortedBy = attrChanges.sortedBy { it.timestamp }

        attrChanges.forEach { attrUpdate ->
            val json = json {
                "ts" to attrUpdate.timestamp
                "values" to {
//                    attrTelemetryPrefix + "::" + attrScope.toString() + "::" + attrUpdate.key to attrUpdate.value
                    json{
                        "attrscope" to attrScope
                        "name" to attrUpdate.key
                    }.toString() to attrUpdate.value
                }
            }

            restClient.sendTelemetry(deviceById.id, mapper.readTree(json.toString()))
        }
    }
}