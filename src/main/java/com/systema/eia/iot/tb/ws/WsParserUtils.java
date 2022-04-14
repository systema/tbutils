package com.systema.eia.iot.tb.ws;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WsParserUtils {

    private static final String CLASS_NAME = WsParserUtils.class.getSimpleName();

    private static final Logger log = LoggerFactory.getLogger(CLASS_NAME);

    // @formatter:off
    /**
     * Parse ThingsBoard JSON message and extract relevant information.
     * 
     * @param message - JSON message of the format:
     * <pre>
     * {
     *     "subscriptionId": -901980230,
     *     "errorCode": 0,
     *     "errorMsg": null,
     *     "data": {
     *          "pressure_value": [
     *                  [1617975311084,"-0.017511"],
     *                  [1617975309584,"-0.018762"]
     *          ],
     *          "processingEnabled": [ 
     *                  [ 1617975371768, "false" ] 
     *          ]
     *     }, 
     *     "latestValues": { 
     *             "processingEnabled": 1617975371768,
     *             "pressure_value": 1617975309584 
     *     }
     * }
     * <pre>
     * 
     * @return empty list, if data field is   
     *          empty, otherwise list of attribute updates, sorted by
     *          timestamp (asc), format:
     * <pre>
     *  [
     *   AttrUpdate{key: "pressure_value", timestamp: 1617975309584, value: -0.018762},
     *   AttrUpdate{key: "pressure_value", timestamp: 1617975311084, value: -0.017511}, 
     *   AttrUpdate{key: "processingEnabled", timestamp: 1617975371768, value: "false"}
     *  ] 
     * <pre>
     * @throws {@link java.text.ParseException} if message could not be parsed as JSON or
     *          data item is missing in message
     */
    // @formatter:on
    public static List<AttrUpdate> parseMessage(String message) throws ParseException {
        // TODO: handle errorMsg / errorCode
        List<AttrUpdate> updateList = new LinkedList<>();
        log.debug("Trying to parse message: " + message);
        final var mapper = new ObjectMapper();
        final var data = extractTopLevelNode(message, "data", mapper);
        if (data != null) {
            log.debug("Found ThingsBoard update data:\n" + data.toPrettyString());
            // get all values out of the {<key>: [[<timestamp>, <value>]]} map and put them
            // into a list [{key: <key>, timestamp: <timestamp>, value: <value>}]
            final ObjectReader reader = mapper.readerFor(new TypeReference<List<List<Object>>>() {
            });
            for (Iterator<String> keys = data.fieldNames(); keys.hasNext(); ) {
                final var key = keys.next();
                List<List<Object>> dataArray = null;
                try {
                    dataArray = reader.readValue(data.get(key));
                } catch (IOException e) {
                    log.warn("Error reading array: " + e + "\nmessage: " + message);
                    continue;
                }
                for (List<Object> innerArray : dataArray) {
                    Object value = null;
                    try {
                        var update = new AttrUpdate();
                        update.key = key;
                        update.timestamp = Long.parseLong(innerArray.get(0).toString());
                        value = innerArray.get(1);
                        update.value = value;
                        updateList.add(update);
                        log.trace("Found update: " + update.key + ": " + update.value + " (" + update.timestamp + ")");
                    } catch (Exception e) {
                        log.warn("Error reading value: " + e.getMessage() + "\nvalue: " + value + "\nmessage: " +
                                 message);
                    }

                }
            }
            // sort all values in the list by timestamp (ascending)
            updateList.sort((u1, u2) -> (int) (u1.timestamp - u2.timestamp));
            var sortedListString = new StringBuilder();
            for (AttrUpdate u : updateList)
                sortedListString.append(u).append("\n");
            log.trace("Sorted update list:\n" + sortedListString);
        } else {
            log.warn("Received ThingsBoard message with empty \"data\" item: " + message);
        }
        return updateList;
    }

    /**
     * Parse subscription ID from a JSON message.
     *
     * @param message - JSON message of the format:
     *                * <pre>
     *                               {
     *                                   "subscriptionId": -901980230,
     *                                   "errorCode": 0,
     *                                   "errorMsg": null,
     *                                   "data": {
     *                                        "pressure_value": [
     *                                                [1617975311084,"-0.017511"],
     *                                                [1617975309584,"-0.018762"]
     *                                        ],
     *                                        "processingEnabled": [
     *                                                [ 1617975371768, "false" ]
     *                                        ]
     *                                   },
     *                                   "latestValues": {
     *                                           "processingEnabled": 1617975371768,
     *                                           "pressure_value": 1617975309584
     *                                   }
     *                               }
     *                               <pre>
     *                               @return subscription ID
     *                               @throws {@link ParseException} if subscription ID could not be parsed
     */
    public static int parseSubscriptionId(String message) throws ParseException {
        log.debug("Trying to parse subscription ID in message: " + message);
        final var mapper = new ObjectMapper();
        final var id = extractTopLevelNode(message, "subscriptionId", mapper);
        try {
            return Integer.parseInt(id.asText());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ParseException(
                    "Error reading subscription ID in message: \"" + message + "\" - " + e.getMessage(), 0);
        }
    }

    public static JsonNode extractTopLevelNode(String json, String key, ObjectMapper mapper) throws ParseException {
        JsonNode result = null;
        if (json != null && !json.isEmpty()) {
            try {
                result = mapper.readTree(json).get(key);
            } catch (NullPointerException e) {
                throw new ParseException("Cannot find \"" + key + "\" item in message: " + json, 0);
            } catch (Exception e) {
                throw new ParseException(
                        "Error parsing message: " + e.getClass() + " - " + e.getMessage() + "\nmessage:\n" + json, 0);
            }
        }
        return result;
    }

}
