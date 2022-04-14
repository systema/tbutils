package com.systema.eia.iot.tb.ws;

import com.systema.eia.iot.tb.ws.AttrUpdate;
import com.systema.eia.iot.tb.ws.WsParserUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WsParserUtilsTest {
    /**
     * Test {@link WsParserUtils#parseMessage(String)} method.
     */
    @Test
    public void parseMessageTest() {
        //@formatter:off
        String message = "{" + 
                "  \"subscriptionId\": -901980230," + 
                "  \"errorCode\": 0," + 
                "  \"errorMsg\": null," + 
                "  \"data\": {" + 
                "    \"pressure_value\": [" + 
                "      [6, \"-0.017511\"]," + 
                "      [3, \"-0.017511\"]," + 
                "      [5, \"-0.018762\"]," + 
                "      [1, \"-0.018762\"]" + 
                "    ]," + 
                "    \"processingEnabled\": [" + 
                "      [4, \"false\"]" + "    ]," + 
                "    \"pressureOk\": [" + 
                "      [3, \"false\"]," + 
                "      [5, \"true\"]," + 
                "      [7, \"false\"]" + 
                "    ]," + 
                "    \"lightTowerGreen\": [" + 
                "      [2, \"on\"]," + 
                "      [9, \"off\"]," + 
                "      [4, \"off\"]," + 
                "      [6, \"on\"]" + 
                "    ]," +
                "    \"barcode\": [" +
                "      [10, \"21079503\"]" +
                "    ]" +
                "  }," + 
                "  \"latestValues\": {" + 
                "    \"processingEnabled\": 4," + 
                "    \"pressure_value\": 6," + 
                "    \"pressureOk\": 7," + 
                "    \"lightTowerGreen\": 9" + 
                "  }" + 
                "}";
                //@formatter:on

        List<AttrUpdate> expectedUpdateList = new LinkedList<>();
        AttrUpdate update = new AttrUpdate();
        update.key = "pressure_value";
        update.timestamp = 1L;
        update.value = "-0.018762";
        expectedUpdateList.add(update);

        update = new AttrUpdate();
        update.key = "lightTowerGreen";
        update.timestamp = 2L;
        update.value = "on";
        expectedUpdateList.add(update);

        update = new AttrUpdate();
        update.key = "pressure_value";
        update.timestamp = 3L;
        update.value = "-0.017511";
        expectedUpdateList.add(update);
        update = new AttrUpdate();
        update.key = "pressureOk";
        update.timestamp = 3L;
        update.value = "false";
        expectedUpdateList.add(update);

        update = new AttrUpdate();
        update.key = "processingEnabled";
        update.timestamp = 4L;
        update.value = "false";
        expectedUpdateList.add(update);
        update = new AttrUpdate();
        update.key = "lightTowerGreen";
        update.timestamp = 4L;
        update.value = "off";
        expectedUpdateList.add(update);

        update = new AttrUpdate();
        update.key = "pressure_value";
        update.timestamp = 5L;
        update.value = "-0.018762";
        expectedUpdateList.add(update);
        update = new AttrUpdate();
        update.key = "pressureOk";
        update.timestamp = 5L;
        update.value = "true";
        expectedUpdateList.add(update);

        update = new AttrUpdate();
        update.key = "pressure_value";
        update.timestamp = 6L;
        update.value = "-0.017511";
        expectedUpdateList.add(update);
        update = new AttrUpdate();
        update.key = "lightTowerGreen";
        update.timestamp = 6L;
        update.value = "on";
        expectedUpdateList.add(update);

        update = new AttrUpdate();
        update.key = "pressureOk";
        update.timestamp = 7L;
        update.value = "false";
        expectedUpdateList.add(update);

        update = new AttrUpdate();
        update.key = "lightTowerGreen";
        update.timestamp = 9L;
        update.value = "off";
        expectedUpdateList.add(update);

        update = new AttrUpdate();
        update.key = "barcode";
        update.timestamp = 10L;
        update.value = "21079503";
        expectedUpdateList.add(update);
        try {
            List<AttrUpdate> actualUpdateList = WsParserUtils.parseMessage(message);
            System.out.println("Expected result:\n" + expectedUpdateList);
            System.out.println("Actual result:\n" + actualUpdateList);
            assertEquals(expectedUpdateList, actualUpdateList);
        } catch (ParseException e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    @NotNull
    private static List<Arguments> provideInvalidMessages() {
        List<Arguments> argumentsList = new LinkedList<>();

        // JSON format error (missing closing bracket)
        //@formatter:off
        String message = new String("{" + 
                "  \"subscriptionId\": -901980230," + 
                "  \"errorCode\": 0," + 
                "  \"errorMsg\": null," + 
                "  \"data\": {}," + 
                "    \"pressure_value\": [" + 
                "      [1, \"-0.018762\"]" +
                "    ]," + 
                "  \"latestValues\": {" + 
                "    \"pressure_value\": 1" + 
                "  }" + 
                "}");
        //@formatter:on
        argumentsList.add(Arguments.of(message));

        // no "data" and "subscriptionId" field
        //@formatter:off
        message = new String("{" +
                "  \"errorCode\": 0," + 
                "  \"errorMsg\": null," + 
                "  \"latestValues\": {" + 
                "    \"lightTowerGreen\": 9" + 
                "  }" + 
                "}");
        //@formatter:on
        argumentsList.add(Arguments.of(message));

        // empty message
        message = new String("");
        argumentsList.add(Arguments.of(message));

        return argumentsList;
    }

    /**
     * Test {@link WsParserUtils#parseMessage(String)} method for invalid JSON input.
     */
    @ParameterizedTest
    @MethodSource("provideInvalidMessages")
    public void parseMessageTestJsonError(String message) {
        try {
            WsParserUtils.parseMessage(message);
        } catch (ParseException e) {
            System.out.println("Test passed due to receiving expected exception: " + e.getMessage());
            // test passes
        }
    }

    @NotNull
    private static List<Arguments> provideEmptyMessages() {
        List<Arguments> argumentsList = new LinkedList<>();

        // empty "data" field
        //@formatter:off
        String message = new String("{" + 
                "  \"subscriptionId\": -901980230," + 
                "  \"errorCode\": 0," + 
                "  \"errorMsg\": null," + 
                "  \"data\": {}," + 
                "  \"latestValues\": {}" + 
                "}");
        //@formatter:on
        argumentsList.add(Arguments.of(message));

        return argumentsList;
    }

    /**
     * Test {@link WsParserUtils#parseMessage(String)} method for invalid JSON input.
     */
    @ParameterizedTest
    @MethodSource("provideEmptyMessages")
    public void parseMessageTestNoData(String message) {
        try {
            List<AttrUpdate> actualUpdateList = WsParserUtils.parseMessage(message);
            System.out.println("Actual result:\n" + actualUpdateList);
            assertTrue(actualUpdateList.isEmpty());
        } catch (ParseException e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    @NotNull
    private static List<Arguments> provideValidSubscrIdMessages() {
        List<Arguments> argumentsList = new LinkedList<>();

        // only "subscriptionId"
        var id = Integer.valueOf(-901980230);
        //@formatter:off
        var message = new String("{" +
                "  \"subscriptionId\": " + id +
                "}");
        //@formatter:on
        argumentsList.add(Arguments.of(message, id));

        // full message
        id = Integer.valueOf(1234567);
        //@formatter:off
        message = new String("{" +
                "  \"subscriptionId\": " + id + "," +
                "  \"errorCode\": 0," +
                "  \"errorMsg\": null," +
                "  \"data\": {" +
                "    \"processingEnabled\": [" +
                "      [4, \"false\"]" +
                "    ]" +
                "  }," +
                "  \"latestValues\": {" +
                "    \"processingEnabled\": 4" +
                "  }" +
                "}");
        //@formatter:on
        argumentsList.add(Arguments.of(message, id));

        return argumentsList;
    }

    /**
     * Test {@link WsParserUtils#parseSubscriptionId(String)} (String)} method for valid JSON input.
     */
    @ParameterizedTest
    @MethodSource("provideValidSubscrIdMessages")
    public void parseSubscriptionIdTest(String message, Integer id) {
        try {
            var parsedId = WsParserUtils.parseSubscriptionId(message);
            assertEquals(id, parsedId);
        } catch (ParseException e) {
            fail("Test failed: " + e.getMessage());
        }
    }

    /**
     * Test {@link WsParserUtils#parseSubscriptionId(String)} method for invalid JSON input.
     */
    @ParameterizedTest
    @MethodSource("provideInvalidMessages")
    public void parseSubscriptionIdTestJsonError(String message) {
        try {
            WsParserUtils.parseSubscriptionId(message);
        } catch (ParseException e) {
            System.out.println("Test passed due to receiving expected exception: " + e.getMessage());
            // test passes
        }
    }
}
