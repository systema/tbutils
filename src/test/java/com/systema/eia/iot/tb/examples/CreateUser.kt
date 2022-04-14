package com.systema.eia.iot.tb.examples

import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.persistence.backup.TbBackup
import java.io.File


fun main() {
    val restClient = ExtRestClient("http://${System.getenv("TB_HOST")}:${System.getenv("TB_PORT")}")

//    TbRollback(restClient).
    TbBackup(restClient).tenantProfile.saveAll(File("test_config"))


}