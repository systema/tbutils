package com.systema.eia.iot.tb.clients;

import com.systema.eia.iot.tb.clients.ExtRestClient;
import com.systema.eia.iot.tb.persistence.search.TbFinder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.alarm.*;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test alarm API functionality extension of {@link com.systema.eia.iot.tb.clients.ExtRestClient}
 */
public class AlarmHandlingTest {

    private static ExtRestClient newRestClient() {
        URL tbURL = null;
        try {
            tbURL = new URL(System.getenv("TB_URL"));
        } catch (MalformedURLException e) {
            try {
                tbURL = new URL("http://" + System.getenv("TB_URL"));
            } catch (MalformedURLException e2) {
                fail("Invalid ThingsBoard URL in environment variable TB_URL: " + System.getenv("TB_URL"));
            }
        }
        System.out.println("Logging in to " + tbURL);
        return new ExtRestClient(tbURL);
    }

    private static Device createTestDevice(ExtRestClient client, TbFinder finder) {
        String testDeviceProfileName = "test-device-profile";

        if (finder.getDeviceProfile().getByName(testDeviceProfileName) == null) {
            DeviceProfile deviceProfile = finder.getDeviceProfile().getByName("default");
            deviceProfile.setId(null);
            deviceProfile.setDefault(false);
            deviceProfile.setName(testDeviceProfileName);
            client.saveDeviceProfile(deviceProfile);
        }

        return client.getOrCreateDevice("AlarmTestDevice" + new Random().nextInt(1000000), testDeviceProfileName);
    }

    /**
     * Test {@link ExtRestClient#getAlarms(EntityId, AlarmSearchStatus, AlarmStatus, TimePageLink, boolean)}.
     */
    @Test
    public void getAlarmsTest() {
        ExtRestClient client = newRestClient();
        TbFinder finder = new TbFinder(client);
        Device device = createTestDevice(client, finder);

        Map<String, Alarm> alarms = new HashMap();
        String alName = "TestAlarm-1";
        alarms.put(alName, newAlarm(client, device, AlarmSeverity.MAJOR, alName, AlarmStatus.ACTIVE_UNACK));
        alName = "TestAlarm-2";
        alarms.put(alName, newAlarm(client, device, AlarmSeverity.CRITICAL, alName, AlarmStatus.CLEARED_UNACK));
        alName = "TestAlarm-3";
        alarms.put(alName, newAlarm(client, device, AlarmSeverity.MINOR, alName, AlarmStatus.CLEARED_ACK));

        // get all alarms
        Set<AlarmStatus> validStates = new HashSet<>();
        validStates.add(AlarmStatus.ACTIVE_UNACK);
        validStates.add(AlarmStatus.ACTIVE_ACK);
        validStates.add(AlarmStatus.CLEARED_ACK);
        validStates.add(AlarmStatus.CLEARED_UNACK);
        checkAlarms(client, device, alarms, AlarmSearchStatus.ANY, validStates);

        // get only unacknowledged alarms
        validStates = new HashSet<>();
        validStates.add(AlarmStatus.ACTIVE_UNACK);
        validStates.add(AlarmStatus.CLEARED_UNACK);
        checkAlarms(client, device, alarms, AlarmSearchStatus.UNACK, validStates);

        // get only cleared alarms
        validStates = new HashSet<>();
        validStates.add(AlarmStatus.CLEARED_ACK);
        validStates.add(AlarmStatus.CLEARED_UNACK);
        checkAlarms(client, device, alarms, AlarmSearchStatus.CLEARED, validStates);

        client.deleteDevice(device.getId());
    }

    private Alarm newAlarm(ExtRestClient client, Device device, AlarmSeverity severity, String type,
            AlarmStatus status) {
        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setSeverity(severity);
        alarm.setType(type);
        alarm.setStatus(status);
        client.saveAlarm(alarm);
        return alarm;
    }

