package com.stripe.android.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ObjectUtilsTest {

    @Test
    public void getOrDefault() {
        assertEquals("default",
                ObjectUtils.getOrDefault(null, "default"));

        assertEquals("value",
                ObjectUtils.getOrDefault("value", "default"));
    }
}
