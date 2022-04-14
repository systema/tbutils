package com.systema.eia.iot.tb.utils;

import com.systema.eia.iot.tb.utils.DeviceTwin;
import com.systema.eia.iot.tb.utils.SubscriptionType;
import com.systema.eia.iot.tb.ws.AttrUpdate;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.Device;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeviceTwinTest {

    /**
     * Test {@link DeviceTwin#getUpdateDiff(Map)} method.
     */
    @Test public void getUpdateDiffTest() {
        DeviceTwin twin = new DeviceTwin(new Device());

        // initialize twin
        Map<String, Object> twinClientScope = new HashMap<>();
        twinClientScope.put("attr1", "val1");
        twinClientScope.put("attr2", 2);
        twinClientScope.put("attr3", false);
        twin.update(SubscriptionType.CLIENT_SCOPE, twinClientScope);
        Map<String, Object> twinServerScope = new HashMap<>();
        twinServerScope.put("attr4", "val4");
        twinServerScope.put("attr5", null);
        twin.update(SubscriptionType.SERVER_SCOPE, twinServerScope);
        System.out.println("Twin state:\n" + twin);

        // update twin
        Map<SubscriptionType, Map<String, Object>> update = new HashMap<>();
        Map<String, Object> updateClientScope = new HashMap<>();
        updateClientScope.put("attr1", "val1");
        updateClientScope.put("attr2", 3);
        updateClientScope.put("attr3", false);
        update.put(SubscriptionType.CLIENT_SCOPE, updateClientScope);
        Map<String, Object> updateServerScope = new HashMap<>();
        updateServerScope.put("attr4", "val5");
        updateServerScope.put("attr5", 1.1);
        updateServerScope.put("attr6", 6);
        updateServerScope.put("attr7", null);
        update.put(SubscriptionType.SERVER_SCOPE, updateServerScope);
        Map<String, Object> updateSharedScope = new HashMap<>();
        updateSharedScope.put("attr8", "val8");
        updateSharedScope.put("attr9", null);
        update.put(SubscriptionType.SHARED_SCOPE, updateSharedScope);
        Map<SubscriptionType, Map<String, Object>> updateDiff = twin.getUpdateDiff(update);
        System.out.println("Actual diff:\n" + updateDiff);

        // expected diff between update and twin state
        Map<SubscriptionType, Map<String, Object>> updateDiffExpected = new HashMap<>();
        Map<String, Object> updateDiffClientScope = new HashMap<>();
        //        updateDiffClientScope.put("attr1", "val1");
        updateDiffClientScope.put("attr2", 3);
        //        updateDiffClientScope.put("attr3", false);
        updateDiffExpected.put(SubscriptionType.CLIENT_SCOPE, updateDiffClientScope);
        Map<String, Object> updateDiffServerScope = new HashMap<>();
        updateDiffServerScope.put("attr4", "val5");
        updateDiffServerScope.put("attr5", 1.1);
        updateDiffServerScope.put("attr6", 6);
        updateDiffServerScope.put("attr7", null);
        updateDiffExpected.put(SubscriptionType.SERVER_SCOPE, updateDiffServerScope);
        Map<String, Object> updateDiffSharedScope = new HashMap<>();
        updateDiffSharedScope.put("attr8", "val8");
        updateDiffSharedScope.put("attr9", null);
        updateDiffExpected.put(SubscriptionType.SHARED_SCOPE, updateDiffSharedScope);
        System.out.println("Expected diff:\n" + updateDiffExpected);

        assertEquals(updateDiffExpected, updateDiff);
    }

    /**
     * Test {@link DeviceTwin#getUpdateDiff(Map, boolean)} method.
     */
    @Test public void getUpdateDiffTestIgnoreTwinNull() {
        DeviceTwin twin = new DeviceTwin(new Device());

        // initialize twin
        Map<String, Object> twinClientScope = new HashMap<>();
        twinClientScope.put("attr1", "val1");
        twinClientScope.put("attr2", 2);
        twinClientScope.put("attr3", false);
        twin.update(SubscriptionType.CLIENT_SCOPE, twinClientScope);
        Map<String, Object> twinServerScope = new HashMap<>();
        twinServerScope.put("attr4", "val4");
        twinServerScope.put("attr5", null);
        twin.update(SubscriptionType.SERVER_SCOPE, twinServerScope);
        Map<String, Object> twinSharedScope = new HashMap<>();
        twinSharedScope.put("attr8", true);
        twinSharedScope.put("attr9", true);
        twin.update(SubscriptionType.SHARED_SCOPE, twinSharedScope);
        System.out.println("Twin state:\n" + twin);

        // update twin
        Map<SubscriptionType, Map<String, Object>> update = new HashMap<>();
        Map<String, Object> updateClientScope = new HashMap<>();
        updateClientScope.put("attr1", "val1");
        updateClientScope.put("attr2", 3);
        updateClientScope.put("attr3", false);
        update.put(SubscriptionType.CLIENT_SCOPE, updateClientScope);
        Map<String, Object> updateServerScope = new HashMap<>();
        updateServerScope.put("attr4", "val5");
        updateServerScope.put("attr5", 1.1);
        updateServerScope.put("attr6", 6);
        updateServerScope.put("attr7", null);
        update.put(SubscriptionType.SERVER_SCOPE, updateServerScope);
        Map<String, Object> updateSharedScope = new HashMap<>();
        updateSharedScope.put("attr8", false);
        updateSharedScope.put("attr9", true);
        update.put(SubscriptionType.SHARED_SCOPE, updateSharedScope);
        Map<SubscriptionType, Map<String, Object>> updateDiff = twin.getUpdateDiff(update, true);
        System.out.println("Actual diff:\n" + updateDiff);

        // expected diff between update and twin state
        Map<SubscriptionType, Map<String, Object>> updateDiffExpected = new HashMap<>();
        Map<String, Object> updateDiffClientScope = new HashMap<>();
        //        updateDiffClientScope.put("attr1", "val1");
        updateDiffClientScope.put("attr2", 3);
        //        updateDiffClientScope.put("attr3", false);
        updateDiffExpected.put(SubscriptionType.CLIENT_SCOPE, updateDiffClientScope);
        Map<String, Object> updateDiffServerScope = new HashMap<>();
        updateDiffServerScope.put("attr4", "val5");
//        updateDiffServerScope.put("attr5", 1.1);
//        updateDiffServerScope.put("attr6", 6);
//        updateDiffServerScope.put("attr7", null);
        updateDiffExpected.put(SubscriptionType.SERVER_SCOPE, updateDiffServerScope);
        Map<String, Object> updateDiffSharedScope = new HashMap<>();
        updateDiffSharedScope.put("attr8", false);
//        updateDiffSharedScope.put("attr9", true);
        updateDiffExpected.put(SubscriptionType.SHARED_SCOPE, updateDiffSharedScope);
        System.out.println("Expected diff:\n" + updateDiffExpected);

        assertEquals(updateDiffExpected, updateDiff);
    }

    /**
     * Test {@link DeviceTwin#getUpdateDiff(Map)} method, particularly handling of boolean values.
     */
    @Test public void getUpdateDiffTestBool() {
        DeviceTwin twin = new DeviceTwin(new Device());

        // initialize twin
        Map<String, Object> twinClientScope = new HashMap<>();
        twinClientScope.put("attr1", "val1");
        twinClientScope.put("attr2", true);
        twinClientScope.put("attr3", true);
        twin.update(SubscriptionType.CLIENT_SCOPE, twinClientScope);
        Map<String, Object> twinServerScope = new HashMap<>();
        twinServerScope.put("attr4", "val4");
        twinServerScope.put("attr5", false);
        twinServerScope.put("attr6", false);
        twinServerScope.put("attr7", "false");
        twinServerScope.put("attr8", "true");
        twin.update(SubscriptionType.SERVER_SCOPE, twinServerScope);
        System.out.println("Twin state:\n" + twin);

        // update twin
        Map<SubscriptionType, Map<String, Object>> update = new HashMap<>();
        Map<String, Object> updateClientScope = new HashMap<>();
        updateClientScope.put("attr1", true);   // diff
        updateClientScope.put("attr2", true);   // equal
        updateClientScope.put("attr3", false);  // diff
        update.put(SubscriptionType.CLIENT_SCOPE, updateClientScope);
        Map<String, Object> updateServerScope = new HashMap<>();
        updateServerScope.put("attr4", false);  // diff
        updateServerScope.put("attr5", false);  // equal
        updateServerScope.put("attr6", true);   // diff
        updateServerScope.put("attr7", false);  // equal!
        updateServerScope.put("attr8", true);   // equal!
        updateServerScope.put("attr9", true);   // diff
        update.put(SubscriptionType.SERVER_SCOPE, updateServerScope);
        Map<String, Object> updateSharedScope = new HashMap<>();
        updateSharedScope.put("attr8", true);   // diff
        updateSharedScope.put("attr9", false);  // diff
        update.put(SubscriptionType.SHARED_SCOPE, updateSharedScope);
        Map<SubscriptionType, Map<String, Object>> updateDiff = twin.getUpdateDiff(update);
        System.out.println("Actual diff:\n" + updateDiff);

        // expected diff between update and twin state
        Map<SubscriptionType, Map<String, Object>> updateDiffExpected = new HashMap<>();
        Map<String, Object> updateDiffClientScope = new HashMap<>();
        updateDiffClientScope.put("attr1", true);   // diff
        //        updateDiffClientScope.put("attr2", true);   // equal
        updateDiffClientScope.put("attr3", false);  // diff
        updateDiffExpected.put(SubscriptionType.CLIENT_SCOPE, updateDiffClientScope);
        Map<String, Object> updateDiffServerScope = new HashMap<>();
        updateDiffServerScope.put("attr4", false);  // diff
        //        updateDiffServerScope.put("attr5", false);  // equal
        updateDiffServerScope.put("attr6", true);   // diff
        //        updateServerScope.put("attr7", false);  // equal!
        //        updateServerScope.put("attr8", true);   // equal!
        updateDiffServerScope.put("attr9", true);   // diff
        updateDiffExpected.put(SubscriptionType.SERVER_SCOPE, updateDiffServerScope);
        Map<String, Object> updateDiffSharedScope = new HashMap<>();
        updateDiffSharedScope.put("attr8", true);   // diff
        updateDiffSharedScope.put("attr9", false);  // diff
        updateDiffExpected.put(SubscriptionType.SHARED_SCOPE, updateDiffSharedScope);
        System.out.println("Expected diff:\n" + updateDiffExpected);

        assertEquals(updateDiffExpected, updateDiff);
    }

    /**
     * Test {@link DeviceTwin#getUpdateDiff(Map)} method, particularly handling of numeric values.
     */
    @Test public void getUpdateDiffTestNum() {
        DeviceTwin twin = new DeviceTwin(new Device());

        // initialize twin
        Map<String, Object> twinClientScope = new HashMap<>();
        twinClientScope.put("attr0", true);
        twinClientScope.put("attr1", "true");
        twinClientScope.put("attr2", 0);
        twinClientScope.put("attr3", 1);
        twin.update(SubscriptionType.CLIENT_SCOPE, twinClientScope);
        Map<String, Object> twinServerScope = new HashMap<>();
        twinServerScope.put("attr4", 1.1);
        twinServerScope.put("attr5", -1.1);
        twinServerScope.put("attr6", -123);
        twinServerScope.put("attr7", "12");
        twinServerScope.put("attr8", "3.3");
        twinServerScope.put("attr9", "-4.5");
        twinServerScope.put("attr10", "-33");
        twinServerScope.put("attr11", 1.1);
        twinServerScope.put("attr12", -1.1);
        twinServerScope.put("attr13", -123);
        twinServerScope.put("attr14", "12");
        twinServerScope.put("attr15", "3.3");
        twinServerScope.put("attr16", "-4.5");
        twinServerScope.put("attr17", "-33");
        twin.update(SubscriptionType.SERVER_SCOPE, twinServerScope);
        System.out.println("Twin state:\n" + twin);

        // update twin
        Map<SubscriptionType, Map<String, Object>> update = new HashMap<>();
        Map<String, Object> updateClientScope = new HashMap<>();
        updateClientScope.put("attr0", 1);   // diff
        updateClientScope.put("attr1", 1);   // diff
        updateClientScope.put("attr2", 1);   // diff
        updateClientScope.put("attr3", 1);   // equal
        update.put(SubscriptionType.CLIENT_SCOPE, updateClientScope);
        Map<String, Object> updateServerScope = new HashMap<>();
        updateServerScope.put("attr4", 1.2);    // diff
        updateServerScope.put("attr5", 1.1);    // diff
        updateServerScope.put("attr6", -123.1); // diff
        updateServerScope.put("attr7", 13);     // diff
        updateServerScope.put("attr8", 3);      // diff
        updateServerScope.put("attr9", 4.5);    // diff
        updateServerScope.put("attr10", 33);    // diff
        updateServerScope.put("attr11", "1.1"); // equal
        updateServerScope.put("attr12", "-1.1");// equal
        updateServerScope.put("attr13", "-123");// equal
        updateServerScope.put("attr14", "12");  // equal
        updateServerScope.put("attr15", "3.3"); // equal
        updateServerScope.put("attr16", "-4.5");// equal
        updateServerScope.put("attr17", "-33"); // equal
        update.put(SubscriptionType.SERVER_SCOPE, updateServerScope);
        Map<String, Object> updateSharedScope = new HashMap<>();
        updateSharedScope.put("attr18", 1);     // diff
        updateSharedScope.put("attr19", 0.1);   // diff
        update.put(SubscriptionType.SHARED_SCOPE, updateSharedScope);
        Map<SubscriptionType, Map<String, Object>> updateDiff = twin.getUpdateDiff(update);
        System.out.println("Actual diff:\n" + updateDiff);

        // expected diff between update and twin state
        Map<SubscriptionType, Map<String, Object>> updateDiffExpected = new HashMap<>();
        Map<String, Object> updateDiffClientScope = new HashMap<>();
        updateDiffClientScope.put("attr0", 1);   // diff
        updateDiffClientScope.put("attr1", 1);   // diff
        updateDiffClientScope.put("attr2", 1);   // diff
        //        updateDiffClientScope.put("attr3", 1);   // equal
        updateDiffExpected.put(SubscriptionType.CLIENT_SCOPE, updateDiffClientScope);
        Map<String, Object> updateDiffServerScope = new HashMap<>();
        updateDiffServerScope.put("attr4", 1.2);    // diff
        updateDiffServerScope.put("attr5", 1.1);    // diff
        updateDiffServerScope.put("attr6", -123.1); // diff
        updateDiffServerScope.put("attr7", 13);     // diff
        updateDiffServerScope.put("attr8", 3);      // diff
        updateDiffServerScope.put("attr9", 4.5);    // diff
        updateDiffServerScope.put("attr10", 33);    // diff
        //        updateDiffServerScope.put("attr11", "1.1"); // equal
        //        updateDiffServerScope.put("attr12", "-1.1");// equal
        //        updateDiffServerScope.put("attr13", "-123");// equal
        //        updateDiffServerScope.put("attr14", "12");  // equal
        //        updateDiffServerScope.put("attr15", "3.3"); // equal
        //        updateDiffServerScope.put("attr16", "-4.5");// equal
        //        updateDiffServerScope.put("attr17", "-33"); // equal
        updateDiffExpected.put(SubscriptionType.SERVER_SCOPE, updateDiffServerScope);
        Map<String, Object> updateDiffSharedScope = new HashMap<>();
        updateDiffSharedScope.put("attr18", 1);     // diff
        updateDiffSharedScope.put("attr19", 0.1);   // diff
        updateDiffExpected.put(SubscriptionType.SHARED_SCOPE, updateDiffSharedScope);
        System.out.println("Expected diff:\n" + updateDiffExpected);

        assertEquals(updateDiffExpected, updateDiff);
    }

    /**
     * Test {@link DeviceTwin#update(SubscriptionType, AttrUpdate)} method.
     */
    @Test public void updateTest() {
        DeviceTwin twin = new DeviceTwin(new Device());

        List<AttrUpdate> updateListClientScope = new LinkedList<>();
        updateListClientScope.add(new AttrUpdate("key1", 123L, "value1"));
        updateListClientScope.add(new AttrUpdate("key2", 124L, true));
        updateListClientScope.add(new AttrUpdate("key3", 125L, 1));
        updateListClientScope.add(new AttrUpdate("key4", 125L, null));
        for (AttrUpdate u : updateListClientScope) {
            twin.update(SubscriptionType.CLIENT_SCOPE, u);
        }

        List<AttrUpdate> updateListServerScope = new LinkedList<>();
        updateListServerScope.add(new AttrUpdate("key1", 123L, "value1"));
        updateListServerScope.add(new AttrUpdate("key2", 124L, true));
        updateListServerScope.add(new AttrUpdate("key3", 125L, 1));
        updateListServerScope.add(new AttrUpdate("key4", 125L, null));
        for (AttrUpdate u : updateListServerScope) {
            twin.update(SubscriptionType.SERVER_SCOPE, u);
        }

        Map<SubscriptionType, Map<String, Object>> expectedTwin = new HashMap<>();
        Map<String, Object> expectedClientScope = new HashMap<>();
        expectedClientScope.put("key1", "value1");
        expectedClientScope.put("key2", true);
        expectedClientScope.put("key3", 1);
        expectedClientScope.put("key4", null);
        expectedTwin.put(SubscriptionType.CLIENT_SCOPE, expectedClientScope);

        Map<String, Object> expectedServerScope = new HashMap<>();
        expectedServerScope.put("key1", "value1");
        expectedServerScope.put("key2", true);
        expectedServerScope.put("key3", 1);
        expectedServerScope.put("key4", null);
        expectedTwin.put(SubscriptionType.SERVER_SCOPE, expectedServerScope);

        System.out.println("Expected twin: " + expectedTwin);
        System.out.println("Actual twin: " + twin.getAll());
        assertEquals(expectedTwin, twin.getAll());
    }

    /**
     * Test, if attribute history entries are being ignored.
     */
    @Test public void updateTestAttrHist() {
        DeviceTwin twin = new DeviceTwin(new Device());

        List<AttrUpdate> updateListClientScope = new LinkedList<>();
        updateListClientScope.add(new AttrUpdate("key1", 123L, "value1"));
        for (AttrUpdate u : updateListClientScope) {
            twin.update(SubscriptionType.CLIENT_SCOPE, u);
        }

        List<AttrUpdate> updateListTelemetry = new LinkedList<>();
        updateListTelemetry.add(new AttrUpdate("attrscope__SERVER_SCOPE__name__underPressureWindowSizeSec", 123L, "20"));
        updateListTelemetry.add(new AttrUpdate("pressureValue", 124L, 1.23));
        for (AttrUpdate u : updateListTelemetry) {
            twin.update(SubscriptionType.LATEST_TELEMETRY, u);
        }

        Map<SubscriptionType, Map<String, Object>> expectedTwin = new HashMap<>();
        Map<String, Object> expectedClientScope = new HashMap<>();
        expectedClientScope.put("key1", "value1");
        expectedTwin.put(SubscriptionType.CLIENT_SCOPE, expectedClientScope);

        Map<String, Object> expectedTelemetry = new HashMap<>();
//        expectedTelemetry.put("attrscope__SERVER_SCOPE__name__underPressureWindowSizeSec", "20");
        expectedTelemetry.put("pressureValue", 1.23);
        expectedTwin.put(SubscriptionType.LATEST_TELEMETRY, expectedTelemetry);

        System.out.println("Expected twin: " + expectedTwin);
        System.out.println("Actual twin: " + twin.getAll());
        assertEquals(expectedTwin, twin.getAll());
    }
}
