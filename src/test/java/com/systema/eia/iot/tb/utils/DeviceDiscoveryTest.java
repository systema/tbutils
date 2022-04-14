package com.systema.eia.iot.tb.utils;

import com.systema.eia.iot.tb.TbTest;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.Device;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.systema.eia.iot.tb.utils.CommonDeviceAttributes.active;
import static com.systema.eia.iot.tb.utils.CommonDeviceAttributes.inactivityTimeout;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

public class DeviceDiscoveryTest extends TbTest {
    private final List<String> activeDevices = new LinkedList<>();
    private final List<String> inactiveDevices = new LinkedList<>();
    private final String deviceProfile = "discoveryTestProfile" + new Random().nextInt(10000);
    private final String deviceBaseName = "discoveryTestDevice";

    private void createDevice(String name, String profile, boolean activeFlag) {
        var device = createTestDevice(name, profile, client, finder);
        client.saveAttribute(device.getId(), inactivityTimeout.scope, inactivityTimeout.name, 60000);
        client.saveAttribute(device.getId(), active.scope, active.name, activeFlag);

        var msg = "Created ";
        if (profile.equals(deviceProfile) && activeFlag) {
            activeDevices.add(device.getName());
            msg += "active ";
        } else if (profile.equals(deviceProfile)) {
            inactiveDevices.add(device.getName());
            msg += "inactive ";
        } else
            msg += "ignored ";
        System.err.println(msg + "device " + device.getName() + " of profile " + profile);
    }

    private String newRandomDeviceName() {
        return deviceBaseName + new Random().nextInt(1000000);
    }

    /**
     * Test whether device discovery finds devices of a given profile, ignores other devices, and groups discovered
     * devices into active and inactive ones.
     */
    @Test
    public void discoveryTest() {
        // create devices in TB
        for (int i = 0; i < 3; i++) {
            // create active device
            createDevice(newRandomDeviceName(), deviceProfile, true);
            // create inactive device
            createDevice(newRandomDeviceName(), deviceProfile, false);
        }
        createDevice(newRandomDeviceName(), "someOtherProfile", true);
        createDevice(newRandomDeviceName(), "yetAnotherProfile", false);

        var executor = Executors.newScheduledThreadPool(2);

        System.err.println("Active devices to be discovered: " + activeDevices);
        System.err.println("Inactive devices to be discovered: " + inactiveDevices);

        // create discovery results consumer
        var activeAction = new Consumer<List<Device>>() {
            @Override
            public void accept(List<Device> discoveredActiveDevices) {
                for (Device dev : discoveredActiveDevices) {
                    var d = dev.getName();
                    System.err.println("Found active device " + d);
                    if (!activeDevices.remove(d)) {
                        System.err.println("Actual device profile: " + dev.getType());
                        System.err.println("Expected device profile: " + deviceProfile);
                        fail("Wrong profile - device shouldn't have been found!");
                    }
                }
                System.err.println("Remaining active devices " + activeDevices);
            }
        };
        var inactiveAction = new Consumer<List<Device>>() {
            @Override
            public void accept(List<Device> discoveredInactiveDevices) {
                for (Device dev : discoveredInactiveDevices) {
                    var d = dev.getName();
                    System.err.println("Found inactive device " + d);
                    if (!inactiveDevices.remove(d)) {
                        System.err.println("Actual device profile: " + dev.getType());
                        System.err.println("Expected device profile: " + deviceProfile);
                        fail("Wrong profile - device shouldn't have been found!");
                    }
                }
                System.err.println("Remaining inactive devices " + inactiveDevices);
            }
        };

        // start discovery
        var discoveryTask = new DeviceDiscovery(client, deviceProfile, activeAction, inactiveAction);
        executor.scheduleAtFixedRate(discoveryTask, 0, 2, TimeUnit.SECONDS);

        await().atMost(30, TimeUnit.SECONDS).until(() -> activeDevices.isEmpty() && inactiveDevices.isEmpty());
    }
}
