/**
 * Copyright MaDgIK Group 2010 - 2015.
 */
package madgik.exareme.utils.serialization;


import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author herald
 */
public class SerializationUtil {
    private static Logger log = LoggerFactory.getLogger(SerializationUtil.class);

    private SerializationUtil() {
        throw new RuntimeException("Cannot create instances of this class");
    }

    @SuppressWarnings("unchecked") public static <T extends Serializable> T deepCopy(T object) {
        try {
            SerializedObject so = new SerializedObject(object);
            return (T) so.getObject();
        } catch (Exception e) {
            log.error("Cannot serialize object. Returning null.", e);
            return null;
        }
    }
}
