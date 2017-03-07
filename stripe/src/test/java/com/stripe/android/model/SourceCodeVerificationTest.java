package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertJsonEquals;
import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link SourceCodeVerification}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class SourceCodeVerificationTest {

    static final String EXAMPLE_JSON_CODE_VERIFICATION = "{" +
            "\"attempts_remaining\": 3," +
            "\"status\": \"pending\"" +
            "}";

    static final Map<String, Object> EXAMPLE_MAP_CODE_VERIFICATION =
            new HashMap<String, Object>() {{
                put("attempts_remaining", 3);
                put("status", "pending");
            }};

    private SourceCodeVerification mCodeVerification;

    @Before
    public void setup() {
        mCodeVerification = SourceCodeVerification.fromString(EXAMPLE_JSON_CODE_VERIFICATION);
        assertNotNull(mCodeVerification);
    }

    @Test
    public void fromJsonString_backToJson_createsIdenticalElement() {
        try {
            JSONObject rawConversion = new JSONObject(EXAMPLE_JSON_CODE_VERIFICATION);
            assertJsonEquals(rawConversion, mCodeVerification.toJson());
        } catch (JSONException jsonException) {
            fail("Test Data failure: " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void fromJsonString_toMap_createsExpectedMap() {
        assertMapEquals(EXAMPLE_MAP_CODE_VERIFICATION, mCodeVerification.toMap());
    }
}
