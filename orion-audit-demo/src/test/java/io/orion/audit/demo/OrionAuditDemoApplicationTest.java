package io.orion.audit.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.query.AuditPageRequest;
import io.orion.audit.core.query.AuditQueryCriteria;
import io.orion.audit.core.query.AuditQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrionAuditDemoApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditQueryService auditQueryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateUpdateAndDeleteAuditRecords() throws Exception {
        String createBody = """
            {"name":"Alice","email":"alice@example.com","password":"secret"}
            """;

        String updateBody = """
            {"name":"Alice Smith","email":"alice.smith@example.com","password":"new-secret"}
            """;

        String response = mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", "trace-123")
                .content(createBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        long customerId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", "trace-456")
                .content(updateBody))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/customers/{id}", customerId))
            .andExpect(status().isNoContent());

        List<Map<String, Object>> auditRows = jdbcTemplate.queryForList(
            "SELECT action, entity_id, old_values, new_values, changed_fields, trace_id FROM audit_log ORDER BY created_at ASC"
        );

        assertThat(auditRows)
            .hasSize(3)
            .allSatisfy(entry -> assertThat(entry.get("entity_id")).isEqualTo(String.valueOf(customerId)));

        assertThat(auditRows)
            .anySatisfy(entry -> {
                assertThat(entry.get("action")).isEqualTo("INSERT");
                assertThat((String) entry.get("new_values")).contains("Alice");
                assertThat((String) entry.get("new_values")).doesNotContain("secret");
                assertThat(entry.get("trace_id")).isEqualTo("trace-123");
            })
            .anySatisfy(entry -> {
                assertThat(entry.get("action")).isEqualTo("UPDATE");
                assertThat((String) entry.get("changed_fields")).contains("Alice Smith");
                assertThat((String) entry.get("changed_fields")).doesNotContain("new-secret");
                assertThat(entry.get("trace_id")).isEqualTo("trace-456");
            })
            .anySatisfy(entry -> {
                assertThat(entry.get("action")).isEqualTo("DELETE");
                assertThat((String) entry.get("old_values")).contains("Alice Smith");
            });

        String changedFields = auditRows.stream()
            .filter(entry -> "UPDATE".equals(entry.get("action")))
            .map(entry -> (String) entry.get("changed_fields"))
            .findFirst()
            .orElseThrow();

        Map<String, Object> changedMap = objectMapper.readValue(changedFields, new TypeReference<>() {
        });
        assertThat(changedMap.keySet()).containsExactlyInAnyOrder("fullName", "email");
        assertThat(changedMap).doesNotContainKeys("id", "password");

        var page = auditQueryService.find(
            AuditQueryCriteria.builder()
                .entityType("io.orion.audit.demo.customer.Customer")
                .entityId(String.valueOf(customerId))
                .actions(java.util.Set.of(AuditAction.INSERT, AuditAction.UPDATE, AuditAction.DELETE))
                .build(),
            AuditPageRequest.of(0, 10)
        );

        assertThat(page.content()).hasSize(3);
        assertThat(page.content().get(0).action()).isEqualTo(AuditAction.DELETE);
    }
}
