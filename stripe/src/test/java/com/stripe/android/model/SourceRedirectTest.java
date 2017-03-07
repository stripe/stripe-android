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

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class SourceRedirectTest {

    static final String EXAMPLE_JSON_REDIRECT = "  {" +
            "\"return_url\": \"https://google.com\"," +
            "\"status\": \"succeeded\"," +
            "\"url\": \"examplecompany://redirect-link\"" +
            "}";

    static final Map<String, Object> EXAMPLE_MAP_REDIRECT =
            new HashMap<String, Object>() {{
                put("return_url", "https://google.com");
                put("status", "succeeded");
                put("url", "examplecompany://redirect-link");
            }};

    private SourceRedirect mSourceRedirect;

    @Before
    public void setup() {
        mSourceRedirect = SourceRedirect.fromString(EXAMPLE_JSON_REDIRECT);
        assertNotNull(mSourceRedirect);
    }

    @Test
    public void fromJsonString_backToJson_createsIdenticalElement() {
        try {
            JSONObject rawConversion = new JSONObject(EXAMPLE_JSON_REDIRECT);
            assertJsonEquals(rawConversion, mSourceRedirect.toJson());
        } catch (JSONException jsonException) {
            fail("Test Data failure: " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void fromJsonString_toMap_createsExpectedMap() {
        assertMapEquals(EXAMPLE_MAP_REDIRECT, mSourceRedirect.toMap());
    }
}
