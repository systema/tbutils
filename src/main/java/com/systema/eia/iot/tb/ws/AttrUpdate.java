package com.systema.eia.iot.tb.ws;


/**
 * Helper class to structure a ThingsBoard device attribute update. This is a
 * structured representation of one item of a ThingsBoard websocket message, see
 * {@link WsParserUtils#parseMessage(String).
 *
 * @author wogawa
 */
public class AttrUpdate {

    public static final String CLASS_NAME = AttrUpdate.class.getSimpleName();

    /**
     * Attribute name.
     */
    public String key;

    /**
     * Timestamp of the attribute update in epoch ms.
     */
    public long timestamp;

    /**
     * New attribute value.
     */
    public Object value;

    public AttrUpdate() {
    }

    public AttrUpdate(String key, long timestamp, Object value) {
        this.key = key;
        this.timestamp = timestamp;
        this.value = value;
    }

    /**
     * Compare two attribute update objects
     *
     * @param o - other object to be compared with this one
     * @return true, if {@code o} is of type {@link AttrUpdate} and all
     * properties are equal - key: both Strings are {@code null} or equal
     * ({@link String#equals(Object)}); timestamp: equal (==); value: both
     * Objects are {@code null} or equal ({@link Object#equals(Object)})
     */
    public boolean equals(Object o) {
        if (!(o instanceof AttrUpdate)) {
            return false;
        }
        AttrUpdate other = (AttrUpdate) o;
        // both keys are null or equal
        boolean keysEqual = (this.key == null && other.key == null) || (this.key != null && this.key.equals(other.key));
        // both timestamps are equal
        boolean timestampsEqual = this.timestamp == other.timestamp;
        // both values are null or equal
        boolean valuesEqual = (this.value == null && other.value == null)
                || (this.value != null && this.value.equals(other.value));
        return keysEqual && timestampsEqual && valuesEqual;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{ key: " + key + ", timestamp: " + timestamp + ", value: " + value + " }";
    }
}
