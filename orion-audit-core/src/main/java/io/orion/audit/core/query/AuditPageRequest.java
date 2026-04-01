package io.orion.audit.core.query;

import java.util.Objects;

/**
 * Page request abstraction for reading audit records.
 */
public final class AuditPageRequest {

    public enum Direction {
        ASC,
        DESC
    }

    private final int page;
    private final int size;
    private final String sortBy;
    private final Direction direction;

    private AuditPageRequest(int page, int size, String sortBy, Direction direction) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        this.page = page;
        this.size = size;
        this.sortBy = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy;
        this.direction = direction == null ? Direction.DESC : direction;
    }

    public static AuditPageRequest of(int page, int size) {
        return new AuditPageRequest(page, size, "createdAt", Direction.DESC);
    }

    public static AuditPageRequest of(int page, int size, String sortBy, Direction direction) {
        return new AuditPageRequest(page, size, sortBy, direction);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuditPageRequest that)) {
            return false;
        }
        return page == that.page
            && size == that.size
            && Objects.equals(sortBy, that.sortBy)
            && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size, sortBy, direction);
    }
}
