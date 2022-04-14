package com.systema.eia.iot.tb.utils;

public class CommonDeviceAttributes {
    public static final DeviceAttribute active = new DeviceAttribute("active", Scope.SERVER_SCOPE);
    public static final DeviceAttribute inactivityTimeout = new DeviceAttribute("inactivityTimeout",
            Scope.SERVER_SCOPE);
}
