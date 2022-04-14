package com.systema.eia.iot.tb.ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.utils.Scope
import com.systema.eia.iot.tb.utils.SubscriptionType
import com.systema.eia.iot.tb.utils.json
import org.java_websocket.client.WebSocketClient
import org.thingsboard.server.common.data.id.DeviceId

private val mapper = ObjectMapper()


//TODO: add docs
fun ExtRestClient.saveAttributeChanges(deviceId: DeviceId): List<Pair<Scope, WebSocketClient>> {
//    val attrScope = SubscriptionType.SERVER_SCOPE
//    val attrTelemetryPrefix = "attrhist"

    //todo handle or return websockets
    return Scope.values().map { attrScope ->
        val socket = subscribeToWS(deviceId, SubscriptionType.valueOf(attrScope.name)) { attrChanges ->

//        val sortedBy = attrChanges.sortedBy { it.timestamp }

            attrChanges.forEach { attrUpdate ->
                // https://stackoverflow.com/questions/12539489/from-org-json-jsonobject-to-org-codehaus-jackson
                val json = json {
                    "ts" to attrUpdate.timestamp
                    "values" to {
                        telemetryAttribute(attrScope, attrUpdate.key) to attrUpdate.value
                    }
                }

                // attrTelemetryPrefix + "::" + attrScope.toString() + "::" + attrUpdate.key to attrUpdate.value

                sendTelemetry(deviceId, mapper.readTree(json.toString()))
            }
        }

        attrScope to socket
    }
}

//TODO: simplify naming pattern "attrHist__${scope}__${attributeName}"?
/** We simply set a convention of how attribute names can be mapped to telemetry names. This includes the scope and the attribute names to become unique.  */
// json does not work here as key yet, see https://github.com/thingsboard/thingsboard/issues/4657
//fun telemetryAttribute(
//    scope: Scope,
//    attributeName: String?
//) = json {
//    "attrscope" to scope
//    "name" to attributeName
//}.toString()
fun telemetryAttribute(
    scope: Scope,
    attributeName: String?
) = "attrscope__${scope}__name__${attributeName}"


/** Returns true if the provided telemetry key relates to a persisted attribute.*/
//fun String.isAttrTelemetryKey() = contains("attrscope") && startsWith("{")
fun String.isAttrTelemetryKey() = startsWith("attrscope__")