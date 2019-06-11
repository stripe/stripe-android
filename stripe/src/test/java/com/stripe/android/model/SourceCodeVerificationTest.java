package com.stripe.android.model;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for {@link SourceCodeVerification}.
 */
public class SourceCodeVerificationTest {

    static final String EXAMPLE_JSON_CODE_VERIFICATION = "{" +
            "\"attempts_remaining\": 3," +
            "\"status\": \"pending\"" +
            "}";

    private static final Map<String, Object> EXAMPLE_MAP_CODE_VERIFICATION =
            new HashMap<String, Object>() {{
                put("attempts_remaining", 3);
                put("status", "pending");
            }};

    @Test
    public void fromJsonString_toMap_createsExpectedMap() {
        final SourceCodeVerification codeVerification =
                SourceCodeVerification.fromString(EXAMPLE_JSON_CODE_VERIFICATION);
        assertNotNull(codeVerification);
        assertMapEquals(EXAMPLE_MAP_CODE_VERIFICATION, codeVerification.toMap());
    }
}
