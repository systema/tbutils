package com.systema.eia.iot.tb;

import com.systema.eia.iot.tb.clients.ExtRestClient;
import com.systema.eia.iot.tb.persistence.search.TbFinder;
import org.junit.jupiter.api.BeforeEach;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.fail;

public abstract class TbTest {
    protected URL tbUrl;
    protected ExtRestClient client;
    protected TbFinder finder;

    @BeforeEach
    public void setup() {
        try {
            tbUrl = new URL(System.getenv("TB_URL"));
        } catch (MalformedURLException e) {
            try {
                tbUrl = new URL("http://" + System.getenv("TB_URL"));
            } catch (MalformedURLException e2) {
                fail("Invalid ThingsBoard URL in environment variable TB_URL: " + System.getenv("TB_URL"));
            }
        }
        System.out.println("Logging in to " + tbUrl);
        client = new ExtRestClient(tbUrl);
        finder = new TbFinder(client);
    }

    protected Device createTestDevice(String deviceName, String profileName, ExtRestClient client, TbFinder finder) {
        if (finder.getDeviceProfile().getByName(profileName) == null) {
            DeviceProfile deviceProfile = finder.getDeviceProfile().getByName("default");
            deviceProfile.setId(null);
            deviceProfile.setDefault(false);
            deviceProfile.setName(profileName);
            client.saveDeviceProfile(deviceProfile);
        }
        return client.getOrCreateDevice(deviceName, profileName);
    }
}
