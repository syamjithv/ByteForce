package com.byteforce.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ByteForceFrameTest {

    @Test
    void frameCanBeConstructed() {
        assertNotNull(new ByteForceFrame());
    }
}
