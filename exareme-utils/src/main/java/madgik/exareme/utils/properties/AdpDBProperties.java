/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.utils.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author herald
 */
public class AdpDBProperties {
    private static final GenericProperties adpDBProperties;
    private static final Logger log = LoggerFactory.getLogger(AdpDBProperties.class);

    static {
        try {
            adpDBProperties = PropertiesFactory.loadProperties("db");
        } catch (Exception e) {
            log.error("Cannot initialize properties", e);
            throw new RuntimeException("Could not initialize adpDB properties!", e);
        }
    }

    private AdpDBProperties() {
        throw new RuntimeException("Cannot create instance of this class");
    }

    public static GenericProperties getAdpDBProps() {
        return adpDBProperties;
    }
}
