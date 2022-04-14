@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.systema.eia.iot.tb.persistence.load

import com.fasterxml.jackson.databind.JsonNode
import com.systema.eia.iot.tb.clients.TbDefaults.TB_TENANT_PW
import com.systema.eia.iot.tb.clients.TbDefaults.TB_TENANT_USER
import com.systema.eia.iot.tb.persistence.search.TbFinder
import org.thingsboard.rest.client.RestClient
import org.thingsboard.server.common.data.*
import org.thingsboard.server.common.data.asset.Asset
import org.thingsboard.server.common.data.id.*
import org.thingsboard.server.common.data.relation.EntityRelation
import org.thingsboard.server.common.data.rule.RuleChain
import org.thingsboard.server.common.data.rule.RuleChainData
import org.thingsboard.server.common.data.rule.RuleChainMetaData
import org.thingsboard.server.common.data.widget.WidgetType
import org.thingsboard.server.common.data.widget.WidgetsBundle
import java.io.File
import java.net.URL
import java.util.*

/**
 * Wrapper for Rest client to provide config loading to TB
 * supported entity types: rule chain, customer, device, device profile, dashboard, widgets, widgets bound
 * @author ViB
 */
@Suppress("RedundantVisibilityModifier")
public class ConfigUploader(val restClient: RestClient) {

    constructor(
        url: URL,
        username: String = TB_TENANT_USER,
        password: String = TB_TENANT_PW
    ) : this(RestClient(url.toString())) {
        restClient.login(username, password)
    }

    val finder: TbFinder = TbFinder(restClient) // used for seeking entities in TB

    public val device = DeviceConfigurator(restClient, finder)
    public val dashboard = DashboardConfigurator(restClient)
    public val widgetBundle = WidgetBundleConfigurator(restClient)
    public val customer = CustomerConfigurator(restClient)
    public val user = UserConfigurator(restClient)
    public val tenantProfile = TenantProfileConfigurator(restClient)
    public val ruleChain = RuleChainConfigurator(restClient)
    public val widget = WidgetConfigurator(restClient)
    public val assets = AssetsConfigurator(restClient)
    public val deviceProfile = DeviceProfileConfigurator(restClient, finder)
    public val relation = RelationConfigurator(finder)

    class DeviceConfigurator internal constructor(restClient: RestClient, private val finder: TbFinder) :
        AConfigUploader<Device>(restClient) {
        override fun load(device: Device): Device = restClient.saveDevice(device)

        /**
         * TODO
         *
         * @param deviceJson
         * @return
         * @throws IllegalArgumentException if JSON processing fails
         * @throws com.fasterxml.jackson.core.JsonProcessingException – if structural JSON conversion fails
         * @throws NoSuchElementException if device profile is not stored in ThingsBoard
         */
        override fun read(deviceJson: JsonNode): Device {
            val device = SearchTextBasedWithAdditionalInfo.mapper.treeToValue(
                deviceJson,
                Device::class.java
            )
            val deviceProfileId = finder.deviceProfile.getByName(device.type)?.id
                ?: throw NoSuchElementException("device profile not found!")

            device.deviceProfileId = deviceProfileId

            val existing: Device? = TbFinder.DeviceFinder(restClient).getByName(device.name)

            // replace id in json to enable overwrite mode
            if (existing != null) {
                device.id = existing.id
            } else {
                // patch the id with the existing device
                device.id = null
            }

            return device
        }

//        override fun postProcessJson(res: ObjectNode) {
//            // test if there is a dashboard with the same name
//            val existing: Device? = TbFinder.DeviceFinder(restClient).getByName(res.get("name").textValue())
//
//            // replace id in json to enable overwrite mode
//            if (existing != null) {
//                (res.get("id") as ObjectNode).replace("id", TextNode(existing.id.id.toString()))
//            } else {
//                // patch the id with the existing device
//                res.remove("id")
//            }
//        }
    }

    class DashboardConfigurator internal constructor(restClient: RestClient) : AConfigUploader<Dashboard>(restClient) {
        override fun load(dashboard: Dashboard): Dashboard = restClient.saveDashboard(dashboard)

