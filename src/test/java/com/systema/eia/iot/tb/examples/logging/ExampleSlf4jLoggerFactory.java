/**
 * Example SLF4J logger factory implementation according to http://www.slf4j.org/faq.html#slf4j_compatible.
 */

package com.systema.eia.iot.tb.examples.logging;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class ExampleSlf4jLoggerFactory implements ILoggerFactory {
    @Override
    public Logger getLogger(String name) {
        return new ExampleSlf4jLoggerAdapter();
    }
}
