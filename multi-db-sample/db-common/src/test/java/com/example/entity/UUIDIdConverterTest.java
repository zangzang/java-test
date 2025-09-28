package com.example.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UUIDIdConverterTest {
    @Test
    void basicConversion() {
        UUIDIdConverter conv = new UUIDIdConverter();
        UUID uuid = conv.fromLong(123L);
        assertNotNull(uuid);
        long back = conv.toLong(uuid);
        assertEquals(123L, back);
    }
}
