package com.stripe.android;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test class for {@link ErrorParser}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class ErrorParserTest {

    private static final String RAW_INVALID_REQUEST_ERROR =
            "{\n" +
                    "  \"error\": {\n" +
                    "    \"message\" : \"The Stripe API is only accessible over HTTPS.  " +
                    "Please see <https://stripe.com/docs> for more information.\",\n" +
                    "    \"type\": \"invalid_request_error\"\n" +
                    "  }\n" +
                    "}";

    private static final String RAW_INCORRECT_FORMAT_ERROR =
            "{\n" +
                    "    \"message\" : \"The Stripe API is only accessible over HTTPS.  " +
                    "Please see <https://stripe.com/docs> for more information.\",\n" +
                    "    \"type\": \"invalid_request_error\"\n" +
                    "}";

    @Test
    public void parseError_withInvalidRequestError_createsCorrectObject() {
        ErrorParser.StripeError parsedStripeError = ErrorParser.parseError(RAW_INVALID_REQUEST_ERROR);
        String errorMessage = "The Stripe API is only accessible over HTTPS.  " +
                "Please see <https://stripe.com/docs> for more information.";
        assertEquals(errorMessage, parsedStripeError.message);
        assertEquals("invalid_request_error", parsedStripeError.type);
        assertEquals("", parsedStripeError.param);
    }

    @Test
    public void parseError_withNoErrorMessage_addsInvalidResponseMessage() {
        ErrorParser.StripeError badStripeError = ErrorParser.parseError(RAW_INCORRECT_FORMAT_ERROR);
        assertEquals(ErrorParser.MALFORMED_RESPONSE_MESSAGE, badStripeError.message);
        assertNull(badStripeError.type);
    }
}