        override fun read(json: JsonNode): Dashboard {
            val dashboard = deserializeEntity<Dashboard>(json)

            // test if there is a dashboard with the same name
            val existing: Dashboard? = TbFinder.DashboardFinder(restClient).getByName(dashboard.name)

            // replace id in json to enable overwrite mode
            if (existing != null) {
                dashboard.id = existing.id
            } else {
                // patch the id with the existing device
                dashboard.id = null
            }

            return dashboard
        }
    }

    class WidgetBundleConfigurator internal constructor(restClient: RestClient) :
        AConfigUploader<WidgetsBundle>(restClient) {
        override fun load(widgetBundle: WidgetsBundle): WidgetsBundle {
            // test if there is a dashboard with the same name
            val existing: WidgetsBundle? =
                TbFinder.WidgetBundleFinder(restClient).getByTitle(widgetBundle.title)
            // replace id in json to enable overwrite mode
            if (existing != null) {
                widgetBundle.id = existing.id
            } else {
                widgetBundle.id = null
            }
            return restClient.saveWidgetsBundle(widgetBundle)
        }

        override fun read(json: JsonNode): WidgetsBundle {
            return deserializeEntity(json)
        }
    }

    class CustomerConfigurator internal constructor(restClient: RestClient) : AConfigUploader<Customer>(restClient) {
        override fun load(customer: Customer): Customer {
            // test if there is a dashboard with the same name
            val existingCustomer = TbFinder.CustomerFinder(restClient).getByTitle(customer.title)
            if (existingCustomer != null) {
                // replace id in json to enable overwrite mode
                customer.id = existingCustomer.id
            } else {
                customer.id = null
            }
            return restClient.saveCustomer(customer)
        }

        override fun read(json: JsonNode): Customer = deserializeEntity(json)
    }

    class UserConfigurator internal constructor(restClient: RestClient) : AConfigUploader<User>(restClient) {
        override fun load(user: User): User = restClient.saveUser(user, false);
        override fun read(json: JsonNode): User = deserializeEntity(json)
    }

    class TenantProfileConfigurator internal constructor(restClient: RestClient) :
        AConfigUploader<TenantProfile>(restClient) {
        override fun load(tenantProfile: TenantProfile): TenantProfile = restClient.saveTenantProfile(tenantProfile)
        override fun read(json: JsonNode): TenantProfile = deserializeEntity(json)
    }

    class RuleChainConfigurator internal constructor(restClient: RestClient) :
        AConfigUploader<RuleChainData>(restClient) {
        override fun load(ruleChain: RuleChainData): RuleChainData {
            restClient.importRuleChains(ruleChain, true)
            return ruleChain
        }

        override fun read(ruleChainJson: JsonNode): RuleChainData {
            val ruleChains = SearchTextBasedWithAdditionalInfo.mapper.treeToValue(
                ruleChainJson["ruleChains"],
                Array<RuleChain>::class.java
            )
            val ruleChainMetaData = SearchTextBasedWithAdditionalInfo.mapper.treeToValue(
                ruleChainJson["metadata"],
                Array<RuleChainMetaData>::class.java
            )
            val ruleChainData = RuleChainData();
            ruleChainData.metadata = ruleChainMetaData.toList()
            ruleChainData.ruleChains = ruleChains.toList()

            return ruleChainData
        }

        private fun mergeToOneRCData(list: List<RuleChainData>): RuleChainData {
            val ruleChainData = list.first()
            list.forEachIndexed { index, rc ->

                // first is saved in ruleChainData
                if (index == 0) return@forEachIndexed

                ruleChainData.ruleChains.addAll(rc.ruleChains)
                ruleChainData.metadata.addAll(rc.metadata)
            }
            return ruleChainData
        }

        fun readAllToOneRuleChainData(configDirectory: File): RuleChainData {
            return mergeToOneRCData(super.readAll(configDirectory))
        }

        override fun loadAll(configDirectory: File): List<RuleChainData> {
            val list = super.readAll(configDirectory);
            val ruleChainData = mergeToOneRCData(list)
            load(ruleChainData)
            return list
        }
    }

