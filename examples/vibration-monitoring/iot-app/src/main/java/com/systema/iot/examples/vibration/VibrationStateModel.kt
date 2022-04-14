package com.systema.iot.examples.vibration

import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.utils.SubscriptionType
import com.systema.eia.iot.tb.ws.saveAttributeChanges
import com.systema.eia.iot.tb.ws.subscribeToWS
import fsmDefinition
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.thingsboard.server.common.data.Device

class VibrationDeviceStateModel(val device: Device, val client: ExtRestClient) {
    val logger = KotlinLogging.logger {}

    // TODO Make configurable via TB
    val MAX_VIBRATION = 40.0

    // TODO Do we want to rather use a moving averae as state-model?
//    val vibrationAvg = MovingAverage(10)

    val fsm  = fsmDefinition.create(this)

    // configure attribute and telemetry subscriptions
    init {
        // enable attribute persistence
        // Note: this is just needed for dash-boarding and not for functioning
        client.saveAttributeChanges(device.id)

        // subscribe to telemetry using web socket
        logger.info { "subscribing to websocket for telemetry updates..." }


        client.subscribeToWS(
            device.id,
            SubscriptionType.LATEST_TELEMETRY,
            keys = listOf(VibrationDeviceAttributes.VIBRATION_TELEMETRY)
        ) { message ->
            message.forEach {
                require(it.key == VibrationDeviceAttributes.VIBRATION_TELEMETRY)
                logger.trace("new vibration measurement ${it.value}")
                val vibMeasurement = it.value.toString().toDouble()

                if(vibMeasurement > MAX_VIBRATION){
                    runBlocking {
                        if(fsm.currentState==ToolState.Productive) {
                            fsm.sendEvent(ToolEvent.MaxVibrationExceeded)
                        }
                    }
                }
            }
        }

        // subscribe to attribute changes
        val broken = VibrationDeviceAttributes.broken
        client.subscribeToWS(device.id, broken.scope, keys = listOf(broken.name)) { message ->
            message.forEach {
                logger.warn("tool broken status changed to ${it.value}")
                val isBroken = it.value.toString().toBoolean()

                // todo change logic to also transition state if isBroken==false
                if(isBroken) {
                    runBlocking {
                        if(fsm.currentState== ToolState.Productive) {
                            fsm.sendEvent(ToolEvent.UnscheduledTooldown)
                        }
                    }
                }
            }
        }
    }

    fun repairCompleted() {
        with(VibrationDeviceAttributes.maintenanceCompleted){
            client.saveAttribute(device.id, attributeScope, name, true )
        }

        //
//        client.saveAttribute(device.id, VibrationDeviceAttributes.maintenanceCompleted, true )

        client.saveAttribute(
            device.id,
            VibrationDeviceAttributes.maintenanceCompleted.attributeScope,
            VibrationDeviceAttributes.maintenanceCompleted.name,
            true
        )

        runBlocking {
            fsm.sendEvent(ToolEvent.MaintenanceComplete)
        }
    }

    fun logStatus() {
        with(VibrationDeviceAttributes.status){
            client.saveAttribute(device.id, attributeScope, name, fsm.currentState )
            // todo remove this temporary hack, once we have a proper state-timeline display
            client.saveAttribute(device.id, attributeScope, name+"_ordinal", fsm.currentState.ordinal.toDouble() )
        }
    }
}
