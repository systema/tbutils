package com.systema.eia.iot.tb.examples

import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.utils.DeviceTwin
import com.systema.eia.iot.tb.utils.Scope
import com.systema.eia.iot.tb.utils.SubscriptionType
import com.systema.eia.iot.tb.utils.json
import com.systema.eia.iot.tb.ws.subscribeToWS

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

fun main() {
    val restClient = ExtRestClient(System.getenv("TB_URL") ?: "http://localhost:9090")
    val deviceById = restClient.getOrCreateDevice("attr wssub test device")!!

    val twin = DeviceTwin(deviceById)
    restClient.subscribeToWS(deviceById.id, SubscriptionType.SHARED_SCOPE) { updates ->
        updates.forEach { twin.update(SubscriptionType.SHARED_SCOPE, it) }
    }

    GlobalScope.launch {
        var counter = 1

        while (true) {
            delay(500)

            restClient.saveAttribute(deviceById.id, Scope.SERVER_SCOPE, "foo", "bar_${counter++}")
        }
    }


    // https://thingsboard.io/docs/user-guide/telemetry/
    // also see https://github.com/thingsboard/thingsboard/blob/master/application/src/main/java/org/thingsboard/server/service/telemetry/cmd/TelemetryPluginCmdsWrapper.java
    object : WebSocketClient(
        URI(
            "ws://${System.getenv("TB_HOST")}:${System.getenv("TB_PORT")}" +
                    "/api/ws/plugins/telemetry?token=${restClient.token}"
        )
    ) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            val attrSubscription = json {
                "entityType" to "DEVICE"
                "entityId" to deviceById.id
                "scope" to Scope.SERVER_SCOPE
                "cmdId" to 10
            }

            val deviceSubscriptionWrapper = json {
                "tsSubCmds" to arrayOf<String>()
                "historyCmds" to arrayOf<String>()
                "attrSubCmds" to arrayOf(attrSubscription)
            }

            send(deviceSubscriptionWrapper.toString())
        }

        override fun onMessage(message: String?) {
            println("received attribute change via ws: $message")
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            println("closing $code $reason $remote")
        }

        override fun onError(ex: Exception?) {
            println("error $ex")
        }
    }.connect()
}