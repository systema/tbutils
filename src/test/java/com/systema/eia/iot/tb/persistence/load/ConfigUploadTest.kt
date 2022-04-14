package com.systema.eia.iot.tb.persistence.load

import com.systema.eia.iot.tb.clients.ExtRestClient
import com.systema.eia.iot.tb.persistence.remove.TB_URL
import com.systema.eia.iot.tb.persistence.search.TbFinder
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.thingsboard.server.common.data.relation.RelationTypeGroup
import java.io.File
import java.io.FileNotFoundException

class ConfigUploadTest {

    companion object {
        val path = File("src/test/resources/backup/")
        val pathToRuleChain = File(path, "rules")
        val pathToDashboard = File(path, "dashboards")
        val pathToDevices = File(path, "devices")
        val pathToDeviceProfiles = File(path, "profiles")
        val pathToAssets = File(path, "assets")
        val pathToCustomers = File(path, "customers")
        val pathToTenantsProfiles = File(path, "tenants")
        val pathToWidgets = File(path, "widgets")
        val pathToWidgetsBundles = File(path, "bundles")
        val pathToUser = File(path, "users")
        val pathToRelations = File(path, "relations")

        val tenantConfigurator = ConfigUploader(TB_URL)
        val sysAdminConfiguator = ConfigUploader(TB_URL)
        val finder = TbFinder(TB_URL)
        val extRestClient = ExtRestClient(TB_URL)
    }

    @Test
    fun `load rule chain`() {
        val client = tenantConfigurator.restClient;
        val ruleFileName = "Schema-Validation-Rule.json"
        val file = File(pathToRuleChain, ruleFileName);
        val name = "Schema-Validation-Rule"
        val rule = finder.ruleChain.getByName(name)
        if (rule != null) {
            client.deleteRuleChain(rule.id)
        }
        tenantConfigurator.ruleChain.load(file);
    }

    @Test
    fun `load device with default profile`() {
        val client = tenantConfigurator.restClient;
        val deviceFileName = "default-device.json"
        val file = File(pathToDevices, deviceFileName);
        val name = "test-device-default"
        val device = finder.device.getByName(name)
        if (device != null) {
            client.deleteDevice(device.id)
        }
        tenantConfigurator.device.load(file);
    }

    @Test(expected = NoSuchElementException::class)
    fun `load device with not existed profile throw exception`() {
        val client = tenantConfigurator.restClient;
        val deviceFileName = "test-profile-test-default-device.json"
        val file = File(pathToDevices, deviceFileName);
        val name = "test-default-device"
        val profileName = "test-profile"
        val device = finder.device.getByName(name)
        val profile = finder.deviceProfile.getByName(profileName)
        if (device != null) {
            client.deleteDevice(device.id)
        }
        if (profile != null) {
            client.deleteDeviceProfile(profile.id)
        }

        tenantConfigurator.device.load(file);
    }

    @Test(expected = FileNotFoundException::class)
    fun `attempting to load non-existing file causes FileNotFoundException`() {
        val file = File(path, "non-existing.file")
        tenantConfigurator.device.load(file)
    }

    @Test(expected = FileNotFoundException::class)
    fun `attempting to load from non-existing directory causes FileNotFoundException`() {
        val file = File(path, "non-existing-dir/non-existing.file")
        tenantConfigurator.device.load(file)
    }

    @Test
    fun `load device profile`() {
        val client = tenantConfigurator.restClient;
        val fileName = "test-profile.json"
        val file = File(pathToDeviceProfiles, fileName);
        val profileName = "test-profile"
        val profile = finder.deviceProfile.getByName(profileName)
        if (profile != null) {
            finder.device.getAllByProfile(profileName).forEach { device ->
                client.deleteDevice(device.id)
            }
            client.deleteDeviceProfile(profile.id)
        }

//        //`load rule chain`()
//        val id = finder.ruleChain.getByName("Schema-Validation-Rule")!!.id
//        println(id)
        tenantConfigurator.deviceProfile.load(file);
    }

    @Test
    fun `load dashboard`() {
        val client = tenantConfigurator.restClient;
        val fileName = "Gateways.json"
        val file = File(pathToDashboard, fileName);
        val title = "Gateways"
        val dashboard = finder.dashboard.getByName(title)
        if (dashboard != null) {
            client.deleteDashboard(dashboard.id)
        }
        tenantConfigurator.dashboard.load(file);
    }

    @Test
    fun `load customer`() {
        val client = tenantConfigurator.restClient;
        val fileName = "Customer_C.json"
        val file = File(pathToCustomers, fileName);
        val title = "Customer C"
        val customer = finder.customer.getByTitle(title)
        if (customer != null) {
            client.deleteCustomer(customer.id)
        }
        tenantConfigurator.customer.load(file);
    }


    @Test
    fun `load widgets bundle`() {
        val client = tenantConfigurator.restClient;
        val fileName = "Systema_Widgets_Test.json"
        val file = File(pathToWidgetsBundles, fileName);
        val title = "Systema Widgets Test"
        val bundle = finder.widgetBundle.getByTitle(title)
        if (bundle != null) {
            client.deleteWidgetsBundle(bundle.id)
        }
        tenantConfigurator.widgetBundle.load(file);
    }

    @Test
    fun `load widgets`() {
        val client = tenantConfigurator.restClient;
        val widgetBundleTitle = "Systema_Widgets_Test.json"
        var bundle = finder.widgetBundle.getByTitle(widgetBundleTitle)
        if (bundle == null) {
            bundle =
                tenantConfigurator.widgetBundle.load(File(pathToWidgetsBundles, "Systema_Widgets_Test.json"));
        }

        val fileName = "test_widget.json"
        val file = File(pathToWidgets, fileName);
        val alias = "test widget"
        val widget = finder.widget.getByBundleAndName(
            bundle,
            alias
        )// client.getWidgetType(false, bundle.alias, alias).orElse(null)
        if (widget != null) {
            client.deleteWidgetType(widget.id)
        }
        tenantConfigurator.widget.load(file);
    }

    @Test
    fun `load relations`() {

        arrayListOf(
            finder.device.getByName("test device 1"),
            finder.device.getByName("test device 2"),
            finder.device.getByName("test device 3")
        )
            .filterNotNull()
            .forEach {
                extRestClient.deleteDevice(it.id)
            }

        val d1 = extRestClient.getOrCreateDevice("test device 1")!!
        val d2 = extRestClient.getOrCreateDevice("test device 2")!!
        val d3 = extRestClient.getOrCreateDevice("test device 3")!!

        tenantConfigurator.relation.loadAll(pathToRelations)

        val e1 = extRestClient.getRelation(d1.id, "test relation", RelationTypeGroup.COMMON, d2.id).get()
        val e2 = extRestClient.getRelation(d1.id, "test relation", RelationTypeGroup.COMMON, d3.id).get()

        Assert.assertNotNull(e1)
        Assert.assertNotNull(e2)

        extRestClient.deleteDevice(d1.id)
        extRestClient.deleteDevice(d2.id)
        extRestClient.deleteDevice(d3.id)

    }

    @Ignore("ignore because its unclear where xdk-pattern.json can be found")
    @Test
    fun saveXdkProfile() {
        tenantConfigurator.ruleChain.load(File(pathToRuleChain, "xdk-patterns.json"))
        tenantConfigurator.deviceProfile.load(File(pathToDeviceProfiles, "xdk.json"))
    }

}