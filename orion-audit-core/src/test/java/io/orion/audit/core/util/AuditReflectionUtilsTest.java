package io.orion.audit.core.util;

import io.orion.audit.core.annotation.AuditField;
import io.orion.audit.core.annotation.AuditIgnore;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditReflectionUtilsTest {

    @Test
    void shouldReturnInheritedNonStaticFields() {
        List<Field> fields = AuditReflectionUtils.getAllFields(ChildRecord.class);

        assertThat(fields)
            .extracting(Field::getName)
            .containsExactly("parentName", "secret", "childName");
    }

    @Test
    void shouldResolveCustomAuditFieldName() throws Exception {
        Field field = ChildRecord.class.getDeclaredField("childName");

        assertThat(AuditReflectionUtils.resolveAuditFieldName(field)).isEqualTo("display_name");
    }

    @Test
    void shouldDetectIgnoredFields() throws Exception {
        Field field = ParentRecord.class.getDeclaredField("secret");

        assertThat(AuditReflectionUtils.isIgnored(field)).isTrue();
    }

    private static class ParentRecord {
        private String parentName = "parent";

        @AuditIgnore
        private String secret = "hidden";

        private static final String STATIC_FIELD = "ignored";
    }

    private static class ChildRecord extends ParentRecord {
        @AuditField("display_name")
        private String childName = "child";
    }
}
