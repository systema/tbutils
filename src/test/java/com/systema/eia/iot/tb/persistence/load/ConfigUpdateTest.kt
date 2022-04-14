package com.systema.eia.iot.tb.persistence.load

import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.persistence.TbConfigPaths
import com.systema.eia.iot.tb.persistence.remove.TB_URL
import com.systema.eia.iot.tb.persistence.search.TbFinder
import com.systema.eia.iot.tb.persistence.substituteVars
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.io.File

class ConfigUpdateTest {

    companion object {
        val path = File("src/test/resources/backup/")

        val configPaths = TbConfigPaths(path)
        val configUploader = ConfigUploader(TB_URL)
//        val rollbackSysAdmin = TbRollback(TB_URL)
        val finder = TbFinder(TB_URL)
        val restClient = ExtRestClient(TB_URL)
    }


    @Test
    fun `it should update a device with a new label`() {
        val name = "test-device-default"
        finder.device.getByName(name)?.let{ restClient.deleteDevice(it.id)}

        val device = configUploader.device.load(File(configPaths.devicesPath, "default-device.json"))

        // update the device
        val updateDevice = configUploader.device.load(File(configPaths.devicesPath, "default-device-modified.json"))

        // ensure that state has been updated
        finder.device.getByName(name)?.label shouldBe "new_label"
        device.id shouldBe updateDevice.id
    }


    @Test
//    @Ignore("does not seem to be supported by TB and fails with device with same name exists already")
    fun `it should update a device profile`() {
        val profileName = "test-profile"

        finder.deviceProfile.getByName(profileName)?.let{ profile ->
            finder.device.getAllByProfile(profileName).forEach { device ->
                restClient.deleteDevice(device.id)
            }
            restClient.deleteDeviceProfile(profile.id)
        }

        val profile = configUploader.deviceProfile.load(File(configPaths.profilesPath, "test-profile.json"))
        profile.description = "huhu"
        restClient.saveDeviceProfile(profile)

        // update the device
        val updateProfile = configUploader.deviceProfile.load(File(configPaths.profilesPath, "test-profile-modified.json"))

        // ensure that state has been updated
        finder.deviceProfile.getByName(profileName)?.description shouldBe "a great profile"
        profile.id shouldBe updateProfile.id
    }

    @Test
    fun `it should update a dashboard`() {
        val title = "Gateways"

        finder.dashboard.getByName(title)?.let { dashboard ->
            restClient.deleteDashboard(dashboard.id)
        }

       configUploader.dashboard.load(File(configPaths.dashboardsPath, "Gateways-modified.json"))

        // update the dashboard
        configUploader.dashboard.load(File(configPaths.dashboardsPath, "Gateways-modified.json"))

        // ensure that state has been updated
        finder.dashboard.getAll().filter{it.name == title}.size shouldBe 1
    }


    @Test
    fun `it should load and update a widgets bundle`() {
        val title = "Systema Widgets Test"
        finder.widgetBundle.getByTitle(title)?.let{
            restClient.deleteWidgetsBundle(it.id)
        }

        configUploader.widgetBundle.load(File(configPaths.widgetsBundlePath, "Systema_Widgets_Test.json"))
        configUploader.widgetBundle.load(File(configPaths.widgetsBundlePath, "Systema_Widgets_Test-modified.json"))

        finder.widgetBundle.getByTitle(title)?.image shouldBe "test.png"

        finder.widgetBundle.getAll().filter{it.title == title}.size shouldBe 1
    }

    @Test
    fun `it should load and update widgets`() {
        val widgetBundleTitle = "Systema_Widgets_Test.json"

        // make sure to have a bundle context
        var bundle = finder.widgetBundle.getByTitle(widgetBundleTitle)
        if (bundle == null) {
            bundle =
                configUploader.widgetBundle.load(File(configPaths.widgetsBundlePath, "Systema_Widgets_Test.json"))
        }

        val alias = "test widget"

        // cleanup
        val widget = finder.widget.getByBundleAndName(bundle, alias)
        if (widget != null) {
            restClient.deleteWidgetType(widget.id)
        }

        configUploader.widget.load(File(configPaths.widgetsPath, "test_widget.json"))

        // load the update
        configUploader.widget.configureBundle(bundle)
        configUploader.widget.load(File(configPaths.widgetsPath, "test_widget-modified.json"))

        finder.widget.getAll(bundle).size shouldBe 1
    }


    @Test
    fun `it should allow to replace parameters in template files`(){
        val input = """
        IIOt is a {{what}} technology.
        It is a lot of {{smthg}} to use.
    """.trimIndent()

        val inputFile = File.createTempFile("test", ".txt")
        inputFile.writeText(input)

        val outputFile = File.createTempFile("test", ".txt")

        val params = mapOf("{{what}}" to "cool", "{{smthg}}" to "fun")
        inputFile.substituteVars(outputFile, params)

        println(outputFile.readText())

        outputFile.readText().replace("\r", "").trim() shouldBe """
        IIOt is a cool technology.
        It is a lot of fun to use.
        """.trimIndent()
    }
}