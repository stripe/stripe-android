package com.stripe.android.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for {@link SourceCodeVerification}.
 */
public class SourceCodeVerificationTest {

    static final String EXAMPLE_JSON_CODE_VERIFICATION = "{" +
            "\"attempts_remaining\": 3," +
            "\"status\": \"pending\"" +
            "}";

    @Test
    public void fromJsonString_createsObject() {
        final SourceCodeVerification codeVerification =
                SourceCodeVerification.fromString(EXAMPLE_JSON_CODE_VERIFICATION);
        assertNotNull(codeVerification);
        assertEquals(3, codeVerification.getAttemptsRemaining());
        assertEquals(SourceCodeVerification.Status.PENDING, codeVerification.getStatus());
    }
}
