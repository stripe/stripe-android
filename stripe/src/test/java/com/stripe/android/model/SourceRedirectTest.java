package com.stripe.android.model;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    public void fromJsonString_toMap_createsExpectedMap() {
        assertMapEquals(EXAMPLE_MAP_REDIRECT, mSourceRedirect.toMap());
    }

    @Test
    public void asStatus() {
        assertEquals(SourceRedirect.Status.FAILED,
                SourceRedirect.asStatus("failed"));
        assertEquals(SourceRedirect.Status.SUCCEEDED,
                SourceRedirect.asStatus("succeeded"));
        assertEquals(SourceRedirect.Status.PENDING,
                SourceRedirect.asStatus("pending"));
        assertEquals(SourceRedirect.Status.NOT_REQUIRED,
                SourceRedirect.asStatus("not_required"));
        assertNull(SourceRedirect.asStatus("something_else"));
    }
}
