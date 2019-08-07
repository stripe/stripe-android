package com.stripe.android.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SourceRedirectTest {

    static final String EXAMPLE_JSON_REDIRECT = "  {" +
            "\"return_url\": \"https://google.com\"," +
            "\"status\": \"succeeded\"," +
            "\"url\": \"examplecompany://redirect-link\"" +
            "}";

    @Before
    public void setup() {
        assertNotNull(SourceRedirect.fromString(EXAMPLE_JSON_REDIRECT));
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
