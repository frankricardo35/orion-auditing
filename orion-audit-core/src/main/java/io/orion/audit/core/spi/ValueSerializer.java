package io.orion.audit.core.spi;

/**
 * Serializes audit payload values.
 */
public interface ValueSerializer {

    /**
     * @param value value to serialize
     * @return serialized representation
     */
    String serialize(Object value);
}
