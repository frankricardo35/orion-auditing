package io.orion.audit.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@SpringBootTest(properties = "orion.audit.listener-mode=jpa")
@AutoConfigureMockMvc
class JpaListenerModeDemoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPreserveJpaFallbackAuditingFlow() throws Exception {
        String response = mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"password\":\"secret\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        long customerId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(put("/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Bob Updated\",\"email\":\"bob@example.com\",\"password\":\"secret-2\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/customers/{id}", customerId))
            .andExpect(status().isNoContent());

        List<Map<String, Object>> auditRows = jdbcTemplate.queryForList(
            "select action, changed_fields from audit_log where entity_id = ? order by created_at asc",
            String.valueOf(customerId)
        );

        assertThat(auditRows).hasSize(3);
        assertThat(auditRows).anySatisfy(row -> {
            assertThat(row.get("action")).isEqualTo("UPDATE");
            assertThat((String) row.get("changed_fields")).contains("Bob Updated");
            assertThat((String) row.get("changed_fields")).doesNotContain("secret-2");
        });
    }
}
