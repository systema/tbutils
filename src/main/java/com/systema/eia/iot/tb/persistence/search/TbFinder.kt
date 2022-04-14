@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package com.systema.eia.iot.tb.persistence.search

import com.systema.eia.iot.tb.clients.TbDefaults.TB_TENANT_PW
import com.systema.eia.iot.tb.clients.TbDefaults.TB_TENANT_USER
import com.systema.eia.iot.tb.utils.RelationDirection
import org.thingsboard.rest.client.RestClient
import org.thingsboard.server.common.data.*
import org.thingsboard.server.common.data.asset.Asset
import org.thingsboard.server.common.data.id.*
import org.thingsboard.server.common.data.page.PageLink
import org.thingsboard.server.common.data.relation.EntityRelation
import org.thingsboard.server.common.data.relation.RelationTypeGroup
import org.thingsboard.server.common.data.rule.RuleChain
import org.thingsboard.server.common.data.rule.RuleChainMetaData
import org.thingsboard.server.common.data.widget.WidgetType
import org.thingsboard.server.common.data.widget.WidgetsBundle
import java.net.URL
import java.util.*

/**
 * Wrapper for Rest client to provide seeking of entities
 * supported entity types: rule chain, customer, device, device profile, dashboard, widgets, widgets bound
 * @author ViB
 */
class TbFinder(val restClient: RestClient) {

    constructor(url: URL, username: String = TB_TENANT_USER, password: String = TB_TENANT_PW) : this(RestClient(url.toString())) {
        restClient.login(username, password)
    }

    companion object {
        private val PAGE_LINK_SIZE = 10000;
    }

    public val device: DeviceFinder
    public val dashboard: DashboardFinder
    public val widgetBundle: WidgetBundleFinder
    public val customer: CustomerFinder
    public val user: UserFinder
    public val tenantProfile: TenantProfileFinder
    public val ruleChain: RuleChainFinder
    public val ruleChainData: RuleChainDataFinder
    public val widget: WidgetFinder
    public val asset: AssetFinder
    public val deviceProfile: DeviceProfileFinder
    public val relations: RelationsFinder

    init {
        device = DeviceFinder(restClient)
        dashboard = DashboardFinder(restClient)
        widgetBundle = WidgetBundleFinder(restClient)
        customer = CustomerFinder(restClient)
        user = UserFinder(restClient)
        tenantProfile = TenantProfileFinder(restClient)
        ruleChain = RuleChainFinder(restClient)
        ruleChainData = RuleChainDataFinder(restClient)
        widget = WidgetFinder(restClient)
        asset = AssetFinder(restClient)
        deviceProfile = DeviceProfileFinder(restClient)
        relations = RelationsFinder(restClient)
    }


    fun getEntityIdByIdAndType(entityId: String, entityType: EntityType): EntityId {
        return EntityIdFactory.getByTypeAndId(entityType.name, entityId)
    }

    fun getEntityByTypeAndName(entityName: String, entityType: EntityType): HasId<*>? {
        return when (entityType) {
            EntityType.ALARM -> {
                throw NotImplementedError()
            }
            EntityType.API_USAGE_STATE -> {
                throw NotImplementedError()
            }
            EntityType.ASSET -> {
                asset.getByName(entityName)
            }
            EntityType.CUSTOMER -> {
                customer.getByTitle(entityName)
            }
            EntityType.TENANT -> {
                tenantProfile.getByName(entityName)
            }
            EntityType.USER -> {
                user.getByName(entityName)
            }
            EntityType.DASHBOARD -> {
                dashboard.getByName(entityName)
            }
            EntityType.DEVICE -> {
                device.getByName(entityName)
            }
            EntityType.RULE_CHAIN -> {
                ruleChain.getByName(entityName)
            }
            EntityType.RULE_NODE -> {
                throw NotImplementedError()
            }
            EntityType.ENTITY_VIEW -> {
                throw NotImplementedError()
            }
            EntityType.DEVICE_PROFILE -> {
                deviceProfile.getByName(entityName)
            }
            else -> {
                throw NotImplementedError()
            }
        }
    }

