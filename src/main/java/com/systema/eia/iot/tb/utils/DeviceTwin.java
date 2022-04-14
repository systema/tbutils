package com.systema.eia.iot.tb.utils;

import com.systema.eia.iot.tb.ws.AttrUpdate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.systema.eia.iot.tb.ws.AttributeHistoryKt.isAttrTelemetryKey;

/**
 * Class holding a local copy of the twin of a ThingsBoard device. This involves attribute values as well as the latest
 * telemetry values.
 * <p>
 * All read and write accesses to the attributes object are thread-safe. This is achieved by using read and write locks
 * of an unfair {@link ReentrantReadWriteLock}. Read access is allowed in parallel, but cannot be interrupted by
 * writing. Write access is guaranteed to happen with exclusive access (no other reader and writer at the same time).
 * See {@link ReentrantReadWriteLock} JavaDocs for behavior details.
 *
 * @author wogawa
 */
public class DeviceTwin {

    private static final String CLASS_NAME = DeviceTwin.class.getSimpleName();

    private Map<SubscriptionType, Map<String, Object>> attributes = new HashMap<>();
    private final Logger log = LoggerFactory.getLogger(CLASS_NAME);
    public final Device tbDevice;
    // read and write locks
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock rLock = rwLock.readLock();
    private final Lock wLock = rwLock.writeLock();

    /**
     * Create twin object and store device object. This will give the opportunity to retrieve device name and ID from
     * the twin later (see {@link #getName()} and {@link #getId()}).
     *
     * @param tbDevice - ThingsBoard device.
     */
    public DeviceTwin(@NotNull Device tbDevice) {
        this.tbDevice = Objects.requireNonNull(tbDevice, "Device must not be null!");
    }

    /**
     * Get device twin holding current device attribute values. Since the scopes may involve "LATEST_TELEMETRY" (see
     * {@link SubscriptionType}), the twin can also store the latest value of each telemetry parameter.
     *
     * @return map holding the current attribute values, format: {@code {scope1: {attr1: "value", attr2: 1}, scope2:
     * {attr3: "false"}}}
     */
    public Map<SubscriptionType, Map<String, Object>> getAll() {
        rLock.lock();
        try {
            return attributes;

        } finally {
            rLock.unlock();
        }
    }

    /**
     * Set the complete attributes map for this client. This method is only used for testing purposes.
     *
     * @param attributes - attributes map (format see {@link #getAll()})
     */
    public void setAttributes(Map<SubscriptionType, Map<String, Object>> attributes) {
        wLock.lock();
        try {
            this.attributes = attributes;
        } finally {
            wLock.unlock();
        }
    }

    /**
     * Get all attributes in a given scope.
     *
     * @param scope - attribute scope
     * @return map containing attribute names (keys) and values, if the scope is present in twin, null otherwise
     */
    public Map<String, Object> get(SubscriptionType scope) {
        rLock.lock();
        try {
            return attributes.get(scope);
        } finally {
            rLock.unlock();
        }
    }

    /**
     * Get attribute value inside a given scope.
     *
     * @param scope - attribute scope
     * @param key   - attribute name
     * @return attribute value, if the attribute is present under the given scope, null otherwise
     */
    public Object get(SubscriptionType scope, String key) {
        rLock.lock();
        try {
            return attributes.get(scope) != null ? attributes.get(scope).get(key) : null;
        } finally {
            rLock.unlock();
        }
    }

    /**
     * Save attribute values in device twin.
     *
     * @param update - attribute scope, format: see {@link #getUpdateDiff(Map)}
     */
    public void update(@NotNull Map<SubscriptionType, Map<String, Object>> update) {
        if (update == null) {
            log.warn("Invalid argument - update: " + update + " - ignoring update!");
            return;
        }
        for (SubscriptionType scope : update.keySet())
            update(scope, update.get(scope));
    }

    /**
     * Save attribute values in device twin.
     *
     * @param scope  - attribute scope
     * @param values - attribute names (keys) and values
     */
    public void update(@NotNull SubscriptionType scope, @NotNull Map<String, Object> values) {
        if (values == null || scope == null) {
            log.warn("Invalid arguments - scope: " + scope + ", values: " + values + " - ignoring update!");
            return;
        }
        for (String key : values.keySet())
            update(scope, key, values.get(key));
    }

