package com.systema.iot.examples.vibration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.utils.Scope
import com.systema.eia.iot.tb.utils.SubscriptionType
import com.systema.eia.iot.tb.ws.subscribeToWS
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.thingsboard.server.common.data.Device

val log = KotlinLogging.logger {}

/**
 * Class modeling a device that vibrates more and more over time. When a certain vibration limit is exceeded for a
 * certain period of time, the device breaks (indicated  by ThingsBoard CLIENT_SCOPE attribute "broken"). The vibration
 * level may be reset by carrying out maintenance (indicated
 * by ThingsBoard SERVER_SCOPE attribute "maintenanceFinished").
 */
class DeviceSimulator(
    val device: Device, val restClient: ExtRestClient, val mqttClient: MqttClient, val frequency:
    Long = 1
) {

    var sendTelemetry: Boolean = true
        set(value) {
            log.info { "Twin continues sending telemetry: $value" }
            field = value
        }

    private val vibration = Vibration {
        restClient.saveAttribute(device.id, Scope.CLIENT_SCOPE, "broken", it)
    }

    companion object {
        val mapper = ObjectMapper()
    }

    val simulationJob: Job = GlobalScope.launch {
        while(true) {
            delay(1000 / frequency)

            if(!sendTelemetry) {
                if(mqttClient.isConnected) mqttClient.disconnect()
                continue
            }

            if(!mqttClient.isConnected) mqttClient.reconnect()

            val jsonPayload = mapper.convertValue(
                mapOf("vibration" to vibration.value),
                JsonNode::class.java
            )
            val msg = MqttMessage(jsonPayload.toString().toByteArray())
            try {
                mqttClient.publish("v1/devices/me/telemetry", msg)
            } catch(e: Throwable) {
                println(e)
            }
        }
    }

    init {
        // subscribe to SHARED_SCOPE attributes
        restClient.subscribeToWS(device.id, SubscriptionType.SHARED_SCOPE) { attrUpdates ->
            attrUpdates.forEach { attrUpdate ->
                when(attrUpdate.key) {
                    "maintenanceFinished" -> {
                        vibration.reset()
                        log.info { "Maintenance was carried out - vibration goes back to normal." }
                    }
                }
            }
        }
    }
}