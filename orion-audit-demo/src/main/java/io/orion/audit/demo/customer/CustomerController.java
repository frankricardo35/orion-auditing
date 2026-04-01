package io.orion.audit.demo.customer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class CustomerController {

    private final CustomerService customerService;
    private final JdbcTemplate jdbcTemplate;

    public CustomerController(CustomerService customerService, JdbcTemplate jdbcTemplate) {
        this.customerService = customerService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    public Customer create(@RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/customers/{id}")
    public Customer update(@PathVariable Long id, @RequestBody UpdateCustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/customers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }

    @GetMapping("/audit-logs")
    public List<AuditRecordResponse> auditLogs() {
        return jdbcTemplate.query("""
            SELECT id, action, entity_type, entity_id, old_values, new_values, changed_fields
            FROM audit_log
            ORDER BY created_at ASC
            """, (rs, rowNum) -> new AuditRecordResponse(
            rs.getString("id"),
            rs.getString("action"),
            rs.getString("entity_type"),
            rs.getString("entity_id"),
            rs.getString("old_values"),
            rs.getString("new_values"),
            rs.getString("changed_fields")
        ));
    }

    public record AuditRecordResponse(
        String id,
        String action,
        String entityType,
        String entityId,
        String oldValues,
        String newValues,
        String changedFields
    ) {
    }
}