    /**
     * Save attribute in device twin under a given scope.
     *
     * @param scope - attribute scope
     * @param key   - attribute name
     * @param value - attribute value
     */
    public void update(@NotNull SubscriptionType scope, @NotNull String key, Object value) {
        if (scope == null || key == null) {
            log.warn("Invalid arguments - scope: " + scope + ", key: " + key + " - ignoring update!");
            return;
        }
        //noinspection StatementWithEmptyBody
        if (isAttrTelemetryKey(key)) {
            // ignored
            log.debug("Ignoring device twin telemetry history attribute - scope: " + scope + ", key: " + key + ", " +
                 "value: " + value);
            return;
        }
        wLock.lock();
        try {
            if (attributes.containsKey(scope)) {
                if (attributes.get(scope).containsKey(key)) {
                    log.debug("Updating device twin attribute - scope: " + scope + ", key: " + key + ", value: " + value);
                } else {
                    log.debug("Storing new device twin attribute - scope: " + scope + ", key: " + key + ", value: " + value);
                }
            } else {
                log.debug("Creating new device twin scope: " + scope);
                attributes.put(scope, new HashMap<>());
            }
            attributes.get(scope).put(key, value);
        } finally {
            wLock.unlock();
        }
    }

    /**
     * Get ThingsBoard device name.
     */
    public String getName() {
        return tbDevice.getName();
    }

    /**
     * Get ThingsBoard device id.
     *
     * @return device id, if twin has been constructed via {@link #DeviceTwin(Device)}, "unknown" otherwise
     */
    public DeviceId getId() {
        return tbDevice.getId();
    }

    /**
     * Update one attribute in one scope of the local device twin (for the format of the device twin, see {@link
     * #getAll()}). If the scope or the attribute in this scope has not been present in the twin by now, a new scope /
     * attribute is created in the twin. Additionally, this method logs what happens to the twin because of this update.<br>
     * <br>
     * This method is supposed to be called as soon as an attribute update is received from ThingsBoard. By calling this
     * method, only the local copy of the device twin is updated. Usual way of updating the ThingsBoard device twin:
     * send update --> remote twin is updated --> receive the update via websocket --> {@link #update(SubscriptionType,
     * AttrUpdate)} to store the update in the local twin.
     *
     * @param scope  twin scope which the attribute update belongs to
     * @param update object containing the attribute name ({@link AttrUpdate#key}) and new value ({@link
     *               AttrUpdate#value})
     */
    public void update(@NotNull SubscriptionType scope, @NotNull AttrUpdate update) {
        if (scope == null || update == null) {
            log.warn("Invalid arguments - scope: " + scope + ", update: " + update + " - ignoring update!");
            return;
        }
        update(scope, update.key, update.value);
    }

    /**
     * Compute the difference between a twin update and the current twin state. That means determine all parts of the
     * update that are different compared to the current twin state.
     *
     * @param scope          twin scope
     * @param update         twin update, format see {@link #get(SubscriptionType)}
     * @param ignoreTwinNull if set to {@code true}, only values might be different that are present and not {@code
     *                       null} in the twin; otherwise any value is considered different, if it is {@code null} or
     *                       not present in the twin xor the update
     * @return map containing only the values that are new compared to the current twin state, format see {@link
     * #getAll()}; if no value is new, an empty map is returned
     */
    public Map<String, Object> getUpdateDiff(SubscriptionType scope, Map<String, Object> update,
            @NotNull boolean ignoreTwinNull) {
        Map<String, Object> updateDiff = new HashMap<>();
        if (update == null) {
            return updateDiff;
        }
        rLock.lock();
        try {
            Map<String, Object> twinScope = attributes.get(scope);
            if (twinScope == null) {
                log.debug("Scope " + scope + " not present in twin: " + update);
                // twin doesn't have this scope at all --> accept all updates for this scope
                if (ignoreTwinNull)
                    return updateDiff;
                else
                    return update;
            } else {
                // twin has this scope --> check, if it has each attribute
                for (String attrKey : update.keySet()) {
                    Object twinVal = twinScope.get(attrKey);
                    Object updateVal = update.get(attrKey);
                    log.trace(
                            "Comparing attribute \"" + attrKey + "\" - twin: " + twinVal + ", update: " + updateVal);
                    if ((!ignoreTwinNull && (!twinScope.containsKey(attrKey) || areDifferent(twinVal,
                            updateVal)) || (ignoreTwinNull && areDifferentIgnoreTwinNull(twinVal, updateVal)))) {
                        log.trace("Attribute not equal");
                        // current twin attribute value is different from update value
                        // --> accept update for this attribute
                        updateDiff.put(attrKey, updateVal);
                    }
                }
            }
            return updateDiff;
        } finally {
            rLock.unlock();
        }
    }

