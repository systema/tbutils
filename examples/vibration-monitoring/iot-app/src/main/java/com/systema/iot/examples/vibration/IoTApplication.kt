@file:Suppress("MemberVisibilityCanBePrivate")

package com.systema.iot.examples.vibration

import com.google.gson.Gson
import com.systema.eia.iot.tb.TbCliApp
import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.persistence.load.UserConfigurator
import com.systema.eia.iot.tb.utils.DeviceDiscovery
import com.systema.eia.iot.tb.utils.Scope
import com.systema.eia.iot.tb.utils.SubscriptionType
import com.systema.iot.examples.vibration.TbConfigurator.TBUTILS_DEMO_SYSADMIN_PW
import com.systema.iot.examples.vibration.TbConfigurator.TBUTILS_DEMO_TENANT_PW
import mu.KotlinLogging
import org.thingsboard.server.common.data.Device
import org.thingsboard.server.common.data.id.DeviceId
import java.io.File
import java.net.URL
import java.util.*


//private val OBJECT_MAPPER = ObjectMapper()
//fun JSONObject.asJsonNode(): JsonNode = OBJECT_MAPPER.readTree(toString())

fun main(args: Array<String>) = IoTApplication().main(args)


// TODO include in tbutils? or use json-schema instead?
data class DeviceAttribute(val name: String, val scope: SubscriptionType) {
    // TODO this looks ugly
    val attributeScope = when (scope) {
        SubscriptionType.CLIENT_SCOPE -> Scope.CLIENT_SCOPE
        SubscriptionType.SERVER_SCOPE -> Scope.SERVER_SCOPE
        SubscriptionType.SHARED_SCOPE -> Scope.SHARED_SCOPE
        SubscriptionType.LATEST_TELEMETRY -> TODO("not supported")
    }
}
//data class DeviceTelemetry(val name: String)

// TODO share code with edge-device?
/** Centralize all device attributes names and scopes. */
class VibrationDeviceAttributes {
    companion object {
        val maintenanceCompleted = DeviceAttribute("maintenanceFinished", SubscriptionType.SHARED_SCOPE)
        val broken = DeviceAttribute("maintenanceFinished", SubscriptionType.SHARED_SCOPE)
        val status = DeviceAttribute("status", SubscriptionType.SHARED_SCOPE)

        const val VIBRATION_TELEMETRY = "vibration"
    }
}

object JsonTest {
    @JvmStatic
    fun main(args: Array<String>) {
//        val OBJECT_MAPPER = ObjectMapper()
//        fun JSONObject.asJsonNode(): JsonNode = OBJECT_MAPPER.readTree(toString())
        val gson = Gson()

        print(gson.toJson(VibrationDeviceAttributes))

    }
}

/**
 * Process input telemetry from a boch device
 */
class IoTApplication : TbCliApp() {
    val logger = KotlinLogging.logger {}

    init {
        System.setProperty("org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY", "INFO");
    }

    /**
     * run listener and processing
     */
    override fun run() {

        printArgs()

        // Configure thingsboard application user (first run only)
        val userConfigurator = UserConfigurator(URL(tbUrl))
        userConfigurator.updateDefaultUserPasswords(TBUTILS_DEMO_SYSADMIN_PW, TBUTILS_DEMO_TENANT_PW)

        // Configure thingsboard application user (first run only)
        userConfigurator.createNewApplicationUser(tbUser, tbPassword, "SYSTEMA")

        // Synchronize entity definition changes into the TB instance
        TbConfigurator(
            URL(tbUrl),
            tbUser,
            tbPassword,
            File("tbconfig")
        ).updateConfiguration()

        // helper class to group iot app components per device
        class DeviceComponents(client: ExtRestClient, stateMachine: VibrationDeviceStateModel) {
            var client: ExtRestClient
            var stateMachine: VibrationDeviceStateModel

            init {
                this.client = client
                this.stateMachine = stateMachine
            }
        }

        val deviceComponents = HashMap<DeviceId, DeviceComponents>()

        // functions to handle discovered devices
        val activeAction =
            { discoveredActiveDevices: List<Device> ->
                for (device in discoveredActiveDevices) {
                    if (!deviceComponents.containsKey(device.id)) {
                        logger.info { "Found new active device ${device.name}." }
                        logger.info { "Initializing new ThingsBoard client for device ${device.name}..." }
                        val client = ExtRestClient(URL(tbUrl), tbUser, tbPassword)
                        logger.info { "Initializing new state machine for device ${device.name}..." }
                        val stateMachine = VibrationDeviceStateModel(device, client)
                        // save device components so that they can be cleared, when device goes inactive
                        deviceComponents[device.id] = DeviceComponents(client, stateMachine)
                    }
                }
            }
        val inactiveAction =
            { discoveredInactiveDevices: List<Device> ->
                for (device in discoveredInactiveDevices) {
                    if (deviceComponents.containsKey(device.id)) {
                        logger.info { "Found new inactive device ${device.name}." }
                        logger.info { "Disconnecting ThingsBoard client of device ${device.name}..." }
                        deviceComponents[device.id]?.client?.close()
                        logger.info { "Removing state machine of device ${device.name}..." }
                        deviceComponents.remove(device.id)
                    }
                }
            }

        // start discovery scheduled to run every 5s
        // TODO@HoB: can this be rewritten using trailing lambdas notation?
        val discoveryTask =
            DeviceDiscovery(URL(tbUrl), tbUser, tbPassword, tbDeviceProfile, activeAction, inactiveAction)
        val timer = Timer("deviceDiscoveryTimer")
        timer.schedule(discoveryTask, 0, 5000)

        // create file for Docker HEALTHCHECK
        File("UP").createNewFile()
    }
}
