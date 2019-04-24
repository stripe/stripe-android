package com.stripe.android.model;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertJsonEqualsExcludingNulls;
import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class PaymentIntentTest {

    private static final String PAYMENT_INTENT_WITH_SOURCE_JSON = "{\n" +
            "  \"id\": \"pi_1CkiBMLENEVhOs7YMtUehLau\",\n" +
            "  \"object\": \"payment_intent\",\n" +
            "  \"payment_method_types\": [\n" +
            "    \"card\"\n" +
            "  ],\n" +
            "  \"amount\": 1000,\n" +
            "  \"canceled_at\": 1530839340,\n" +
            "  \"capture_method\": \"automatic\",\n" +
            "  \"client_secret\": \"pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA\",\n" +
            "  \"confirmation_method\": \"publishable\",\n" +
            "  \"created\": 1530838340,\n" +
            "  \"currency\": \"usd\",\n" +
            "  \"description\": \"Example PaymentIntent charge\",\n" +
            "  \"livemode\": false,\n" +
            "  \"next_source_action\": null,\n" +
            "  \"receipt_email\": null,\n" +
            "  \"shipping\": null,\n" +
            "  \"source\": \"src_1CkiC3LENEVhOs7YMSa4yx4G\",\n" +
            "  \"status\": \"succeeded\"\n" +
            "}\n";

    private static final String BAD_URL = "nonsense-blahblah";

    private static final String PAYMENT_INTENT_WITH_SOURCE_WITH_BAD_AUTH_URL_JSON = "{\n" +
            "  \"id\": \"pi_1CkiBMLENEVhOs7YMtUehLau\",\n" +
            "  \"object\": \"payment_intent\",\n" +
            "  \"amount\": 1000,\n" +
            "  \"canceled_at\": 1530839340,\n" +
            "  \"capture_method\": \"automatic\",\n" +
            "  \"client_secret\": \"pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA\",\n" +
            "  \"confirmation_method\": \"publishable\",\n" +
            "  \"created\": 1530838340,\n" +
            "  \"currency\": \"usd\",\n" +
            "  \"description\": \"Example PaymentIntent charge\",\n" +
            "  \"livemode\": false,\n" +
            "  \"next_source_action\": {" +
            "       \"type\": \"authorize_with_url\"," +
            "           \"authorize_with_url\": {" +
            "             \"url\": \""+ BAD_URL +"\"," +
            "             \"return_url\": \"yourapp://post-authentication-return-url\"" +
            "           } " +
            "       },\n" +
            "  \"receipt_email\": null,\n" +
            "  \"shipping\": null,\n" +
            "  \"source\": \"src_1CkiC3LENEVhOs7YMSa4yx4G\",\n" +
            "  \"status\": \"requires_source_action\"\n" +
            "}\n";

    private static final String PAYMENT_INTENT_WITH_PAYMENT_METHODS_JSON = "{\n" +
            "  \"id\": \"pi_Aabcxyz01aDfoo\",\n" +
            "  \"object\": \"payment_intent\",\n" +
            "  \"amount\": 750,\n" +
            "  \"amount_capturable\": 0,\n" +
            "  \"amount_received\": 750,\n" +
            "  \"application\": null,\n" +
            "  \"application_fee_amount\": null,\n" +
            "  \"canceled_at\": null,\n" +
            "  \"cancellation_reason\": null,\n" +
            "  \"capture_method\": \"automatic\",\n" +
            "  \"charges\": {\n" +
            "    \"object\": \"list\",\n" +
            "    \"data\": [],\n" +
            "    \"has_more\": false,\n" +
            "    \"total_count\": 0,\n" +
            "    \"url\": \"/v1/charges?payment_intent=pi_Aabcxyz01aDfoo\"\n" +
            "  },\n" +
            "  \"client_secret\": null,\n" +
            "  \"confirmation_method\": \"publishable\",\n" +
            "  \"created\": 123456789,\n" +
            "  \"currency\": \"usd\",\n" +
            "  \"customer\": null,\n" +
            "  \"description\": \"PaymentIntent Description\",\n" +
            "  \"last_payment_error\": null,\n" +
            "  \"livemode\": false,\n" +
            "  \"metadata\": {\n" +
            "    \"order_id\": \"123456789\"\n" +
            "  },\n" +
            "  \"next_action\": null,\n" +
            "  \"on_behalf_of\": null,\n" +
            "  \"payment_method\": null,\n" +
            "  \"payment_method_types\": [\n" +
            "    \"card\"\n" +
            "  ],\n" +
            "  \"receipt_email\": \"jenny@example.com\",\n" +
            "  \"review\": null,\n" +
            "  \"shipping\": {\n" +
            "    \"address\": {\n" +
            "      \"city\": \"Stockholm\",\n" +
            "      \"country\": \"Sweden\",\n" +
            "      \"line1\": \"Mega street 5\",\n" +
            "      \"line2\": \"Mega street 5\",\n" +
            "      \"postal_code\": \"12233JJHH\",\n" +
            "      \"state\": \"NYC\"\n" +
            "    },\n" +
            "    \"carrier\": null,\n" +
            "    \"name\": \"Mohit  Name\",\n" +
            "    \"phone\": null,\n" +
            "    \"tracking_number\": null\n" +
            "  },\n" +
            "  \"source\": \"src_1E884r2eZvKYlo2CTft0qEyY\",\n" +
            "  \"statement_descriptor\": \"PaymentIntent Statement Descriptor\",\n" +
            "  \"status\": \"succeeded\",\n" +
            "  \"transfer_data\": null,\n" +
            "  \"transfer_group\": null\n" +
            "}";

    private static final String PARTIAL_PAYMENT_INTENT_WITH_REDIRECT_URL_JSON = "{\n" +
            "\t\"id\": \"pi_Aabcxyz01aDfoo\",\n" +
            "\t\"object\": \"payment_intent\",\n" +
            "\t\"status\": \"requires_action\",\n" +
            "\t\"next_action\": {\n" +
            "\t\t\"type\": \"redirect_to_url\",\n" +
            "\t\t\"redirect_to_url\": {\n" +
            "\t\t\t\"url\": \"https://example.com/redirect\",\n" +
            "\t\t\t\"return_url\": \"yourapp://post-authentication-return-url\"\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    private static final String PARTIAL_PAYMENT_INTENT_WITH_AUTHORIZE_WITH_URL_JSON = "{\n" +
            "\t\"id\": \"pi_Aabcxyz01aDfoo\",\n" +
            "\t\"object\": \"payment_intent\",\n" +
            "\t\"status\": \"requires_action\",\n" +
            "\t\"next_action\": {\n" +
            "\t\t\"type\": \"authorize_with_url\",\n" +
            "\t\t\"authorize_with_url\": {\n" +
            "\t\t\t\"url\": \"https://example.com/redirect\",\n" +
            "\t\t\t\"return_url\": \"yourapp://post-authentication-return-url\"\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    private static final List<String> PAYMENT_METHOD_TYPES = new ArrayList<String>() {{
        add("card");
    }};

    private static final Map<String, Object> PAYMENT_INTENT_WITH_SOURCE_MAP =
            new HashMap<String, Object>() {{
                put(PaymentIntent.FIELD_ID, "pi_1CkiBMLENEVhOs7YMtUehLau");
                put(PaymentIntent.FIELD_OBJECT, "payment_intent");
                put(PaymentIntent.FIELD_PAYMENT_METHOD_TYPES, PAYMENT_METHOD_TYPES);
                put(PaymentIntent.FIELD_AMOUNT, 1000L);
                put(PaymentIntent.FIELD_CANCELED, 1530839340L);
                put(PaymentIntent.FIELD_CLIENT_SECRET,
                        "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA");
                put(PaymentIntent.FIELD_CONFIRMATION_METHOD, "publishable");
                put(PaymentIntent.FIELD_CREATED, 1530838340L);
                put(PaymentIntent.FIELD_CURRENCY, "usd");
                put(PaymentIntent.FIELD_DESCRIPTION, "Example PaymentIntent charge");
                put(PaymentIntent.FIELD_LIVEMODE, false);
                put(PaymentIntent.FIELD_SOURCE, "src_1CkiC3LENEVhOs7YMSa4yx4G");
                put(PaymentIntent.FIELD_CAPTURE_METHOD, "automatic");
                put(PaymentIntent.FIELD_STATUS, "succeeded");
            }};

    private static final PaymentIntent PAYMENT_INTENT_WITH_SOURCE = PaymentIntent
            .fromString(PAYMENT_INTENT_WITH_SOURCE_JSON);

    @Test
    public void fromJsonString_backToJson_createsIdenticalElement() throws JSONException {
        assertNotNull(PAYMENT_INTENT_WITH_SOURCE);
        JSONObject rawConversion = new JSONObject(PAYMENT_INTENT_WITH_SOURCE_JSON);
        JSONObject actualObject = PAYMENT_INTENT_WITH_SOURCE.toJson();
        assertJsonEqualsExcludingNulls(rawConversion, actualObject);
    }

    @Test
    public void fromJsonString_toMap_createsExpectedMap() {
        assertNotNull(PAYMENT_INTENT_WITH_SOURCE);
        final Map<String, Object> paymentIntentMap = PAYMENT_INTENT_WITH_SOURCE.toMap();
        assertMapEquals(paymentIntentMap, PAYMENT_INTENT_WITH_SOURCE_MAP);
    }

    @Test
    public void getAuthorizationUrl_whenProvidedBadUrl_doesNotCrash() {
        final PaymentIntent paymentIntent = PaymentIntent.fromString(
                PAYMENT_INTENT_WITH_SOURCE_WITH_BAD_AUTH_URL_JSON);
        assertNotNull(paymentIntent);

        final Uri authUrl = paymentIntent.getAuthorizationUrl();
        assertNotNull(authUrl);

        assertEquals(BAD_URL, authUrl.getEncodedPath());
    }

    @Test
    public void getRedirectUrl_withRedirectToUrlPopulate_returnsRedirectUrl() {
        final PaymentIntent paymentIntent = PaymentIntent
                .fromString(PARTIAL_PAYMENT_INTENT_WITH_REDIRECT_URL_JSON);
        assertNotNull(paymentIntent);
        final Uri redirectUrl = paymentIntent.getRedirectUrl();
        assertNotNull(redirectUrl);
        assertEquals("https://example.com/redirect", redirectUrl.toString());
    }

    @Test
    public void getRedirectUrl_withAuthorizeWithUrlPopulated_returnsRedirectUrl() {
        final PaymentIntent paymentIntent = PaymentIntent
                .fromString(PARTIAL_PAYMENT_INTENT_WITH_AUTHORIZE_WITH_URL_JSON);
        assertNotNull(paymentIntent);
        final Uri redirectUrl = paymentIntent.getRedirectUrl();
        assertNotNull(redirectUrl);
        assertEquals("https://example.com/redirect", redirectUrl.toString());
    }

    @Test
    public void parseIdFromClientSecret_parsesCorrectly() {
        final String clientSecret = "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA";
        final String id = PaymentIntent.parseIdFromClientSecret(clientSecret);
        assertEquals("pi_1CkiBMLENEVhOs7YMtUehLau", id);
    }

    @Test
    public void parsePaymentIntentWithPaymentMethods() {
        final PaymentIntent paymentIntent =
                PaymentIntent.fromString(PAYMENT_INTENT_WITH_PAYMENT_METHODS_JSON);
        assertNotNull(paymentIntent);
        assertEquals("card", paymentIntent.getPaymentMethodTypes().get(0));
    }
}
