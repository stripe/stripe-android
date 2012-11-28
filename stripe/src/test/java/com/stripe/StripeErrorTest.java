package com.stripe;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class StripeErrorTest {
    private HashMap<String, Object> errorMap;
    private final String DEV_MESSAGE = "This is a dev message";

    @Before
    public void setup() {
        errorMap = new HashMap<String, Object>();
    }

    @Test
    public void fromJSON_should_give_unkown_error_if_type_missing() {
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.UNKNOWN_ERROR, stripeError);
    }

    @Test
    public void fromJSON_should_give_unkown_error_if_message_missing() {
        errorMap.put("type", "api_error");
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.UNKNOWN_ERROR, stripeError);
    }

    @Test
    public void fromJSON_should_camel_case_param() {
        errorMap.put("type", "api_error");
        errorMap.put("message", DEV_MESSAGE);
        errorMap.put("param", "exp_month");
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(stripeError.parameter, "expMonth");
    }

    @Test
    public void fromJSON_should_interpret_api_error() {
        errorMap.put("type", "api_error");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.APIError, stripeError.errorCode);
        assertEquals(StripeError.MessageKeyUnexpectedError, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_invalid_request() {
        errorMap.put("type", "invalid_request_error");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.InvalidRequestError, stripeError.errorCode);
        assertEquals(StripeError.MessageKeyInvalidRequest, stripeError.errorMessageKey);
    }

    @Test
    public void fromJSON_should_interpret_card_error_with_unexpected_problem() {
        errorMap.put("type", "card_error");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.UnexpectedError, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyUnexpectedError, stripeError.errorMessageKey);
    }

    @Test
    public void fromJSON_should_interpret_card_error_with_incorrect_number() {
        errorMap.put("type", "card_error");
        errorMap.put("code", "incorrect_number");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidNumber, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyInvalidNumber, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_card_error_with_invalid_number() {
        errorMap.put("type", "card_error");
        errorMap.put("code", "invalid_number");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidNumber, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyInvalidNumber, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_card_error_with_invalid_cvc() {
        errorMap.put("type", "card_error");
        errorMap.put("code", "invalid_cvc");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidCVC, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyInvalidCVC, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_card_error_with_incorrect_cvc() {
        errorMap.put("type", "card_error");
        errorMap.put("code", "incorrect_cvc");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidCVC, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyInvalidCVC, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_card_error_with_invalid_exp_month() {
        errorMap.put("type", "card_error");
        errorMap.put("code", "invalid_expiry_month");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidExpMonth, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyInvalidExpMonth, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_card_error_with_invalid_exp_year() {
        errorMap.put("type", "card_error");
        errorMap.put("code", "invalid_expiry_year");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidExpYear, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyInvalidExpYear, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_card_error_with_expired() {
        errorMap.put("type", "card_error");
        errorMap.put("code", "expired_card");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.ExpiredCard, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyExpiredCard, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_card_error_with_declined() {
        errorMap.put("type", "card_error");
        errorMap.put("code", "card_declined");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.CardDeclined, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyDeclined, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_card_error_with_processing_error() {
        errorMap.put("type", "card_error");
        errorMap.put("code", "processing_error");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.ProcessingError, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyProcessingError, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_missing_number() {
        errorMap.put("type", "card_error");
        errorMap.put("code", null);
        errorMap.put("param", "number");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidNumber, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyMissingNumber, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_missing_month() {
        errorMap.put("type", "card_error");
        errorMap.put("code", null);
        errorMap.put("param", "exp_month");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidExpMonth, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyMissingExpMonth, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_missing_year() {
        errorMap.put("type", "card_error");
        errorMap.put("code", null);
        errorMap.put("param", "exp_year");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidExpYear, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyMissingExpYear, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    @Test
    public void fromJSON_should_interpret_missing_cvc() {
        errorMap.put("type", "card_error");
        errorMap.put("code", null);
        errorMap.put("param", "cvc");
        errorMap.put("message", DEV_MESSAGE);
        StripeError stripeError = StripeError.fromJSON(getJSON(errorMap));
        assertEquals(StripeError.StripeErrorCode.CardError, stripeError.errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidCVC, stripeError.cardErrorCode);
        assertEquals(StripeError.MessageKeyMissingCVC, stripeError.errorMessageKey);
        assertEquals(DEV_MESSAGE, stripeError.developerMessage);
    }

    public JSONObject getJSON(Map<String, Object> errorMap) {
        HashMap<String, Object> errorWrapper = new HashMap<String, Object>();
        errorWrapper.put("error", new JSONObject(errorMap));
        return new JSONObject(errorWrapper);
    }

}
