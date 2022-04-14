package com.systema.eia.iot.tb.utils;

/**
 * ThingsBoard websocket subscription type Author AWo
 *
 * @see <a href=https://thingsboard.io/docs/user-guide/telemetry/#websocket-api>
 *      ThingsBoard websocket docs</a>
 */
public enum SubscriptionType {
    CLIENT_SCOPE,
    SERVER_SCOPE,
    SHARED_SCOPE,
    LATEST_TELEMETRY
}