    private void checkAlarms(ExtRestClient client, Device device, Map<String, Alarm> alarms,
            AlarmSearchStatus searchStatus, Set<AlarmStatus> validStates) {
        PageData<AlarmInfo> res = client.getAlarms(device.getId(), searchStatus, null, new TimePageLink(100), false);
        for (AlarmInfo al : res.getData()) {
            System.out.println("Found " + al.getName() + " (" + al.getType() + "): " + al.getStatus().toString());
            String type = al.getType();
            if (alarms.containsKey(type)) {
                assertTrue(validStates.contains(al.getStatus()),
                        () -> al.getStatus() + " is not one of the valid " + "statuses: " + validStates);
                assertEquals(alarms.get(type).getStatus(), al.getStatus());
                assertEquals(alarms.get(type).getSeverity(), al.getSeverity());
            } else {
                fail("Found unknown/invalid alarm!");
            }
        }
    }

    /**
     * Test if alarm is recognized as active based on its status.
     */
    @Test
    public void isActiveTest() {
        AlarmInfo al = new AlarmInfo();
        al.setStatus(AlarmStatus.ACTIVE_ACK);
        assertTrue(ExtRestClient.Companion.isAlarmActive(al));
        al.setStatus(AlarmStatus.ACTIVE_UNACK);
        assertTrue(ExtRestClient.Companion.isAlarmActive(al));
        al.setStatus(AlarmStatus.CLEARED_ACK);
        assertFalse(ExtRestClient.Companion.isAlarmActive(al));
        al.setStatus(AlarmStatus.CLEARED_UNACK);
        assertFalse(ExtRestClient.Companion.isAlarmActive(al));
    }

    /**
     * Argument provider method for {@link #isAnyActiveTest(List, String, AlarmInfo)}
     */
    @NotNull
    private static List<Arguments> provideAlarmLists() {
        List<Arguments> argumentsList = new LinkedList<>();
        String type = "exampleAlarmType";

        // only one active alarm of the type of interest
        List<AlarmInfo> alarms = new LinkedList<>();
        AlarmInfo al = new AlarmInfo();
        al.setStatus(AlarmStatus.ACTIVE_ACK);
        al.setType(type);
        alarms.add(al);
        AlarmInfo activeAl = new AlarmInfo(al);
        argumentsList.add(Arguments.of(alarms, type, activeAl));

        // only one active and one inactive alarm of the type of interest
        alarms = new LinkedList<>();
        al = new AlarmInfo();
        al.setStatus(AlarmStatus.CLEARED_ACK);
        al.setType(type);
        alarms.add(al);
        al = new AlarmInfo();
        al.setStatus(AlarmStatus.ACTIVE_ACK);
        al.setType(type);
        alarms.add(al);
        activeAl = new AlarmInfo(al);
        argumentsList.add(Arguments.of(alarms, type, activeAl));

        // one active and one inactive alarm of another type
        alarms = new LinkedList<>();
        al = new AlarmInfo();
        al.setStatus(AlarmStatus.CLEARED_ACK);
        al.setType(type);
        alarms.add(al);
        al = new AlarmInfo();
        al.setStatus(AlarmStatus.ACTIVE_ACK);
        al.setType(type);
        alarms.add(al);
        activeAl = new AlarmInfo(al);
        argumentsList.add(Arguments.of(alarms, "someOtherAlarmType", null));

        // one inactive alarm of the type of interest
        alarms = new LinkedList<>();
        al = new AlarmInfo();
        al.setStatus(AlarmStatus.CLEARED_UNACK);
        al.setType(type);
        alarms.add(al);
        argumentsList.add(Arguments.of(alarms, type, null));

        // one active alarm of another type and one inactive alarm of the type of interest
        alarms = new LinkedList<>();
        al = new AlarmInfo();
        al.setStatus(AlarmStatus.CLEARED_UNACK);
        al.setType(type);
        alarms.add(al);
        al = new AlarmInfo();
        al.setStatus(AlarmStatus.ACTIVE_UNACK);
        al.setType("someOtherAlarmType");
        alarms.add(al);
        argumentsList.add(Arguments.of(alarms, type, null));

        return argumentsList;
    }

    /**
     * Test, if active alarms of a given type are detected in a list of alarms.
     *
     * @param alarms   list of alarms
     * @param type     alarm type that is searched in the list
     * @param activeAl active alarm of type {@code type} that is supposed to be found in {@code alarms}
     */
    @ParameterizedTest
    @MethodSource("provideAlarmLists")
    public void isAnyActiveTest(List<AlarmInfo> alarms, String type, AlarmInfo activeAl) {
        assertEquals(activeAl != null, ExtRestClient.Companion.isAnyAlarmActive(alarms, type) != null);
    }

