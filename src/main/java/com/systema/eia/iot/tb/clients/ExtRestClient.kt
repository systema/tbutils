package com.systema.eia.iot.tb.clients

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.systema.eia.iot.tb.clients.InternalRestClientHelpers.*
import com.systema.eia.iot.tb.clients.TbDefaults.TB_TENANT_PW
import com.systema.eia.iot.tb.clients.TbDefaults.TB_TENANT_USER
import com.systema.eia.iot.tb.persistence.search.TbFinder
import com.systema.eia.iot.tb.utils.Scope
import mu.KotlinLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.thingsboard.rest.client.RestClient
import org.thingsboard.server.common.data.Device
import org.thingsboard.server.common.data.alarm.*
import org.thingsboard.server.common.data.id.DeviceId
import org.thingsboard.server.common.data.id.EntityId
import org.thingsboard.server.common.data.page.PageData
import org.thingsboard.server.common.data.page.TimePageLink
import org.thingsboard.server.common.data.security.DeviceCredentials
import java.net.URL

/** Some ThingsBoard defaults.*/
object TbDefaults {
    const val TB_TENANT_USER = "tenant@thingsboard.org"
    const val TB_TENANT_PW = "tenant"

    const val TB_SYSADMIN_USER = "sysadmin@thingsboard.org"
    const val TB_SYSADMIN_PW = "sysadmin"
}

/**
 * Extended Rest client
 * @author HoB, ViB
 */
open class ExtRestClient

/**
 * Create REST client and login to ThingsBoard
 *
 * @property tbURL
 * @property login
 * @property password
 * @throws RuntimeException if login fails
 */
@JvmOverloads
constructor(
    @Suppress("CanBeParameter") val tbURL: URL,
    val login: String = TB_TENANT_USER,
    val password: String = TB_TENANT_PW
) : RestClient(tbURL.toString()) {


    /**
     * Create REST client and login to ThingsBoard
     *
     * @property tbURL
     * @property login
     * @property password
     * @throws RuntimeException if login fails
     */
    @JvmOverloads
    constructor(
        tbURL: String,
        login: String = TB_TENANT_USER,
        password: String = TB_TENANT_PW,
    ) : this(URL(tbURL), login, password) {
    }


    val finder = TbFinder(this)

    val tbHost = tbURL.host
    val tbPort = tbURL.port

    init {
        // Creating new rest client and auth with credentials
        try {
            login(login, password)
        } catch (e: NullPointerException) {
            throw RuntimeException(
                "Failed to login with user $login and password ${
                    password.subSequence(0, 1)
                }${
                    password.subSequence(1, password.length).replace(
                        Regex("."), "*"
                    )
                }\"", e
            )
        }
    }

    companion object {
        private val mapper = ObjectMapper()

        // default device type
        var DEFAULT_DEVICE_PROFILE_NAME: String = "default"

        val log = KotlinLogging.logger {}

        /**
         * Check, if there's any active alarm of the given type among the given alarms.
         *
         * @param alarms alarms to be checked
         * @param type   alarm type to be looked for
         * @return the first active alarm of the given type that was found, null if there's no such alarm
         * @see .isActive
         */
        @JvmStatic
        open fun isAnyAlarmActive(alarms: List<AlarmInfo>?, type: String): AlarmInfo? {
            if (alarms == null) {
                log.warn("Searching for alarms failed!")
                return null
            }
            for (al in alarms) {
                if (type == al.type && isAlarmActive(al)) {
                    return al
                }
            }
            return null
        }

        /**
         * Check whether an alarm is in an active state, i.e. ACTIVE_ACK or ACTIVE_UNACK.
         *
         * @param al alarm to be checked
         * @return true, if alarm is not `null` and in one of the above mentioned states, false otherwise
         */
        @JvmStatic
        open fun isAlarmActive(al: AlarmInfo?): Boolean {
            return al != null && (AlarmStatus.ACTIVE_ACK == al.status || (AlarmStatus.ACTIVE_UNACK
                    == al.status))
        }
    }

    /**
     * send telemetry to device
     * @param node
     */
    fun sendTelemetry(deviceId: DeviceId, node: JsonNode) {
        saveEntityTelemetry(deviceId, Scope.CLIENT_SCOPE.name, node)
    }


