package io.orion.audit.autoconfigure.query;

import io.orion.audit.autoconfigure.driver.AuditLogEntity;
import io.orion.audit.autoconfigure.driver.AuditLogRepository;
import io.orion.audit.autoconfigure.properties.AuditProperties;
import io.orion.audit.autoconfigure.support.AuditJdbcSupport;
import io.orion.audit.autoconfigure.support.AuditRecordMapper;
import io.orion.audit.core.model.AuditAction;
import io.orion.audit.core.query.AuditPageRequest;
import io.orion.audit.core.query.AuditPageResult;
import io.orion.audit.core.query.AuditQueryCriteria;
import io.orion.audit.core.query.AuditQueryService;
import io.orion.audit.core.query.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default database-backed audit query implementation.
 */
public class DatabaseAuditQueryService implements AuditQueryService {

    private final AuditLogRepository repository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuditProperties properties;
    private final AuditRecordMapper mapper;

    public DatabaseAuditQueryService(
        AuditLogRepository repository,
        NamedParameterJdbcTemplate jdbcTemplate,
        AuditProperties properties,
        AuditRecordMapper mapper
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public AuditPageResult<AuditRecord> find(AuditQueryCriteria criteria, AuditPageRequest pageRequest) {
        AuditQueryCriteria effectiveCriteria = criteria == null ? AuditQueryCriteria.builder().build() : criteria;
        AuditPageRequest effectivePageRequest = pageRequest == null ? AuditPageRequest.of(0, 20) : pageRequest;
        if (usesRepository()) {
            return queryWithRepository(effectiveCriteria, effectivePageRequest);
        }
        return queryWithJdbc(effectiveCriteria, effectivePageRequest);
    }

    private boolean usesRepository() {
        return repository != null && "audit_log".equals(properties.getTableName()) && (properties.getSchema() == null || properties.getSchema().isBlank());
    }

    private AuditPageResult<AuditRecord> queryWithRepository(AuditQueryCriteria criteria, AuditPageRequest pageRequest) {
        Sort.Direction direction = pageRequest.getDirection() == AuditPageRequest.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<AuditLogEntity> page = repository.findAll(specification(criteria), PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), Sort.by(direction, toEntityProperty(pageRequest.getSortBy()))));
        return new AuditPageResult<>(
            page.getContent().stream().map(mapper::map).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    private AuditPageResult<AuditRecord> queryWithJdbc(AuditQueryCriteria criteria, AuditPageRequest pageRequest) {
        String tableName = AuditJdbcSupport.qualifiedTableName(properties);
        StringBuilder where = new StringBuilder(" where 1=1");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        applyCriteria(criteria, where, parameters);

        String orderBy = toColumnName(pageRequest.getSortBy()) + " " + pageRequest.getDirection().name();
        parameters.addValue("limit", pageRequest.getSize(), Types.INTEGER);
        parameters.addValue("offset", pageRequest.getPage() * pageRequest.getSize(), Types.INTEGER);

        List<AuditRecord> content = jdbcTemplate.query(
            "select id, action, entity_type, entity_name, entity_id, old_values, new_values, changed_fields, actor_id, actor_name, actor_type, ip_address, user_agent, request_uri, http_method, trace_id, source, tenant_id, tags, created_at "
                + "from " + tableName + where + " order by " + orderBy + " limit :limit offset :offset",
            parameters,
            (rs, rowNum) -> mapper.map(rs)
        );
        Long total = jdbcTemplate.queryForObject("select count(*) from " + tableName + where, parameters, Long.class);
        long totalElements = total == null ? 0L : total;
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageRequest.getSize());
        return new AuditPageResult<>(content, pageRequest.getPage(), pageRequest.getSize(), totalElements, totalPages);
    }

    private void applyCriteria(AuditQueryCriteria criteria, StringBuilder where, MapSqlParameterSource parameters) {
        if (hasText(criteria.getEntityType())) {
            where.append(" and entity_type = :entityType");
            parameters.addValue("entityType", criteria.getEntityType());
        }
        if (hasText(criteria.getEntityId())) {
            where.append(" and entity_id = :entityId");
            parameters.addValue("entityId", criteria.getEntityId());
        }
        if (!criteria.getActions().isEmpty()) {
            where.append(" and action in (:actions)");
            parameters.addValue("actions", criteria.getActions().stream().map(AuditAction::name).toList());
        }
        if (criteria.getCreatedFrom() != null) {
            where.append(" and created_at >= :createdFrom");
            parameters.addValue("createdFrom", criteria.getCreatedFrom());
        }
        if (criteria.getCreatedTo() != null) {
            where.append(" and created_at <= :createdTo");
            parameters.addValue("createdTo", criteria.getCreatedTo());
        }
        if (hasText(criteria.getActorId())) {
            where.append(" and actor_id = :actorId");
            parameters.addValue("actorId", criteria.getActorId());
        }
        if (hasText(criteria.getActorType())) {
            where.append(" and actor_type = :actorType");
            parameters.addValue("actorType", criteria.getActorType());
        }
        if (hasText(criteria.getSource())) {
            where.append(" and source = :source");
            parameters.addValue("source", criteria.getSource());
        }
        if (hasText(criteria.getTenantId())) {
            where.append(" and tenant_id = :tenantId");
            parameters.addValue("tenantId", criteria.getTenantId());
        }
    }

    private Specification<AuditLogEntity> specification(AuditQueryCriteria criteria) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (hasText(criteria.getEntityType())) {
                predicates.add(cb.equal(root.get("entityType"), criteria.getEntityType()));
            }
            if (hasText(criteria.getEntityId())) {
                predicates.add(cb.equal(root.get("entityId"), criteria.getEntityId()));
            }
            if (!criteria.getActions().isEmpty()) {
                predicates.add(root.get("action").in(criteria.getActions().stream().map(AuditAction::name).toList()));
            }
            if (criteria.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedFrom()));
            }
            if (criteria.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedTo()));
            }
            if (hasText(criteria.getActorId())) {
                predicates.add(cb.equal(root.get("actorId"), criteria.getActorId()));
            }
            if (hasText(criteria.getActorType())) {
                predicates.add(cb.equal(root.get("actorType"), criteria.getActorType()));
            }
            if (hasText(criteria.getSource())) {
                predicates.add(cb.equal(root.get("source"), criteria.getSource()));
            }
            if (hasText(criteria.getTenantId())) {
                predicates.add(cb.equal(root.get("tenantId"), criteria.getTenantId()));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private String toEntityProperty(String sortBy) {
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("createdAt", "createdAt");
        mappings.put("action", "action");
        mappings.put("entityType", "entityType");
        mappings.put("entityId", "entityId");
        mappings.put("actorId", "actorId");
        mappings.put("actorType", "actorType");
        mappings.put("source", "source");
        mappings.put("tenantId", "tenantId");
        return mappings.getOrDefault(sortBy, "createdAt");
    }

    private String toColumnName(String sortBy) {
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("createdAt", "created_at");
        mappings.put("action", "action");
        mappings.put("entityType", "entity_type");
        mappings.put("entityId", "entity_id");
        mappings.put("actorId", "actor_id");
        mappings.put("actorType", "actor_type");
        mappings.put("source", "source");
        mappings.put("tenantId", "tenant_id");
        return mappings.getOrDefault(sortBy, "created_at");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
