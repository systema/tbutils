package com.systema.iot.examples.vibration

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.systema.eia.iot.tb.EnvVars
import com.systema.eia.iot.tb.TbCliApp
import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.clients.TbDefaults
import mu.KotlinLogging
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence
import org.thingsboard.server.common.data.Device
import java.net.URL
import kotlin.random.Random

fun main(args: Array<String>) = DeviceSimulationApp().main(args)

object AdditionalEnvVars {
    val frequency = "SAMPLE_FREQ"
}

class DeviceSimulationApp : TbCliApp() {

    val frequency by option(
        "-f",
        "--sample-frequency",
        help = "Number of vibration samples per second to send. Alternative env var: ${AdditionalEnvVars.frequency}."
    ).default(System.getenv(AdditionalEnvVars.frequency) ?: "1")

    val log = KotlinLogging.logger {}

    override fun run() {
        printArgs()

        val restClient = ExtRestClient(URL(tbUrl), tbUser, tbPassword)
        log.info("Provisioning simulation device to broker")
        val testDevice: Device = restClient.getOrCreateDevice(tbDeviceName + Random.nextInt(1000000), tbDeviceProfile)
        log.info { "Provisioned test device ${testDevice.name}. Starting operation..." }

        val token = restClient.getDeviceTokenByDeviceId(testDevice.id)!!

        val persistence = MqttDefaultFilePersistence(System.getProperty("user.dir") + "/paho-tcp")
        val mqttUrl = "tcp://${tbHost}:${tbMqttPort}"
        val mqttClient = MqttClient(mqttUrl, MqttClient.generateClientId(), persistence)
        val connOpts = MqttConnectOptions().apply {
            isCleanSession = true

            userName = token

            // for reconnect behavior see javadoc
            isAutomaticReconnect = true
        }
        log.info { "Connecting to TB MQTT at $mqttUrl with opts $connOpts" }
        mqttClient.connect(connOpts)
        require(mqttClient.isConnected) { "could not connect to mqtt broker on $mqttUrl with token '$token'" }

        DeviceSimulator(testDevice, restClient, mqttClient, frequency.toLong())
    }
}