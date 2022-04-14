package com.systema.eia.iot.tb.persistence.remove

import com.systema.eia.iot.tb.clients.TbDefaults.TB_TENANT_PW
import com.systema.eia.iot.tb.clients.TbDefaults.TB_TENANT_USER
import com.systema.eia.iot.tb.persistence.search.TbFinder
import com.systema.eia.iot.tb.utils.RelationDirection
import org.springframework.web.client.HttpClientErrorException
import org.thingsboard.rest.client.RestClient
import org.thingsboard.server.common.data.*
import org.thingsboard.server.common.data.asset.Asset
import org.thingsboard.server.common.data.id.*
import org.thingsboard.server.common.data.relation.EntityRelation
import org.thingsboard.server.common.data.relation.RelationTypeGroup
import org.thingsboard.server.common.data.rule.RuleChain
import org.thingsboard.server.common.data.widget.WidgetType
import org.thingsboard.server.common.data.widget.WidgetsBundle
import java.net.URL
import java.util.*

/**
 * Wrapper for Rest client to provide safe removing of entities
 * supported entity types: rule chain, customer, device, device profile, dashboard, widgets, widgets bound
 * @author ViB
 */
class TbRemover(val restClient: RestClient) {

    constructor(url: URL, username: String = TB_TENANT_USER, password: String = TB_TENANT_PW) : this(RestClient(url.toString())) {
        restClient.login(username, password)
    }

    // contains methods for every entity
    public val device: DeviceRemover
    public val dashboard: DashboardRemover
    public val widgetBundle: WidgetBundleRemover
    public val customer: CustomerRemover
    public val user: UserRemover
    public val tenantProfile: TenantProfileRemover
    public val ruleChain: RuleChainRemover
    public val widget: WidgetRemover
    public val assets: AssetsRemover
    public val deviceProfile: DeviceProfileRemover
    public val relation: RelationRemover

    private val finder: TbFinder


    init {
        finder = TbFinder(restClient)
        device = DeviceRemover(restClient, finder)
        dashboard = DashboardRemover(restClient, finder)
        widgetBundle = WidgetBundleRemover(restClient, finder)
        customer = CustomerRemover(restClient, finder)
        user = UserRemover(restClient, finder)
        tenantProfile = TenantProfileRemover(restClient, finder)
        widget = WidgetRemover(restClient, finder)
        assets = AssetsRemover(restClient, finder)
        deviceProfile = DeviceProfileRemover(restClient, finder, device)
        ruleChain = RuleChainRemover(restClient, finder, deviceProfile)
        relation = RelationRemover(restClient, finder)
    }

    /**
     * default removing of customer
     * supported methods: findAndRemoveIfExistByName, removeIfExistById, and findAndRemoveById
     */
    class CustomerRemover(private val restClient: RestClient, finder: TbFinder) :
        ATbRemoverFinder<Customer>(finder.customer) {
        override fun remove(id: String) = restClient.deleteCustomer(CustomerId(UUID.fromString(id)))
    }

    /**
     * default removing of users
     * supported methods: findAndRemoveIfExistByName, removeIfExistById, and findAndRemoveById
     */
    class UserRemover(private val restClient: RestClient, finder: TbFinder) : ATbRemoverFinder<User>(finder.user) {
        override fun remove(id: String) = restClient.deleteUser(UserId(UUID.fromString(id)))
    }

    /**
     * default removing of tenant profiles
     * supported methods: findAndRemoveIfExistByName, removeIfExistById, and findAndRemoveById
     */
    class TenantProfileRemover(private val restClient: RestClient, finder: TbFinder) :
        ATbRemoverFinder<TenantProfile>(finder.tenantProfile) {
        override fun remove(id: String) = restClient.deleteTenantProfile(TenantProfileId(UUID.fromString(id)))
    }

