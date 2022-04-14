/**
 * Example SLF4J logger binder implementation, according to http://www.slf4j.org/faq.html#slf4j_compatible
 */

package org.slf4j.impl;

import com.systema.eia.iot.tb.examples.logging.ExampleSlf4jLoggerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {

    /**
     * The unique instance of this class.
     *
     */
    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    private static final String LOGGER_FACTORYCLASS_STR = ExampleSlf4jLoggerFactory.class.getSimpleName();

    /**
     * Return the singleton of this class.
     *
     * @return the StaticLoggerBinder singleton
     */
    public static final StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    /**
     * The ILoggerFactory instance returned by the {@link #getLoggerFactory}
     * method should always be the same object
     */
    private final ILoggerFactory loggerFactory;

    private StaticLoggerBinder() {
        loggerFactory = new ExampleSlf4jLoggerFactory();
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public String getLoggerFactoryClassStr() {
        return LOGGER_FACTORYCLASS_STR;
    }
}
