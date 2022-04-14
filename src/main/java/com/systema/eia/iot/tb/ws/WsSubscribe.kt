package com.systema.eia.iot.tb.ws

import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.utils.SubscriptionType
import com.systema.eia.iot.tb.utils.json
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import org.thingsboard.server.common.data.id.DeviceId
import java.net.URI
import java.util.*

fun ExtRestClient.subscribeToWS(
    deviceId: DeviceId,
    type: SubscriptionType,
    msgHandler: (List<AttrUpdate>) -> Unit
) = subscribeToWS(deviceId, type, null, msgHandler)

fun ExtRestClient.subscribeToWS(
    deviceId: DeviceId,
    type: SubscriptionType,
    keys: List<String>?,
    msgHandler: (List<AttrUpdate>) -> Unit
) = subscribeToWSMsg(deviceId, type, keys) { message: String ->
    val parseMessage = WsParserUtils.parseMessage(message).map {
        AttrUpdate(
            it.key,
            it.timestamp,
            it.value
        )
    }

    require(parseMessage != null) { "failed to parse message $message" }

    msgHandler(parseMessage)
}

fun ExtRestClient.subscribeToWSMsg(
    deviceId: DeviceId,
    type: SubscriptionType,
    msgHandler: (String) -> Unit
) = subscribeToWSMsg(deviceId, type, null, msgHandler)

fun ExtRestClient.subscribeToWSMsg(
    deviceId: DeviceId,
    type: SubscriptionType,
    keys: List<String>?,
    msgHandler: (String) -> Unit
): WebSocketClient = object : WebSocketClient(URI("ws://${tbHost}:${tbPort}/api/ws/plugins/telemetry?token=${token}")) {

    override fun onOpen(handshakedata: ServerHandshake?) {
        val deviceSubscriptionWrapper = buildWsSubscriptionJson(deviceId, type, keys)
        send(deviceSubscriptionWrapper.toString())
    }

    override fun onMessage(message: String?) {
        //            println("received attribute change via ws: $message")
        message?.let { msgHandler(it) }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        println("closing $code $reason $remote")
    }

    override fun onError(ex: Exception?) {
        println("error $ex")
    }
}.apply { connect() }

/**
 * Build a ThingsBoard websocket subscription string of the format:
 * ```
 *  {
 *      "attrSubCmds": [
 *          {
 *              "cmdId": 11,
 *              "entityType": "DEVICE",
 *              "scope": <scope>,
 *              "keys": <keys>,
 *              "entityId": <deviceId>
 *          }
 *      ],
 *      "tsSubCmds": [],
 *      "historyCmds": []
 *  }
 * ```
 *
 * @param deviceId  ThingsBoard device Id (see
 *                  [ThingsBoard user guide](https://thingsboard.io/docs/user-guide/ui/devices/#get-device-id)
 * @param type     subscription type
 * @param keys     list of attribute keys to subscribe for updates; if this is a non-empty list, the subscription is
 *                 only going to yield updates for the given attribute names
 * @return JSON object of the above format
 */
fun buildWsSubscriptionJson(deviceId: DeviceId, type: SubscriptionType, keys: List<String>?): JSONObject {
    return buildWsSubscriptionJsonWithId(deviceId, type, keys).second
}

fun buildWsSubscriptionJsonWithId(deviceId: DeviceId, type: SubscriptionType, keys: List<String>?): Pair<Int,
        JSONObject> {
    val id = Random().nextInt()
    val subscriptionConfig = json {
        "entityType" to "DEVICE"
        "entityId" to deviceId.id
        "scope" to type.toString()
        "cmdId" to id
    }
    keys?.let { subscriptionConfig.put("keys", StringBuilder(keys.toString()).replace(Regex("[\\s\\[\\]]"), "")) }
    val subscr = json {
        if (type == SubscriptionType.LATEST_TELEMETRY) {
            "tsSubCmds" to arrayOf(subscriptionConfig)
            "attrSubCmds" to arrayOf<String>()
        } else {
            "attrSubCmds" to arrayOf(subscriptionConfig)
            "tsSubCmds" to arrayOf<String>()
        }

        "historyCmds" to arrayOf<String>()
    }
    return Pair(id, subscr)
}


// usage example
fun main() {
    val restClient = ExtRestClient("http://${System.getenv("TB_HOST")}:${System.getenv("TB_PORT")}")
    val device = restClient.getOrCreateDevice("ws-sub-device")!!

    restClient.subscribeToWS(device.id, SubscriptionType.SHARED_SCOPE, listOf("temperature", "humidity")) { changes ->
        changes.forEach { println("Received attribute update: $it") }
    }

    println("subscribed to attribute changes of $device")
}