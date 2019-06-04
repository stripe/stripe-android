package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertJsonEquals;
import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class SourceRedirectTest {

    static final String EXAMPLE_JSON_REDIRECT = "  {" +
            "\"return_url\": \"https://google.com\"," +
            "\"status\": \"succeeded\"," +
            "\"url\": \"examplecompany://redirect-link\"" +
            "}";

    private static final Map<String, Object> EXAMPLE_MAP_REDIRECT =
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
    public void fromJsonString_backToJson_createsIdenticalElement() throws JSONException {
        assertJsonEquals(new JSONObject(EXAMPLE_JSON_REDIRECT), mSourceRedirect.toJson());
    }

    @Test
    public void fromJsonString_toMap_createsExpectedMap() {
        assertMapEquals(EXAMPLE_MAP_REDIRECT, mSourceRedirect.toMap());
    }

    @Test
    public void asStatus() {
        assertEquals(SourceRedirect.FAILED,
                SourceRedirect.asStatus("failed"));
        assertEquals(SourceRedirect.SUCCEEDED,
                SourceRedirect.asStatus("succeeded"));
        assertEquals(SourceRedirect.PENDING,
                SourceRedirect.asStatus("pending"));
        assertEquals(SourceRedirect.NOT_REQUIRED,
                SourceRedirect.asStatus("not_required"));
        assertNull(SourceRedirect.asStatus("something_else"));
    }
}
