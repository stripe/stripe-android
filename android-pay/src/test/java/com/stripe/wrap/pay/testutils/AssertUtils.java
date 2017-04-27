package com.stripe.wrap.pay.testutils;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Utility functions for tests.
 */
public class AssertUtils {

    public static void assertEmpty(List list) {
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    public static void assertEmpty(Map map) {
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }
}
