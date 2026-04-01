package io.orion.audit.core.query;

/**
 * Public read-side API for stored audit records.
 */
public interface AuditQueryService {

    /**
     * Finds audit records using the supplied criteria and page request.
     *
     * @param criteria query filters, may be empty
     * @param pageRequest page and sorting options
     * @return matching records
     */
    AuditPageResult<AuditRecord> find(AuditQueryCriteria criteria, AuditPageRequest pageRequest);
}
