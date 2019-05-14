package com.stripe.android;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test class for {@link ErrorParser}.
 */
public class ErrorParserTest {

    private static final String RAW_INVALID_REQUEST_ERROR = "" +
            "{\n" +
            "  \"error\": {\n" +
            "    \"message\" : \"The Stripe API is only accessible over HTTPS.  " +
            "Please see <https://stripe.com/docs> for more information.\",\n" +
            "    \"type\": \"invalid_request_error\"\n" +
            "  }\n" +
            "}";

    private static final String RAW_INCORRECT_FORMAT_ERROR = "" +
            "{\n" +
            "    \"message\" : \"The Stripe API is only accessible over HTTPS.  " +
            "Please see <https://stripe.com/docs> for more information.\",\n" +
            "    \"type\": \"invalid_request_error\"\n" +
            "}";

    private static final String RAW_ERROR_WITH_ALL_FIELDS = "" +
            "{\n" +
            "  \"error\": {\n" +
            "    \"code\": \"code_value\",\n" +
            "    \"param\": \"param_value\",\n" +
            "    \"charge\": \"charge_value\",\n" +
            "    \"decline_code\": \"card_declined\",\n" +
            "    \"message\": \"Your card was declined.\",\n" +
            "    \"type\": \"invalid_request_error\"\n" +
            "  }\n" +
            "}";

    @Test
    public void parseError_withInvalidRequestError_createsCorrectObject() {
        final StripeError parsedStripeError =
                ErrorParser.parseError(RAW_INVALID_REQUEST_ERROR);
        String errorMessage = "The Stripe API is only accessible over HTTPS.  " +
                "Please see <https://stripe.com/docs> for more information.";
        assertEquals(errorMessage, parsedStripeError.message);
        assertEquals("invalid_request_error", parsedStripeError.type);
        assertEquals("", parsedStripeError.param);
    }

    @Test
    public void parseError_withNoErrorMessage_addsInvalidResponseMessage() {
        final StripeError badStripeError =
                ErrorParser.parseError(RAW_INCORRECT_FORMAT_ERROR);
        assertEquals(ErrorParser.MALFORMED_RESPONSE_MESSAGE, badStripeError.message);
        assertNull(badStripeError.type);
    }

    @Test
    public void parseError_withAllFields_parsesAllFields() {
        final StripeError error = ErrorParser.parseError(RAW_ERROR_WITH_ALL_FIELDS);
        assertEquals("code_value", error.code);
        assertEquals("param_value", error.param);
        assertEquals("charge_value", error.charge);
        assertEquals("Your card was declined.", error.message);
        assertEquals("card_declined", error.declineCode);
        assertEquals("invalid_request_error", error.type);
    }
}