    class WidgetConfigurator internal constructor(restClient: RestClient) : AConfigUploader<WidgetType>(restClient) {
        override fun load(widget: WidgetType): WidgetType = restClient.saveWidgetType(widget)

        var currentBundle: WidgetsBundle? = null

        fun configureBundle(widgetBundle: WidgetsBundle?) {
            currentBundle = widgetBundle
        }

        override fun read(json: JsonNode): WidgetType {
            val widget = deserializeEntity<WidgetType>(json)

            val existing = currentBundle?.let {
                val widgetFinder = TbFinder.WidgetFinder(restClient)
                widgetFinder.getByBundleAndName(it, widget.name)
            }

            if (existing != null) {
                widget.id = existing.id
            } else {
                widget.id = null
            }

            return widget
        }
    }

    class AssetsConfigurator internal constructor(restClient: RestClient) : AConfigUploader<Asset>(restClient) {
        override fun load(asset: Asset): Asset = restClient.saveAsset(asset)
        override fun read(json: JsonNode): Asset = deserializeEntity(json)
    }

    class DeviceProfileConfigurator internal constructor(restClient: RestClient, private val finder: TbFinder) :
        AConfigUploader<DeviceProfile>(restClient) {

        fun load(deviceProfile: DeviceProfile, defaultRuleChain: RuleChain): DeviceProfile {
            deviceProfile.defaultRuleChainId = defaultRuleChain.id
            return load(deviceProfile)
        }

        override fun load(deviceProfile: DeviceProfile): DeviceProfile {
//            deviceProfile.id = null
            return restClient.saveDeviceProfile(deviceProfile)
        }

        public override fun read(node: JsonNode): DeviceProfile {
            val defaultRuleChainName = node.get("additionalConfigs").get("defaultRuleChain").asText()
            val config = node.get("config");
            val deviceProfile = SearchTextBasedWithAdditionalInfo.mapper.treeToValue(
                config,
                DeviceProfile::class.java
            )
            val defaultRuleChainId = finder.ruleChain.getByName(defaultRuleChainName)?.id
                ?: throw NullPointerException("device profile default rule chain not found!")

            deviceProfile.defaultRuleChainId = defaultRuleChainId

            val existing = TbFinder.DeviceProfileFinder(restClient).getByName(deviceProfile.name)

            // replace id in json to enable overwrite mode

            if (existing != null) {
                deviceProfile.id = existing.id
            } else {
                deviceProfile.id = null
            }


            return deviceProfile
        }
    }

    class RelationConfigurator internal constructor(private val finder: TbFinder) :
        AConfigUploader<EntityRelation>(finder.restClient) {
        override fun read(json: JsonNode): EntityRelation {
            val nameTo = json.get("additionalConfigs").get("nameTo").asText()
            val nameFrom = json.get("additionalConfigs").get("nameFrom").asText()
            val config = json.get("config");

            val entityRelation = SearchTextBasedWithAdditionalInfo.mapper.treeToValue(
                config,
                EntityRelation::class.java
            )

            val entityIdTo = finder.getEntityByTypeAndName(nameTo, entityRelation.to.entityType)
                ?: throw NullPointerException("${entityRelation.to.entityType} $nameTo not found! (entity to)")
            val entityIdFrom = finder.getEntityByTypeAndName(nameFrom, entityRelation.from.entityType)
                ?: throw NullPointerException("${entityRelation.from.entityType} $nameFrom not found! (entity from)")

            val newConfigStr = config.toString()
                .replace(entityRelation.to.id.toString(), entityIdTo.id.toString())
                .replace(entityRelation.from.id.toString(), entityIdFrom.id.toString())


            val res = SearchTextBasedWithAdditionalInfo.mapper.treeToValue(
                mapper.readTree(newConfigStr),
                EntityRelation::class.java
            )

            return res
        }

        override fun load(entity: EntityRelation): EntityRelation {
            finder.restClient.saveRelation(entity)
            return entity;
        }
    }


}
