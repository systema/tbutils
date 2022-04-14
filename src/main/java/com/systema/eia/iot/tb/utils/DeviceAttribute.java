package com.systema.eia.iot.tb.utils;

public class DeviceAttribute {
    public final String name;
    public final Scope scope;
    public DeviceAttribute(String name, Scope scope) {
        this.name = name;
        this.scope = scope;
    }

    @Override
    public String toString() {
        return name;
    }
}