//    private fun getUrlParams(pageLink: PageLink): String? {
//        var urlParams = "pageSize={pageSize}&page={page}"
//        if (!StringUtils.isEmpty(pageLink.textSearch)) {
//            urlParams += "&textSearch={textSearch}"
//        }
//        if (pageLink.sortOrder != null) {
//            urlParams += "&sortProperty={sortProperty}&sortOrder={sortOrder}"
//        }
//        return urlParams
//    }
//
//
//    private fun addPageLinkToParam(params: MutableMap<String, String>, pageLink: PageLink) {
//        params["pageSize"] = pageLink.pageSize.toString()
//        params["page"] = pageLink.page.toString()
//        if (!StringUtils.isEmpty(pageLink.textSearch)) {
//            params["textSearch"] = pageLink.textSearch
//        }
//        if (pageLink.sortOrder != null) {
//            params["sortProperty"] = pageLink.sortOrder.property
//            params["sortOrder"] = pageLink.sortOrder.direction.name
//        }
//    }

    /**
     * send telemetry to device
     * @param map
     */
    fun sendTelemetry(deviceId: DeviceId, map: Map<String, Any?>) {
        sendTelemetry(deviceId, mapper.convertValue(map, JsonNode::class.java))
    }

    /**
     * send telemetry to device
     * @param map
     */
    fun sendTelemetry(deviceId: DeviceId, name: String, value: Any) {
        val jsonNode = mapper.convertValue(mapOf(name to value), JsonNode::class.java)

        sendTelemetry(deviceId, jsonNode)
    }

    /**
     * Get attribute of device
     * @param scope attribute scope
     * @param name attribute name
     */
    fun getAttribute(deviceId: DeviceId, scope: Scope, name: String): Any? {
        val attributesByScope = getAttributesByScope(deviceId, scope.toString(), listOf(name))
        return attributesByScope.firstOrNull { it.key == name }?.value
    }

    /**
     * Set attribute to device
     * @param node attributes as json node
     * @param scope attribute scope
     */
    fun saveAttribute(deviceId: DeviceId, scope: Scope, name: String, value: Any) {
        val jsonNode = mapper.convertValue(mapOf(name to value), JsonNode::class.java)

        when (scope) {
            Scope.CLIENT_SCOPE -> saveClientAttributes(deviceId, jsonNode)
            else -> saveAttributes(deviceId, jsonNode, scope)
        }
    }

    /**
     * Send attribute update to this device.
     * @param deviceId  ThingsBoard device ID
     * @param node      attributes as json node (keys: attribute names, values: new attribute values) (see
     *                      [ThingsBoard REST API docs](https://thingsboard.io/docs/reference/http-api/#attributes-api))
     * @param scope     attribute scope
     */
    fun saveAttributes(deviceId: DeviceId, node: JsonNode, scope: Scope) {
        saveDeviceAttributes(deviceId, scope.name, node)
    }

    /**
     * Send attribute update to this device.
     * @param deviceId  ThingsBoard device ID
     * @param map       attribute update payload (keys: attribute names, values: new attribute values) (see
     *                  [ThingsBoard REST API docs](https://thingsboard.io/docs/reference/http-api/#attributes-api))
     * @param scope     attribute scope
     */
    fun saveAttributes(deviceId: DeviceId, map: Map<String, Any?>, scope: Scope) {
        saveAttributes(deviceId, mapper.convertValue(map, JsonNode::class.java), scope)
    }

    /**
     * save client attribute for a device
     * @param deviceId
     * @param request
     * @throws NullPointerException if device not found
     */
    fun saveClientAttributes(deviceId: DeviceId, request: JsonNode?): Boolean {
        val token =
            getDeviceTokenByDeviceId(deviceId)
                ?: throw NullPointerException("device '$deviceId' not found")

        return restTemplate.postForEntity(
            "$baseURL/api/v1/{ACCESS_TOKEN}/attributes",
            request,
            Any::class.java,
            token
        )
            .statusCode
            .is2xxSuccessful
    }