    /**
     * removing of assets
     * supported methods: findAndRemoveIfExistByName, removeIfExistById, and findAndRemoveById
     */
    class AssetsRemover(private val restClient: RestClient, private val finder: TbFinder) : ATbRemover<Asset>(),
        ITbRemoverByName<Asset> {
        override fun findById(id: String): Asset? = finder.asset.getById(id)
        override fun remove(id: String) {
            restClient.deleteAsset(AssetId.fromString(id))
        }

        override fun findAndRemoveByName(name: String): Asset? {
            val ass = finder.asset.getByName(name) ?: return null
            remove(ass.id.toString())
            return ass; }
    }

    /**
     * removing of entity relations
     * supported methods: removeAllBetween, removeAll, removeByType, removeBetweenByType, remove, removeIfExist
     */
    class RelationRemover(private val restClient: RestClient, private val finder: TbFinder) {
        fun removeAllBetween(
            entityId1: EntityId,
            entityId2: EntityId,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): List<EntityRelation> {
            val list = finder.relations.getAllBetween(entityId1, entityId2, relationTypeGroup)
            list.forEach { removeIfExist(it) }
            return list;
        }

        fun removeAll(
            entityId: EntityId,
            direction: RelationDirection,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): List<EntityRelation> {
            val list = finder.relations.getAllByEntityId(entityId, direction, relationTypeGroup)
            list.forEach { removeIfExist(it) }
            return list;
        }

        fun removeByType(
            entityId: EntityId,
            direction: RelationDirection,
            relationType: String,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): List<EntityRelation> {
            val list = finder.relations.getAllByTypeName(entityId, direction, relationType, relationTypeGroup)
            list.forEach { removeIfExist(it) }
            return list;
        }

        fun removeBetweenByType(
            entityId1: EntityId,
            entityId2: EntityId,
            relationType: String,
            relationTypeGroup: RelationTypeGroup = RelationTypeGroup.COMMON
        ): List<EntityRelation> {
            val list = finder.relations.getAllBetween(entityId1, entityId2, relationTypeGroup)
                .filter { it.type == relationType }
            list.forEach { removeIfExist(it) }
            return list;
        }

        fun remove(relation: EntityRelation) {
            restClient.deleteRelation(relation.from, relation.type, relation.typeGroup, relation.to)
        }

        fun removeIfExist(relation: EntityRelation): Boolean {
            try {
                remove(relation)
            } catch (e: HttpClientErrorException) {
                return false
            }
            return true
        }
    }

    /**
     * default removing of Widget Bundles
     * supported methods: findAndRemoveIfExistByName, removeIfExistById, findAndRemoveById, removeDuplicate
     */
    class WidgetBundleRemover(private val restClient: RestClient, finder: TbFinder) :
        ATbRemoverDuplicate<WidgetsBundle>(finder.widgetBundle) {
        override fun equalsObj(o1: WidgetsBundle, o2: WidgetsBundle) = o1.title == o2.title
        override fun remove(id: String) = restClient.deleteWidgetsBundle(WidgetsBundleId(UUID.fromString(id)))
    }


    /**
     * default removing of Widget Bundles
     * supported methods: findAndRemoveIfExistByName, removeIfExistById, findAndRemoveById, removeDuplicate
     */
    class WidgetRemover(private val restClient: RestClient, private val finder: TbFinder) : ATbRemover<WidgetType>(),
        ITbRemoverDuplicate<WidgetType> {
        override fun findById(id: String): WidgetType? = finder.widget.getById(id)
        override fun remove(id: String) = restClient.deleteWidgetType(WidgetTypeId(UUID.fromString(id)))

        /**
         * find widgets by name and bundle title and remove it
         * @param bundleTitle
         * @param widgetsName
         */
        fun findAndRemoveIfExistByName(bundleTitle: String, widgetsName: String): WidgetType? {
            val obj = finder.widget.getByBundleTitleAndName(bundleTitle, widgetsName) ?: return null
            super.removeIfExistById(obj.id.toString())
            return obj;
        }

        override fun removeDuplicateById(id: String, keepOriginal: Boolean): List<WidgetType>? {
            val obj = finder.widget.getById(id) ?: return null
            val wb = finder.widgetBundle.getAll().firstOrNull { it.alias == obj.bundleAlias } ?: return null
            val list = finder.widget.getAll(wb).filter { it.name == obj.name }.toMutableList()
            if (!keepOriginal) {
                list.add(obj)
            }
            list.forEach { super.removeIfExistById(it.id.toString()) }
            return list;
        }
    }