    fun getEntityById(id: EntityId): HasId<*>? {
        return when (id.entityType) {
            EntityType.ALARM -> {
                throw NotImplementedError()
            }
            EntityType.API_USAGE_STATE -> {
                throw NotImplementedError()
            }
            EntityType.ASSET -> {
                asset.getById(id.toString())
            }
            EntityType.CUSTOMER -> {
                customer.getById(id.toString())
            }
            EntityType.TENANT -> {
                tenantProfile.getById(id.toString())
            }
            EntityType.USER -> {
                user.getById(id.toString())
            }
            EntityType.DASHBOARD -> {
                dashboard.getById(id.toString())
            }
            EntityType.DEVICE -> {
                device.getById(id.toString())
            }
            EntityType.RULE_CHAIN -> {
                ruleChain.getById(id.toString())
            }
            EntityType.RULE_NODE -> {
                throw NotImplementedError()
            }
            EntityType.ENTITY_VIEW -> {
                throw NotImplementedError()
            }
            EntityType.DEVICE_PROFILE -> {
                deviceProfile.getById(id.toString())
            }
            else -> {
                throw NotImplementedError()
            }
        }
    }

    class DeviceFinder internal constructor(private val restClient: RestClient) : ATbFinder<Device> {
        override fun getById(id: String): Device? = restClient.getDeviceById(DeviceId(UUID.fromString(id))).orElse(null)
        override fun getByName(name: String): Device? = restClient.getTenantDevice(name).orElse(null)

        override fun getAll(): List<Device> {
            val list = mutableListOf<Device>()
            val profiles = restClient.getTenantDevices(PageLink(PAGE_LINK_SIZE)).data;
            profiles.forEach {
                list.addAll(restClient.getTenantDevices(it.name, PageLink(PAGE_LINK_SIZE)).data)
            }
            return list;
        }

        public fun getAllByProfile(profileName: String): List<Device> {
            // validate presence of device profile
            //getTenantDevices returns device profiles =)
            val profiles = restClient.getTenantDevices(PageLink(PAGE_LINK_SIZE)).data.map { it.name };
            assert(profiles.contains(profileName)) { "device profile '$profileName' not found!" }

            return restClient.getTenantDevices(profileName, PageLink(PAGE_LINK_SIZE)).data.toList()
        }

    }

    class DashboardFinder internal constructor(private val restClient: RestClient) : ATbFinder<Dashboard> {
        override fun getById(id: String): Dashboard? =
            restClient.getDashboardById(DashboardId(UUID.fromString(id))).orElse(null)

        override fun getByName(name: String): Dashboard? {
            val dashboardInfo =
                restClient.getTenantDashboards(PageLink(PAGE_LINK_SIZE)).data.findLast { it.name == name }
                    ?: return null
            return restClient.getDashboardById(dashboardInfo.id).get()
        }

        override fun getAll(): List<Dashboard> =
            restClient.getTenantDashboards(PageLink(PAGE_LINK_SIZE)).data.map { Dashboard(it) }
    }

    class WidgetBundleFinder internal constructor(private val restClient: RestClient) : ATbFinder<WidgetsBundle> {
        public override fun getById(id: String) =
            restClient.getWidgetsBundleById(WidgetsBundleId(UUID.fromString(id))).orElse(null)

        fun getByTitle(widgetsBundleTitle: String): WidgetsBundle? {
            return restClient.getWidgetsBundles(PageLink(PageLink(PAGE_LINK_SIZE))).data
                .findLast { it.title == widgetsBundleTitle }
        }

        override fun getAll() = restClient.getWidgetsBundles(PageLink(PAGE_LINK_SIZE)).data
        override fun getByName(widgetsBundleTitle: String): WidgetsBundle? = getByTitle(widgetsBundleTitle)
    }

    class CustomerFinder internal constructor(private val restClient: RestClient) : ATbFinder<Customer> {
        override fun getByName(name: String) =
            restClient.getCustomers(PageLink(PAGE_LINK_SIZE)).data.findLast { it.name == name }

        fun getByTitle(name: String) =
            restClient.getCustomers(PageLink(PAGE_LINK_SIZE)).data.findLast { it.title == name }