    /**
     * Test if a {@link ExtRestClient} is capable of telling, whether there's an active alarm of a given type for a
     * specific device, and if so, can clear this alarm.
     *
     * @param alarms   list of alarms that should be present in the device
     * @param type     alarm type of interest
     * @param activeAl active alarm among {@code alarms}
     */
    @ParameterizedTest
    @MethodSource("provideAlarmLists")
    public void clearAlarmIfActiveTest(List<AlarmInfo> alarms, String type, AlarmInfo activeAl) {
        ExtRestClient client = newRestClient();
        TbFinder finder = new TbFinder(client);
        Device device = createTestDevice(client, finder);
        for (AlarmInfo ali : alarms) {
            Alarm alarm = new Alarm(ali);
            alarm.setOriginator(device.getId());
            alarm.setSeverity(AlarmSeverity.MAJOR);
            client.saveAlarm(alarm);
        }
        if (activeAl != null)
            alarms.get(alarms.indexOf(activeAl)).setStatus(getClearedAlarmStatus(activeAl));
        client.clearAlarmIfActive(device.getId(), device.getName(), type);
        List<AlarmInfo> presentAlarms = client.getAlarms(device.getId(), AlarmSearchStatus.ANY, null, null, false)
                .getData();
        checkAlarms(alarms, presentAlarms);
        client.deleteDevice(device.getId());
    }

    /**
     * Test if a {@link ExtRestClient} is capable of telling, whether there's an active alarm of a given type for a
     * specific device, and if not, can create a new alarm of this type.
     *
     * @param alarms   list of alarms that should be present in the device
     * @param type     alarm type of interest
     * @param activeAl active alarm among {@code alarms}
     */
    @ParameterizedTest
    @MethodSource("provideAlarmLists")
    public void createAlarmIfNoneActiveTest(List<AlarmInfo> alarms, String type, AlarmInfo activeAl) {
        ExtRestClient client = newRestClient();
        TbFinder finder = new TbFinder(client);
        Device device = createTestDevice(client, finder);
        for (AlarmInfo ali : alarms) {
            Alarm alarm = new Alarm(ali);
            alarm.setOriginator(device.getId());
            alarm.setSeverity(AlarmSeverity.MAJOR);
            client.saveAlarm(alarm);
        }
        if (activeAl == null) {
            AlarmInfo alarm = new AlarmInfo();
            alarm.setType(type);
            alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
            alarms.add(alarm);
        }

        client.newAlarmIfNotActive(device.getId(), device.getName(), type, AlarmSeverity.WARNING);
        List<AlarmInfo> presentAlarms = client.getAlarms(device.getId(), AlarmSearchStatus.ANY, null, null, false)
                .getData();

        checkAlarms(alarms, presentAlarms);

        client.deleteDevice(device.getId());
    }

    /**
     * Get the status of an alarm after it has been cleared
     *
     * @param al - alarm incl. status
     * @return the status that this alarm would have after it has been cleared
     */
    private AlarmStatus getClearedAlarmStatus(AlarmInfo al) {
        if (al.getStatus().equals(AlarmStatus.ACTIVE_ACK))
            return AlarmStatus.CLEARED_ACK;
        else if (al.getStatus().equals(AlarmStatus.ACTIVE_UNACK))
            return AlarmStatus.CLEARED_UNACK;
        return al.getStatus();
    }

    /**
     * Check if two alarm lists are equal regarding type and status of the contained alarms
     *
     * @param expected 1st list of alarms
     * @param actual   2nd list of alarms
     */
    private void checkAlarms(List<AlarmInfo> expected, List<AlarmInfo> actual) {
        for (AlarmInfo aliPres : actual) {
            AlarmInfo match = null;
            for (AlarmInfo ali : expected) {
                if (ali.getType().equals(aliPres.getType()) && ali.getStatus().equals(aliPres.getStatus())) {
                    match = ali;
                    break;
                }
            }
            if (match != null) {
                expected.remove(match);
            } else {
                fail("Alarm " + aliPres + " is not supposed to be present!");
            }
        }
        if (!expected.isEmpty()) {
            StringBuffer msg = new StringBuffer("Alarm(s)\n");
            for (AlarmInfo al : expected) {
                msg.append(al + "\n");
            }
            msg.append("could not be found!");
            fail(msg.toString());
        }
    }
}
