package com.systema.eia.iot.tb.utils;

import com.systema.eia.iot.tb.clients.ExtRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.common.data.Device;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DeviceDiscovery extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(DeviceDiscovery.class);

    private final String tbDevProfile;
    private final ExtRestClient restClient;
    private final Consumer<List<Device>> activeAction;
    private final Consumer<List<Device>> inactiveAction;

    /**
     * Create device discovery class incl a new {@link ExtRestClient}.
     *
     * @param tbUrl          - ThingsBoard URL (incl. HTTP port)
     * @param tbUser         - ThingsBoard user ID
     * @param tbPw           - ThingsBoard password
     * @param tbDevProfile   - ThingsBoard device profile name, used for filtering devices during discovery
     * @param activeAction   - function acting on discovered active devices
     * @param inactiveAction - function acting on discovered inactive devices
     */
    public DeviceDiscovery(URL tbUrl, String tbUser, String tbPw, String tbDevProfile,
            Consumer<List<Device>> activeAction, Consumer<List<Device>> inactiveAction) {
        this.tbDevProfile = tbDevProfile;
        this.activeAction = activeAction;
        this.inactiveAction = inactiveAction;
        log.info("Connecting to ThingsBoard at " + tbUrl + " with user: " + tbUser + " ...");
        restClient = new ExtRestClient(tbUrl, tbUser, tbPw);
        restClient.login(tbUser, tbPw);
        log.info("Login successful!");
    }

    /**
     * Create device discovery class using an existing {@link ExtRestClient}.
     *
     * @param client         - existing REST client; must already be connected, i.e. {@link ExtRestClient#login(String,
     *                       String)} must have been called before.
     * @param tbDevProfile   - ThingsBoard device profile name, used for filtering devices during discovery
     * @param activeAction   - function acting on discovered active devices
     * @param inactiveAction - function acting on discovered inactive devices
     */
    public DeviceDiscovery(ExtRestClient client, String tbDevProfile, Consumer<List<Device>> activeAction,
            Consumer<List<Device>> inactiveAction) {
        this.tbDevProfile = tbDevProfile;
        this.activeAction = activeAction;
        this.inactiveAction = inactiveAction;
        restClient = client;
    }

    /**
     * Query ThingsBoard for devices of the given profile. Each device that is found is checked for inactivity (see
     * {@link #isActive(Device)}) and returned as element of the active or inactive devices list, respectively.
     * <p>
     * This task is designed to run periodically, e.g. by using {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable,
     * long, long, TimeUnit)}.
     */
    @Override
    public void run() {
        List<Device> activeDevices = new LinkedList<>();
        List<Device> inactiveDevices = new LinkedList<>();
        log.debug("Querying ThingsBoard for devices of profile \"" + tbDevProfile + "\" ...");

        List<Device> deviceList = restClient.getDevicesByProfile(tbDevProfile);

        if (deviceList.isEmpty()) {
            log.debug("No devices found!");
        } else {
            for (Device device : deviceList) {
                String deviceName = device.getName();
                log.debug("Found device: " + deviceName);
                if (isActive(device)) {
                    activeDevices.add(device);
                } else {
                    inactiveDevices.add(device);
                }
            }
        }
        activeAction.accept(activeDevices);
        inactiveAction.accept(inactiveDevices);
    }

    /**
     * Close REST client connection.
     */
    public void closeTbRestClient() {
        restClient.close();
    }

    /**
     * Check, if ThingsBoard device is active (wrt. its {@code active} attribute.
     *
     * @param device - ThingsBoard device
     * @return true, if the device is currently active, false otherwise
     */
    public boolean isActive(Device device) {
        Boolean active = (Boolean) restClient.getAttribute(device.getId(), Scope.SERVER_SCOPE,
                CommonDeviceAttributes.active.name);
        return active != null && active;
    }
}
