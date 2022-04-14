package com.systema.eia.iot.tb.utils

import com.fasterxml.jackson.databind.JsonNode
import mu.KLogging
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Mqtt Client for based on mqttv3.MqttClient
 * used slf4j for logging
 * @author ViB
 * @param connOpt  MqttConnectOptions
 * @param reconnectIfLost  if true, try to reconnect if connection has been lost
 */

//https://gist.githubusercontent.com/m2mIO-gister/5275324/raw/2df225d4473f832002a3302ed54a32bd7c5c824e/SimpleMqttClient.java
class SimpleMqttClient(val connOpt: MqttConnectOptions) : MqttCallback {

    val client: MqttClient // mqttv3.MqttClient
    private val BROKER_URL: String // broker url without protocol name

    // https://www.hivemq.com/blog/mqtt-essentials-part-6-mqtt-quality-of-service-levels/
    private val pubQoS = 0 //At most once (0)
    private val subQoS = 0 //At most once (0)

    private val disconnectAttemps = 5
    private val persistence: MqttDefaultFilePersistence

    // logger
    companion object : KLogging()

    //listeners for delivered messages
    private val messageDeliveredListenerFuncMap: MutableMap<IMqttDeliveryToken, () -> Unit> = mutableMapOf()

    // listener for all input messages
    public val messageArrivedListener: (topic: String, message: MqttMessage) -> Unit = { _, _ -> }


    constructor(
        brokerUrl: String = "localhost:1883",
        username: String? = null,
        password: String? = null,
        keepAliveInterval: Int = 500,
        isCleanSession: Boolean = true,
        isAutomaticReconnect: Boolean = true
    ) : this(
        MqttConnectOptions().also {
            it.isCleanSession = isCleanSession
            it.keepAliveInterval = keepAliveInterval
            it.userName = username
            it.password = password?.toCharArray()
            it.serverURIs = arrayOf("tcp://$brokerUrl")
            it.isAutomaticReconnect = isAutomaticReconnect
        }
    )

    init {
        BROKER_URL = connOpt.serverURIs.first()
        persistence = MqttDefaultFilePersistence(System.getProperty("user.dir") + "/paho-tcp")
        client = MqttClient(BROKER_URL, UUID.randomUUID().toString(), persistence)
        client.setCallback(this)
    }


    /**
     * connect client with connOpt from constructor
     * @throws MqttException
     */
    fun connect() {
        logger.info { "connect ${client.clientId} to $BROKER_URL" }
        client.connect(connOpt)
        logger.info { "Connected to $BROKER_URL" }
    }

    /**
     * disconnect and close client
     * if the client does not disconnect, tries to disconnect several times.
     * If the number of attempts to disconnect is exceeded, it disconnects forcibly
     * @param force forcibly disconnect
     */
    fun disconnect(force: Boolean = false) {
        try {
            if (force) {
                logger.info { "force disconnect ${client.clientId}" }
                client.disconnectForcibly()
                client.close(force)
                return
            }

            logger.info { "Try disconnect ${client.clientId}" }
            client.disconnect()

            for (i in 1..disconnectAttemps) {
                if (!client.isConnected) {
                    break
                }
                Thread.sleep(TimeUnit.SECONDS.toMillis(1))
                logger.info { "Try disconnect ${client.clientId} ($i)" }
                client.disconnect()
            }

            if (client.isConnected) {
                disconnect(true)
            } else {
                client.close(force)
            }

            logger.info { "Disconnected!" }
        } catch (e: Exception) {
            logger.error(e) { "can not disconnect: ${e.message}" }
        } finally {
            // disabled because fails in all tests
//            persistence.clear()
        }
    }

    /**
     * override MqttCallback methods
     * reconnect if connection was lost
     * @param cause
     */
    override fun connectionLost(cause: Throwable) {
        logger.warn { "Connection lost, case '${cause.message}'" }
        logger.error(cause) { "Connection has not been re-established" }
    }

    /**
     * override MqttCallback methods
     * called if message has been arrived
     * run messageArrivedListenerFunc - listener of all input message
     * @param topic
     * @param message
     */
    override fun messageArrived(topic: String, message: MqttMessage) {
        logger.debug { "Message arrived $topic $message" }
        messageArrivedListener(topic, message)
    }


    /**
     * override MqttCallback methods
     * called if message has been delivered
     * run listeners for a message.
     * Because every message is unique, remove listener after calling
     * @param topic
     * @param message
     */
    override fun deliveryComplete(token: IMqttDeliveryToken) {
        messageDeliveredListenerFuncMap.remove(token)?.invoke()
    }


    /**
     * Publish a json message to a topic
     * @param topic
     * @param message
     * @param waitForCompletion if true, return only after message has been delivered
     * @param deliveryCompleteListener -> called after a message has been delivered
     * @return IMqttDeliveryToken
     */
    fun publish(
        topic: String,
        message: JsonNode,
        waitForCompletion: Boolean = false,
        deliveryCompleteListener: () -> Unit = {}
    ): IMqttDeliveryToken {

        if (!client.isConnected) {
            connect()
        }

        val mqttMessage = MqttMessage(message.toPrettyString().toByteArray())
        mqttMessage.qos = pubQoS
        mqttMessage.isRetained = false

        logger.info { "Publishing to topic \"$topic\" message: $message" }

        var token: MqttDeliveryToken? = null
        try {
            // publish message to broker
            token = client.getTopic(topic).publish(mqttMessage)

            // Wait until the message has been delivered to the broker
            if (waitForCompletion) {
                messageDeliveredListenerFuncMap[token] = deliveryCompleteListener
                token.waitForCompletion()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return token!!;
    }


    /**
     * subscribe for a topic
     * @param topic
     * @param messageArrivedListener -> called, when new message has been arrived from a topic
     * @throws MqttException
     */
    fun subscribe(topic: String, messageArrivedListener: (topic: String, message: MqttMessage) -> Unit) {
        if (!client.isConnected) {
            connect()
        }
        client.subscribe(topic, subQoS) { topic, message -> messageArrivedListener(topic, message) }
        logger.info { "subscribed to '$topic'" }
    }
}