package com.systema.eia.iot.tb

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.systema.eia.iot.tb.clients.TbDefaults

object EnvVars {
    const val tbHost = "TB_HOST"
    const val tbRestPort = "TB_REST_PORT"
    const val tbMqttPort = "TB_MQTT_PORT"
    const val tbUser = "TB_USER"
    const val tbUrl = "TB_URL"
    const val tbPassword = "TB_PW"
    const val tbDeviceName = "DEVICE_NAME"
    const val tbDeviceProfile = "DEVICE_PROFILE"
}

abstract class TbCliApp : CliktCommand() {

    val tbHost by option(
        "-h",
        "--host",
        help = "ThingsBoard hostname. Alternative env var: ${EnvVars.tbHost}."
    ).default(System.getenv(EnvVars.tbHost) ?: "thingsboard")

    val tbRestPort by option(
        "-r",
        "--rest-port",
        help = "ThingsBoard REST port. Alternative env var: ${EnvVars.tbRestPort}."
    ).default(System.getenv(EnvVars.tbRestPort) ?: "9090")

    val tbUrl by option(
        "-t",
        "--tb-url",
        help = "ThingsBoard URL. Alternative env vars: ${EnvVars.tbUrl} or http://\$${EnvVars.tbHost}:\$${
            EnvVars
                .tbRestPort
        }."
    ).default(
        System.getenv(EnvVars.tbUrl) ?: "http://${System.getenv(EnvVars.tbHost)}:${System.getenv(EnvVars.tbRestPort)}"
    )

    val tbMqttPort by option(
        "-m",
        "--mqtt-port",
        help = "ThingsBoard MQTT port. Alternative env var: ${EnvVars.tbMqttPort}."
    ).default(System.getenv(EnvVars.tbMqttPort) ?: "1883")

    val tbUser by option(
        "-u",
        "--tb-user",
        help = "ThingsBoard application user. Alternative env var: ${EnvVars.tbUser}."
    ).default(System.getenv(EnvVars.tbUser) ?: TbDefaults.TB_TENANT_USER)

    val tbPassword by option(
        "-p",
        "--tb-password",
        help = "ThingsBoard application user password. Alternative env var: ${EnvVars.tbPassword}."
    ).default(System.getenv(EnvVars.tbPassword) ?: TbDefaults.TB_TENANT_PW)

    val tbDeviceName by option(
        "-d",
        "--device",
        help = "The name of the TB device. Alternative env var: " +
                "${EnvVars.tbDeviceName}."
    ).default(System.getenv(EnvVars.tbDeviceName) ?: "")

    val tbDeviceProfile by option(
        "--profile",
        help = "The name of the TB device profile. Alternative env var: ${EnvVars.tbDeviceProfile}."
    ).default(System.getenv(EnvVars.tbDeviceProfile) ?: "")

    fun printArgs() {
        println("Starting the application with these values:")
        println("tbHost: $tbHost")
        println("tbRestPort: $tbRestPort")
        println("tbUrl: $tbUrl")
        println("tbMqttPort: $tbMqttPort")
        println("tbUser: $tbUser")
        println(
            "tbPassword: ${
                tbPassword.subSequence(0, 1)
            }${
                tbPassword.subSequence(1, tbPassword.length).replace(
                    Regex("."), "*"
                )
            }"
        )
        println("tbDeviceName: $tbDeviceName")
        println("tbDeviceProfile: $tbDeviceProfile")
    }
}