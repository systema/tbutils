package com.systema.eia.iot.tb.clients

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.systema.eia.iot.tb.utils.SimpleMqttClient


// list of topics
private val TOPIC_TELEMETRY_UPLOAD = "v1/devices/me/telemetry"
private val TOPIC_ATTRIBUTES = "v1/devices/me/attributes"
private val TOPIC_ATTRIBUTES_RESPONSE_PLUS = "v1/devices/me/attributes/response/+"
private val TOPIC_ATTRIBUTES_REQUEST = "v1/devices/me/attributes/request/"
private val TOPIC_RPC = "v1/devices/me/rpc/request/"
//private val TOPIC_CLAMING = "v1/devices/me/claim"
//private val DEVICE_PROVISIONING = "/provision"

private val mapper = ObjectMapper()


/** Simple type wrapper around device tokens. */
data class DeviceToken(val token: String)


/**
 * Create Mqtt clients for Tb Devices to send telemetry, send and subscribe attributes
 * more read here https://thingsboard.io/docs/reference/mqtt-api/#telemetry-upload-api
 * used
 * @author ViB, HoB
 */
class DeviceMqttClient(val mqttClient: SimpleMqttClient) {

    constructor(tbMqttHostPort: String, token: DeviceToken) : this(
        SimpleMqttClient(
            brokerUrl = tbMqttHostPort,
            username = token.token
        )
    )

    /**
     * Send telemetry to a device
     * @param node json message
     * @param token of device. to get token, use ExtRestClient::getDeviceTokenBy*
     * */
    fun sendTelemetry(node: JsonNode) {
        // mqtt pub -v -h "thingsboard.cloud" -t "v1/devices/me/telemetry" -u '$ACCESS_TOKEN' -s
        mqttClient.publish(TOPIC_TELEMETRY_UPLOAD, node, true)
    }

    /**
     * Send telemetry to a device
     * @param map key-value telemetry map
     * @param token of device. to get token, use ExtRestClient::getDeviceTokenBy*
     */
    fun sendTelemetry(map: Map<String, Any?>) {
        sendTelemetry(mapper.convertValue(map, JsonNode::class.java))
    }

    /**
     * Send client attribute to a device
     * @param node attributes as json node
     * @param token of device. to get token, use ExtRestClient::getDeviceTokenBy*
     */
    fun sendClientAttribute(node: JsonNode) {
        //mqtt pub -d -h "127.0.0.1" -t "v1/devices/me/attributes" -u '$ACCESS_TOKEN' -s
        mqttClient.publish(TOPIC_ATTRIBUTES, node, true)
    }


    /**
     * Send client attribute to a device
     * @param map key-value attribute map
     * @param token of device. to get token, use ExtRestClient::getDeviceTokenBy*
     */
    fun sendClientAttribute(map: Map<String, Any?>) {
        sendClientAttribute(mapper.convertValue(map, JsonNode::class.java))
    }


    /**
     * subscribe for changing of device shared attributes
     * @param token device token
     * @param listener callled if attr has been changed
     * @return SimpleMqttClient to manually disconnect client
     */
    fun subscribeSharedAttributes(listener: (message: Map<String, Any?>) -> Unit) {
        //mosquitto_sub -d -h "thingsboard.cloud" -t "v1/devices/me/attributes" -u "$ACCESS_TOKEN"

        // get client with a username "token"

        // subscribe for device
        mqttClient.subscribe(TOPIC_ATTRIBUTES) { _, message ->
            run {
                // read input as json
                val json = mapper.readTree(message.toString())
                val attr = mapper.convertValue(
                    json,
                    Map::class.java
                ) as? Map<String, Any?> ?: mapOf()
                listener(attr)
            }
        }
    }


    /**
     * Get device client or shared attributes
     * Disconnected automatically after 2 seconds
     * @param token device token
     * @param requestId any integer
     * @param clientAttributesKeys list of a client attributes keys to read
     * @param sharedAttributesKeys list of shared attributes keys to read
     * @param messageArrivedListener called when message with attributes arrived
     */
    fun requestClientOrShareAttributes(
        requestId: Int = java.util.Random().nextInt(),
        clientAttributesKeys: List<String> = listOf(),
        sharedAttributesKeys: List<String> = listOf(),
        messageArrivedListener: (sharedAttributes: Map<String, Any?>, clientAttributes: Map<String, Any?>) -> Unit
    ) {
        // output message
        val node = mapper.readTree("{}") as ObjectNode

        // read input attributes and put it to output json messages
        if (clientAttributesKeys.isNotEmpty()) {
            node.put("clientKeys", clientAttributesKeys.joinToString(","))
        }

        if (sharedAttributesKeys.isNotEmpty()) {
            node.put("sharedKeys", sharedAttributesKeys.joinToString(","))
        }

        // subscribe for a attributes
        mqttClient.subscribe(TOPIC_ATTRIBUTES_RESPONSE_PLUS) { _, message ->
            run {
                val json = mapper.readTree(message.toString())

                // parse input message as key-value maps
                val clientAttr = mapper.convertValue(
                    json.get("client"),
                    Map::class.java
                ) as? Map<String, Any?> ?: mapOf()

                val sharedAttr = mapper.convertValue(
                    json.get("shared"),
                    Map::class.java
                ) as? Map<String, Any?> ?: mapOf()

                messageArrivedListener(clientAttr, sharedAttr)
            }
        }

        mqttClient.publish(getTopicAttrRequest(requestId.toString()), node, true)
    }


    /**
     * Return RPC topic with request id
     * @param request_id
     */
    private fun getTopicRPC(request_id: String): String {
        return "$TOPIC_RPC$request_id";
    }

    /**
     * Return Attr request topic with request id
     * @param request_id
     */
    private fun getTopicAttrRequest(request_id: String): String {
        return "$TOPIC_ATTRIBUTES_REQUEST$request_id";
    }
}