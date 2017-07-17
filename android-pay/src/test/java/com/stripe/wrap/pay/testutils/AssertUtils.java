package com.stripe.wrap.pay.testutils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.identity.intents.model.CountrySpecification;

import org.assertj.core.util.Sets;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

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

    public static void assertContainsEqual(
            @NonNull Object desired,
            @NonNull Set set) {
        for (Object o : set) {
            if (o.equals(desired)) {
                return;
            }

            if (o instanceof CountrySpecification
                    && desired instanceof CountrySpecification
                    && countriesEqual(
                    (CountrySpecification) o,
                    (CountrySpecification) desired)) {
                return;
            }
        }
        fail(String.format(Locale.ENGLISH,
                "Object %s not found in list",
                desired.toString()));
    }

    /**
     * Currently, two "equal" but not identical {@link CountrySpecification} objects
     * do not pass the .equals test. They should, but since they don't, we
     * need to check this manually.
     *
     * @param expected a {@link CountrySpecification}
     * @param actual another {@link CountrySpecification}
     * @return {@code true} if the inputs specify the same country
     */
    public static boolean countriesEqual(
            @NonNull CountrySpecification expected,
            @NonNull CountrySpecification actual) {
        return expected.getCountryCode().equals(actual.getCountryCode());
    }

    public static void assertListEquals(
            @Nullable List expected,
            @Nullable List actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }

        if (actual == null) {
            fail("List should not be null");
            return;
        }

        assertEquals("List sizes should be equal", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(String.format(Locale.ENGLISH, "Lists should be equal at index %d", i),
                    expected.get(i),
                    actual.get(i));
        }
    }

    @SuppressWarnings("unchecked")
    public static void assertUnorderedListEquals(
            @Nullable List expected,
            @Nullable List actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }

        if (actual == null) {
            fail("List should not be null");
            return;
        }

        assertEquals("List sizes should be equal", expected.size(), actual.size());
        Set expectedSet = new HashSet(expected);
        Set actualSet = new HashSet(actual);
        assertEquals("Lists should have the same number of object types",
                expectedSet.size(),
                actualSet.size());
        for (Object o: expectedSet) {
            assertContainsEqual(o, actualSet);
        }
    }
}