        override fun getAll() = restClient.getCustomers(PageLink(PAGE_LINK_SIZE)).data
        override fun getById(id: String): Customer? =
            restClient.getCustomerById(CustomerId(UUID.fromString(id))).orElse(null)
    }

    class UserFinder internal constructor(private val restClient: RestClient) : ATbFinder<User> {

        override fun getById(id: String): User? = restClient.getUserById(UserId(UUID.fromString(id))).orElse(null)
        override fun getByName(name: String): User? =
            restClient.getUsers(PageLink(PAGE_LINK_SIZE)).data.findLast { it.name == name }

        override fun getAll(): List<User> = restClient.getUsers(PageLink(PAGE_LINK_SIZE)).data
        fun getByFirstAndLastName(firstName: String, lastName: String): User? = restClient.getUsers(
            PageLink(
                PAGE_LINK_SIZE
            )
        ).data.findLast { it.firstName == firstName && it.lastName == lastName }

    }

    class TenantProfileFinder internal constructor(private val restClient: RestClient) : ATbFinder<TenantProfile> {
        override fun getByName(name: String) =
            restClient.getTenantProfiles(PageLink(PAGE_LINK_SIZE)).data.findLast { it.name == name }

        override fun getAll() = restClient.getTenantProfiles(PageLink(PAGE_LINK_SIZE)).data
        override fun getById(id: String): TenantProfile? =
            restClient.getTenantProfileById(TenantProfileId(UUID.fromString(id))).orElse(null)
    }

    class RuleChainDataFinder internal constructor(private val restClient: RestClient) :
        ATbFinder<Pair<RuleChain, RuleChainMetaData>> {

        override fun getById(id: String): Pair<RuleChain, RuleChainMetaData>? {
            return getAll().findLast { it.first.id.toString() == id }
        }


        override fun getByName(name: String): Pair<RuleChain, RuleChainMetaData>? {
            val export = restClient.exportRuleChains(PAGE_LINK_SIZE);
            val ruleChain = export.ruleChains.findLast { it.name == name } ?: return null
            val metadata = export.metadata.findLast { it.ruleChainId == ruleChain.id }!!
            return Pair(ruleChain, metadata)
        }

        public override fun getAll(): List<Pair<RuleChain, RuleChainMetaData>> {
            val export = restClient.exportRuleChains(PAGE_LINK_SIZE);
            return export.ruleChains.associateWith { ruleChain -> export.metadata.findLast { it.ruleChainId == ruleChain!!.id }!! }
                .toList()
        }

    }

    class RuleChainFinder internal constructor(private val restClient: RestClient) : ATbFinder<RuleChain> {
        override fun getById(id: String): RuleChain? =
            restClient.getRuleChainById(RuleChainId(UUID.fromString(id))).orElse(null)

        override fun getByName(name: String): RuleChain? = getAll().findLast { it.name == name }
        override fun getAll(): List<RuleChain> =
            restClient.getRuleChains(PageLink(PAGE_LINK_SIZE)).data.map { RuleChain(it) }
    }

    class WidgetFinder internal constructor(private val restClient: RestClient) {
        fun getById(id: String): WidgetType? =
            restClient.getWidgetTypeById(WidgetTypeId(UUID.fromString(id))).orElse(null)

        public fun getByBundleAndName(
            bundle: WidgetsBundle,
            widgetName: String,
            isSystem: Boolean = false
        ): WidgetType? {

            return restClient.getBundleWidgetTypes(isSystem, bundle.alias).findLast { it.name == widgetName }
        }

        public fun getByBundleTitleAndName(
            bundleTitle: String,
            widgetName: String,
            isSystem: Boolean = false
        ): WidgetType? {
            val wb = restClient.getWidgetsBundles(PageLink(PAGE_LINK_SIZE)).data.findLast { it.title == bundleTitle }
                ?: return null
            return restClient.getBundleWidgetTypes(isSystem, wb.alias).findLast { it.name == widgetName }
        }

        public fun getAll(widgetsBundle: WidgetsBundle, isSystem: Boolean = false) =
            restClient.getBundleWidgetTypes(isSystem, widgetsBundle.alias)
    }