    /**
     * default removing of Widget Bundles
     * supported methods: findAndRemoveIfExistByName, removeIfExistById, findAndRemoveById, removeIfExistCascadeById
     */
    class DeviceProfileRemover(
        private val restClient: RestClient,
        private val _finder: TbFinder,
        private val deviceRemover: DeviceRemover
    ) : ATbRemoverFinder<DeviceProfile>(_finder.deviceProfile) {
        override fun remove(id: String) = restClient.deleteDeviceProfile(DeviceProfileId(UUID.fromString(id)))

        /**
         * removed device Profiles and all related devices
         * @return data class with removed deviceProfile and list of devices
         */
        fun removeIfExistCascadeById(id: String): RemovedDeviceProfileCascadeData? {
            val profile = super.finder.getById(id) ?: return null
            val devices = _finder.device.getAllByProfile(profile.name)
            devices.forEach { deviceRemover.removeIfExistById(it.id.toString()) }
            removeIfExistById(profile.id.toString())
            return RemovedDeviceProfileCascadeData(profile, devices)
        }

        // cascade output
        data class RemovedDeviceProfileCascadeData(val deviceProfile: DeviceProfile, val devices: List<Device>)
    }

    /**
     * default removing of Devices
     * supported methods: findAndRemoveIfExistByName, removeIfExistById, findAndRemoveById
     */
    class DeviceRemover(private val restClient: RestClient, finder: TbFinder) :
        ATbRemoverFinder<Device>(finder.device) {
        override fun remove(id: String) = restClient.deleteDevice(DeviceId(UUID.fromString(id)))
    }

    /**
     * removing of Dashboards
     * supported methods: findAndRemoveIfExistByName, removeIfExistById, findAndRemoveById,  removeDuplicateByID
     */
    class DashboardRemover(private val restClient: RestClient, finder: TbFinder) :
        ATbRemoverDuplicateHasName<Dashboard>(finder.dashboard) {
        override fun remove(id: String) = restClient.deleteDashboard(DashboardId(UUID.fromString(id)))
    }

    /**
     * default removing of Devices
     * supported methods: findAndRemoveIfExistByName, removeIfExistById, findAndRemoveById, removeDuplicateByID, removeIfExistCascade
     */
    class RuleChainRemover(
        val restClient: RestClient,
        val _finder: TbFinder,
        private val deviceProfileRemover: DeviceProfileRemover
    ) : ATbRemoverDuplicateHasName<RuleChain>(_finder.ruleChain) {
        override fun remove(id: String) = restClient.deleteRuleChain(RuleChainId(UUID.fromString(id)))

        /**
         * Remove rule chain and all related device profiles, what use it as root chain
         * By removing device profiles -> devices is also will be removed
         * @return data class with removed rule chain and list of caascade removed Device Profiles
         */
        fun removeIfExistCascade(id: String): RemovedRuleCascadeData? {

            val ruleChain = findById(id) ?: return null

            val profileCascadeData = _finder.deviceProfile.getAll()
                .filter { it.defaultRuleChainId.toString() == id }
                .mapNotNull { deviceProfileRemover.removeIfExistCascadeById(it.id.toString()) }

            removeIfExistById(ruleChain.id.toString())

            return RemovedRuleCascadeData(ruleChain, profileCascadeData)
        }

        /**
         * return data class of `removeIfExistCascade`
         * contains Rule chain and List of Removed DeviceProfile Cascade Data
         * @see DeviceProfileRemover.RemovedDeviceProfileCascadeData
         */
        data class RemovedRuleCascadeData(
            val ruleChain: RuleChain,
            val deviceProfiles: List<DeviceProfileRemover.RemovedDeviceProfileCascadeData>
        )

    }
}