package io.orion.audit.core.query;

import java.util.List;

/**
 * Page result for querying audit records.
 *
 * @param content records in the current page
 * @param page current page index
 * @param size page size
 * @param totalElements total matching record count
 * @param totalPages total number of pages
 */
public record AuditPageResult<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