//    fun changePassword(oldPass: String, newPass: String) {
//        return restTemplate.postForEntity(
//                        "$baseURL/api/auth/changePassword",
//                        request,
//                        Any::class.java,
//                        token)
//                .statusCode
//                .is2xxSuccessful
//    }


    /**
     * create a device if device exist, but has some other profile, then update profile if this
     * device already exist -> return it
     * @param deviceName
     * @param deviceProfileName
     * @return device
     */
    fun getOrCreateDevice(
        deviceName: String,
        deviceProfileName: String = DEFAULT_DEVICE_PROFILE_NAME
    ): Device {
        val actualDevice: Device? = finder.device.getByName(deviceName)
        if (actualDevice?.type == deviceProfileName) {
            return actualDevice
        } else if (actualDevice != null) {
            // if device exist in Tb, but has some other type
//            deleteDevice(actualDevice.id)

            // validate existence of device profile
            require(finder.deviceProfile.getByName(deviceProfileName) != null) {
                "Device profile '$deviceProfileName' does not exist"
            }

            actualDevice.type = deviceProfileName
            saveDevice(actualDevice)

            return actualDevice
        }

        // create a new device
        val newDevice = Device()
        newDevice.name = deviceName
        newDevice.type = deviceProfileName
        return saveDevice(newDevice)
    }

    /**
     * Check for attributes. This method will be blocking until an attribute changes is recorded.
     * @param deviceId
     * @param timeout
     * @throws NullPointerException if device not found
     */
    @Deprecated(
        "We do not want to use this method, but rather subscribe tto attribute changes via WebSocket"
    )
    fun waitForAttributeChanges(deviceId: DeviceId, timeout: Int = 20000): Map<String, Any> {
        val token =
            getDeviceTokenByDeviceId(deviceId)
                ?: throw NullPointerException("device '$deviceId' not found")

        //        restTemplate.requestFactory
        val template = RestTemplate(SimpleClientHttpRequestFactory())

        (template.requestFactory as SimpleClientHttpRequestFactory).setConnectTimeout(timeout)
        (template.requestFactory as SimpleClientHttpRequestFactory).setReadTimeout(timeout)

        val changedAttributes: JsonNode? =
            template.exchange(
                // http(s)://host:port/api/v1/$ACCESS_TOKEN/attributes/updates
                "$baseURL/api/v1/{token}/attributes/updates?timeout=$timeout",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                JsonNode::class.java,
                token
            )
                .body

        return mapper.convertValue(changedAttributes, object : TypeReference<Map<String, Any>>() {})
        //        return RestJsonConverter.toAttributes(listOf(changedAttributes))
    }

    /**
     * get Device Credentials By Device Name
     * @param name
     * @return null if not found
     */
    fun getDeviceCredentialsByDeviceName(name: String): DeviceCredentials? {
        val device = finder.device.getByName(name) ?: return null
        return getDeviceCredentialsByDeviceId(device.id).orElse(null)
    }

    /**
     * get Device Token By Device Id
     * @param deviceId
     * @return null if a device not found
     */
    fun getDeviceTokenByDeviceId(deviceId: DeviceId): String? {
        val credentials = getDeviceCredentialsByDeviceId(deviceId).orElse(null) ?: return null
        return credentials.credentialsId
    }

    /**
     * get Device Token By Device Name
     * @param name
     * @return null if a device not found
     */
    fun getDeviceTokenByDeviceName(name: String): String? {
        val credentials = getDeviceCredentialsByDeviceName(name) ?: return null
        return credentials.credentialsId
    }

    /**
     * Get all devices of a given profile
     *
     * @param profileName ThingsBoard device profile name
     * @return list of all devices of the given profile
     * @see TbFinder.DeviceFinder.getAllByProfile(String)
     */
    fun getDevicesByProfile(profileName: String): List<Device> {
        return finder.device.getAllByProfile(profileName)
    }


    /**
     * Modified version of [RestClient.getAlarms]. It has the same behavior as the original method, apart from:
     * - the generation of the HTTP request URL has been fixed (see
     * [RestClient GitHub issue](https://github.com/thingsboard/thingsboard/issues/2628#issuecomment-838647013)),
     * - added null-safety for pageLink parameter
     */
    override fun getAlarms(
        entityId: EntityId,
        searchStatus: AlarmSearchStatus?, status: AlarmStatus?, pageLink: TimePageLink?, fetchOriginator: Boolean
    ): PageData<AlarmInfo>? {
        var urlSecondPart = "/api/alarm/{entityType}/{entityId}?fetchOriginator={fetchOriginator}"
        val params: MutableMap<String, String> = HashMap()

        params["entityType"] = entityId.entityType.name
        params["entityId"] = entityId.id.toString()
        params["fetchOriginator"] = fetchOriginator.toString()

        if (searchStatus != null) {
            params["searchStatus"] = searchStatus.name
            urlSecondPart += "&searchStatus={searchStatus}"
        }

        if (status != null) {
            params["status"] = status.name
            urlSecondPart += "&status={status}"
        }

        addTimePageLinkToParam(params, pageLink ?: TimePageLink(100))

        return restTemplate
            .exchange<PageData<AlarmInfo>>(
                baseURL + urlSecondPart + "&" + getTimeUrlParams(pageLink ?: TimePageLink(100)),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                params
            ).body
    }

    /**
     * Create a new alarm of the given type for this client's device, if there's not already an ACTIVE one present.
     *
     * @param entityId ID of the entity for which to search for alarms
     * @param entityName human-readable name of the entity for which to search for alarms (e.g. device name); used
     * for logging
     * @param type alarm type
     * @param severity severity of the new alarm to be created
     */
    open fun newAlarmIfNotActive(entityId: EntityId, entityName: String, type: String, severity: AlarmSeverity) {
        val res: PageData<AlarmInfo>? = getAlarms(entityId, AlarmSearchStatus.ACTIVE, null, TimePageLink(100), false)
        if (isAnyAlarmActive(res?.data, type) != null) {
            log.debug {
                "There is already an active alarm of type: $type for entity $entityName- not creating " +
                        "a new one."
            }
            return
        }
        log.debug { "There is no active alarm of type: $type for entity $entityName - creating a new one." }
        val alarm = Alarm()
        alarm.type = type
        alarm.originator = entityId
        alarm.severity = severity
        alarm.status = AlarmStatus.ACTIVE_UNACK
        saveAlarm(alarm)
    }

    /**
     * Clear an alarm of the given type for this client's device, if there's an ACTIVE one.
     *
     * @param entityId ID of the entity for which to search for alarms
     * @param type alarm type
     * @param entityName human-readable name of the entity for which to search for alarms (e.g. device name), used
     * for logging
     */
    open fun clearAlarmIfActive(entityId: EntityId, entityName: String, type: String) {
        val res: PageData<AlarmInfo>? = getAlarms(entityId, AlarmSearchStatus.ACTIVE, null, TimePageLink(100), false)
        val activeAlarm = isAnyAlarmActive(res?.data, type)
        if (activeAlarm != null) {
            log.debug { "There is an active alarm of type: $type for entity $entityName - clearing it." }
            clearAlarm(activeAlarm.id)
            return
        }
        log.debug { "There is no active alarm of type: $type for entity $entityName - nothing to clear." }
    }
}
