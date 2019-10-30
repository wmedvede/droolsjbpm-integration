package org.jbpm.task.assigning.runtime.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyUtil {

    private static Logger LOGGER = LoggerFactory.getLogger(PropertyUtil.class);

    public interface PropertyParser<String, T, E extends Exception> {

        T parse(String value) throws E;
    }

    private PropertyUtil() {
    }

    public static <T, E extends Exception> T readSystemProperty(String propertyName, T defaultValue, PropertyParser<String, T, E> parser) {
        String strValue = null;
        try {
            strValue = System.getProperty(propertyName);
            if (strValue == null) {
                LOGGER.debug("Property: {}  was not configured. Default value will be used instead: {}", propertyName, defaultValue);
                return defaultValue;
            }

            return parser.parse(strValue);
        } catch (Exception e) {
            LOGGER.error("An error was produced while parsing " + propertyName + " value from string: " + strValue +
                                 ", default value: " + defaultValue + " will be used instead.", e);
            return defaultValue;
        }
    }
}
