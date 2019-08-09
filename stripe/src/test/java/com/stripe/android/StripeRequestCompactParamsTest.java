package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StripeRequestCompactParamsTest {
    @Test
    public void compatParams_removesNullParams() {
        final AbstractMap<String, Object> params = new HashMap<>();
        params.put("a", null);
        params.put("b", "not null");

        final Map<String, ?> compactParams = getCompactedParams(params);
        assertEquals(1, compactParams.size());
        assertTrue(compactParams.containsKey("b"));
    }

    @Test
    public void compatParams_removesEmptyStringParams() {
        final Map<String, Object> params = new HashMap<>();
        params.put("a", "fun param");
        params.put("b", "not null");
        params.put("c", "");

        final Map<String, ?> compactParams = getCompactedParams(params);
        assertEquals(2, compactParams.size());
        assertTrue(compactParams.containsKey("a"));
        assertTrue(compactParams.containsKey("b"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void compatParams_removesNestedEmptyParams() {
        final Map<String, ?> outParams = getCompactedParams(createParamsWithNestedMap());

        assertEquals(3, outParams.size());
        assertTrue(outParams.containsKey("a"));
        assertTrue(outParams.containsKey("b"));
        assertTrue(outParams.containsKey("c"));

        final Map<String, Object> firstNestedMap =
                Objects.requireNonNull((Map<String, Object>) outParams.get("c"));
        assertEquals(2, firstNestedMap.size());
        assertTrue(firstNestedMap.containsKey("1a"));
        assertTrue(firstNestedMap.containsKey("1c"));

        final Map<String, Object> secondNestedMap =
                Objects.requireNonNull((Map<String, Object>) firstNestedMap.get("1c"));
        assertEquals(1, secondNestedMap.size());
        assertTrue(secondNestedMap.containsKey("2b"));
    }

    @NonNull
    private Map<String, Object> createParamsWithNestedMap() {
        final Map<String, Object> inParams = new HashMap<>();
        final AbstractMap<String, Object> firstNestedMap = new HashMap<>();
        final Map<String, Object> secondNestedMap = new HashMap<>();
        inParams.put("a", "fun param");
        inParams.put("b", "not null");
        firstNestedMap.put("1a", "something");
        firstNestedMap.put("1b", null);
        secondNestedMap.put("2a", "");
        secondNestedMap.put("2b", "hello world");
        firstNestedMap.put("1c", secondNestedMap);
        inParams.put("c", firstNestedMap);
        return inParams;
    }

    @NonNull
    private Map<String, ?> getCompactedParams(@NonNull Map<String, Object> params) {
        return Objects.requireNonNull(new FakeRequest(params).params);
    }

    private static final class FakeRequest extends StripeRequest {
        FakeRequest(@Nullable Map<String, ?> params) {
            super(Method.POST, "https://example.com", params,
                    "application/x-www-form-urlencoded");
        }

        @NonNull
        @Override
        Map<String, String> createHeaders() {
            return Collections.emptyMap();
        }

        @NonNull
        @Override
        String getUserAgent() {
            return "";
        }

        @NonNull
        @Override
        byte[] getOutputBytes() {
            return new byte[0];
        }
    }
}
