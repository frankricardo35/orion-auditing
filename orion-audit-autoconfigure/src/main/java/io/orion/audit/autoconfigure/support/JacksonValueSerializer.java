package io.orion.audit.autoconfigure.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.orion.audit.core.spi.ValueSerializer;

/**
 * Jackson-based value serializer.
 */
public class JacksonValueSerializer implements ValueSerializer {

    private final ObjectMapper objectMapper;

    public JacksonValueSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit value", ex);
        }
    }
}