    class AssetFinder internal constructor(private val restClient: RestClient) {
        fun getById(id: String): Asset? = restClient.getAssetById(AssetId.fromString(id)).orElse(null)
        fun getByName(name: String): Asset? = restClient.getTenantAsset(name).orElse(null)
        fun getAllByType(type: String) = restClient.getTenantAssetInfos(PageLink(PAGE_LINK_SIZE), type).data
    }

    class DeviceProfileFinder internal constructor(private val restClient: RestClient) : ATbFinder<DeviceProfile> {
        override fun getById(id: String): DeviceProfile? =
            restClient.getDeviceProfileById(DeviceProfileId.fromString(id)).orElse(null)

        override fun getByName(name: String): DeviceProfile? = restClient.getTenantDevices(
            PageLink(
                PAGE_LINK_SIZE
            )
        ).data.findLast { it.name == name }

        override fun getAll(): List<DeviceProfile> = restClient.getTenantDevices(PageLink(PAGE_LINK_SIZE)).data

    }

    class RelationsFinder internal constructor(private val restClient: RestClient) {


        fun getAllByEntityId(
            entityId: EntityId,
            direction: RelationDirection,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): List<EntityRelation> {

            val list = mutableListOf<EntityRelation>()
            if (direction.isTo()) {
                list.addAll(restClient.findByTo(entityId, relationTypeGroup))
            }
            if (direction.isFrom()) {
                list.addAll(restClient.findByFrom(entityId, relationTypeGroup))
            }
            return list
        }


//        fun getAllRelations(entityIdFrom: EntityId, entityIdTo: EntityId, relationType: String, relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON){
//            return restClient.getRelation(entityIdFrom, relationType, relationTypeGroup, entityIdTo)
//        }

        fun getAllByTypeName(
            entityId: EntityId,
            direction: RelationDirection,
            relationType: String,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): MutableList<EntityRelation> {
            val list = mutableListOf<EntityRelation>()

            if (direction.isTo()) {
                list.addAll(restClient.findByTo(entityId, relationType, relationTypeGroup))
            }
            if (direction.isFrom()) {
                list.addAll(restClient.findByFrom(entityId, relationType, relationTypeGroup))
            }

            return list
        }


        fun getAllBetween(
            entityId1: EntityId,
            entityId2: EntityId,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): List<EntityRelation> {
            val from = getAllByEntityId(entityId1, RelationDirection.FROM, relationTypeGroup)
                .filter { it.to.id == entityId2.id }
                .toMutableList()
            val to = getAllByEntityId(entityId2, RelationDirection.FROM, relationTypeGroup)
                .filter { it.to.id == entityId1.id }
                .toMutableList()

            from.addAll(to)
            return from
        }

        fun get(
            sourceEntityId: EntityId,
            targetEntityId: EntityId,
            direction: RelationDirection,
            relationType: String,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): EntityRelation? {
            return if (direction == RelationDirection.FROM) {
                restClient.getRelation(sourceEntityId, relationType, relationTypeGroup, targetEntityId).orElse(null)
            } else if (direction == RelationDirection.TO) {
                restClient.getRelation(targetEntityId, relationType, relationTypeGroup, sourceEntityId).orElse(null)
            } else {
                throw IllegalArgumentException("Can not process BOTH direction in one time")
            }
        }

        fun getTo(
            sourceEntityId: EntityId,
            targetEntityId2: EntityId,
            relationType: String,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): EntityRelation? {
            return get(sourceEntityId, targetEntityId2, RelationDirection.TO, relationType, relationTypeGroup)
        }

        fun getFrom(
            sourceEntityId: EntityId,
            targetEntityId2: EntityId,
            relationType: String,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): EntityRelation? {
            return get(sourceEntityId, targetEntityId2, RelationDirection.FROM, relationType, relationTypeGroup)
        }

        fun getBoth(
            sourceEntityId: EntityId,
            targetEntityId2: EntityId,
            relationType: String,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): List<EntityRelation>? {
            val r1 = get(sourceEntityId, targetEntityId2, RelationDirection.FROM, relationType, relationTypeGroup)
            val r2 = get(sourceEntityId, targetEntityId2, RelationDirection.TO, relationType, relationTypeGroup)
            if (r1 == null || r2 == null) return null

            return listOf(r1, r2)
        }


    }
}