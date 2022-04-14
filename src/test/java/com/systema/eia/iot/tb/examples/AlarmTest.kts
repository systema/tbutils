package com.systema.eia.iot.tb.examples

import com.systema.eia.iot.tb.clients.ExtRestClient
import org.thingsboard.server.common.data.alarm.Alarm
import org.thingsboard.server.common.data.alarm.AlarmSeverity
import org.thingsboard.server.common.data.alarm.AlarmStatus

val restClient = ExtRestClient(
    System.getenv("TB_URL"),
    System.getenv("TB_USER"),
    System.getenv("TB_PW")
)

val device = restClient.getOrCreateDevice("test_alarm_device")

val alarm = Alarm().apply {
    type = "test_alarm"
    originator = device.id
    severity = AlarmSeverity.MAJOR
    status = AlarmStatus.ACTIVE_UNACK
}

println("saving alarm")
restClient.createAlarm(alarm)


