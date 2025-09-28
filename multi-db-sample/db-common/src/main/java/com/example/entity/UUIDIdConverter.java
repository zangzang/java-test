package com.example.entity;

import java.util.UUID;

/**
 * Simple IdConverter implementation that converts between Long and UUID using UUID.fromString
 * or by embedding the numeric value in the UUID's least-significant bits. This is a sample
 * approach — in real systems you may persist UUIDs directly as UUID columns.
 */
public class UUIDIdConverter implements GenericJooqUserDao.IdConverter<UUID> {
    public UUIDIdConverter() {
    }

    @Override
    public UUID fromLong(Long id) {
        // simple conversion: put id into least significant bits
        return new UUID(0L, id == null ? 0L : id.longValue());
    }

    @Override
    public long toLong(UUID value) {
        if (value == null) return 0L;
        return value.getLeastSignificantBits();
    }
}