    /**
     * Compute the difference between a twin update and the current twin state. That means determine all parts of the
     * update that are different compared to the current twin state.
     *
     * @param scope          twin scope
     * @param key            update attribute key
     * @param value          update attribute value
     * @param ignoreTwinNull if set to {@code true}, only values might be different that are present and not {@code
     *                       null} in the twin; otherwise any value is considered different, if it is {@code null} or
     *                       not present in the twin xor the update
     * @return {@code true}, if the update value is different from the current twin value, {@code false} otherwise
     */
    public boolean getUpdateDiff(SubscriptionType scope, @NotNull String key, Object value,
            @NotNull boolean ignoreTwinNull) {
        return !getUpdateDiff(scope, Map.of(key, value == null ? "null" : value), ignoreTwinNull).isEmpty();
    }

    /**
     * Compute the difference between a twin update and the current twin state. That means determine all parts of the
     * update that are different or new (not present) compared to the current twin state.
     *
     * @param update - twin update, format see {@link #getAll()}
     * @return map containing only the values that are new compared to the current twin state, format see {@link
     * #getAll()}; if no value is new, an empty map is returned
     */
    public Map<SubscriptionType, Map<String, Object>> getUpdateDiff(
            @NotNull Map<SubscriptionType, Map<String, Object>> update) {
        return getUpdateDiff(update, false);
    }

    /**
     * Compute the difference between a twin update and the current twin state. That means determine all parts of the
     * update that are different compared to the current twin state.
     *
     * @param update         twin update, format see {@link #getAll()}
     * @param ignoreTwinNull if set to {@code true}, only values might be different that are present and not {@code
     *                       null} in the twin; otherwise any value is considered different, if it is {@code null} or
     *                       not present in the twin xor the update
     * @return map containing only the values that are new compared to the current twin state, format see {@link
     * #getAll()}; if no value is new, an empty map is returned
     */
    public Map<SubscriptionType, Map<String, Object>> getUpdateDiff(Map<SubscriptionType, Map<String, Object>> update,
            @NotNull boolean ignoreTwinNull) {
        Map<SubscriptionType, Map<String, Object>> updateDiff = new HashMap<>();
        if (update == null) {
            return updateDiff;
        }
        rLock.lock();
        try {
            for (SubscriptionType scope : update.keySet()) {
                Map<String, Object> scopeUpdateDiff = getUpdateDiff(scope, update.get(scope), ignoreTwinNull);
                if (scopeUpdateDiff != null)
                    updateDiff.put(scope, scopeUpdateDiff);
            }
            return updateDiff;
        } finally {
            rLock.unlock();
        }
    }

    /**
     * Determine whether an update value is different from the current twin value. Two values are considered NOT
     * different, if one of the following conditions holds:
     * <ul>
     *     <li>Both values are null</li>
     *     <li>The two values are equal as per {@link Object#equals(Object)}</li>
     *     <li>The String representations of the two values are equal (see {@code toString()} method). </li>
     * </ul>
     * The latter condition has been added, since ThingsBoard may convert non-String values, such as Booleans, to
     * Strings. When receiving an update of a Boolean attribute, {@link Object#equals(Object)} would always tell that
     * the intended update (type Boolean) is different from the value stored in the twin (type String), even if both
     * are {@code true}.
     *
     * @param twinVal   - attribute value stored in the twin
     * @param updateVal - intended update value
     * @return true, if both values are different, false otherwise
     */
    private boolean areDifferent(Object twinVal, Object updateVal) {
        if (twinVal == null && updateVal == null)
            return false;
        if (twinVal == null || updateVal == null)
            return true;
        if (twinVal.equals(updateVal))
            return false;
        return !twinVal.toString().equals(updateVal.toString());
    }

    /**
     * Determine whether an update value is different from the current twin value. Two values are considered NOT
     * different, if one of the following conditions holds:
     * <ul>
     *     <li>The twin value is null</li>
     *     <li>The two values are equal as per {@link Object#equals(Object)}</li>
     *     <li>The String representations of the two values are equal (see {@code toString()} method). </li>
     * </ul>
     * The latter condition has been added, since ThingsBoard may convert non-String values, such as Booleans, to
     * Strings. When receiving an update of a Boolean attribute, {@link Object#equals(Object)} would always tell that
     * the intended update (type Boolean) is different from the value stored in the twin (type String), even if both
     * are {@code true}.
     *
     * @param twinVal   - attribute value stored in the twin
     * @param updateVal - intended update value
     * @return true, if both values are different, false otherwise
     */
    private boolean areDifferentIgnoreTwinNull(Object twinVal, Object updateVal) {
        if (twinVal == null)
            return false;
        if (twinVal.equals(updateVal))
            return false;
        return !twinVal.toString().equals(updateVal.toString());
    }

    @Override
    public String toString() {
        rLock.lock();
        try {
            return attributes.toString();
        } finally {
            rLock.unlock();
        }
    }

}