package com.stripe.android.model;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertJsonEqualsExcludingNulls;
import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class PaymentIntentTest {

    static final String EXAMPLE_PAYMENT_INTENT_SOURCE= "{\n" +
            "  \"id\": \"pi_1CkiBMLENEVhOs7YMtUehLau\",\n" +
            "  \"object\": \"payment_intent\",\n" +
            "  \"allowed_source_types\": [\n" +
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

    static final String BAD_URL = "nonsense-blahblah";

    static final String EXAMPLE_PAYMENT_INTENT_SOURCE_WITH_BAD_AUTH_URL = "{\n" +
            "  \"id\": \"pi_1CkiBMLENEVhOs7YMtUehLau\",\n" +
            "  \"object\": \"payment_intent\",\n" +
            "  \"allowed_source_types\": [\n" +
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
            "  \"next_source_action\": {" +
            "       authorize_with_url: {" +
        "           url: \""+ BAD_URL +"\" } " +
            "       },\n" +
            "  \"receipt_email\": null,\n" +
            "  \"shipping\": null,\n" +
            "  \"source\": \"src_1CkiC3LENEVhOs7YMSa4yx4G\",\n" +
            "  \"status\": \"requires_source_action\"\n" +
            "}\n";

    private static List<String> ALLOWED_SOURCE_TYPES = new ArrayList<String>() {{
        add("card");
    }};

    static final Map<String, Object> EXAMPLE_PAYMENT_INTENT_MAP = new HashMap<String, Object>() {{
        put("id", "pi_1CkiBMLENEVhOs7YMtUehLau");
        put("object", "payment_intent");
        put("allowed_source_types", ALLOWED_SOURCE_TYPES);
        put("amount", 1000L);
        put("canceled_at", 1530839340L);
        put("client_secret", "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA");
        put("confirmation_method", "publishable");
        put("created", 1530838340L);
        put("currency", "usd");
        put("description", "Example PaymentIntent charge");
        put("livemode", false);
        put("source", "src_1CkiC3LENEVhOs7YMSa4yx4G");
        put("capture_method", "automatic");
        put("status", "succeeded");
    }};

    private PaymentIntent mPaymentIntent;

    @Before
    public void setup() {
        mPaymentIntent = PaymentIntent.fromString(EXAMPLE_PAYMENT_INTENT_SOURCE);
        assertNotNull(mPaymentIntent);
    }

    @Test
    public void fromJsonString_backToJson_createsIdenticalElement() {
        try {
            JSONObject rawConversion = new JSONObject(EXAMPLE_PAYMENT_INTENT_SOURCE);
            JSONObject actualObject = mPaymentIntent.toJson();
            assertJsonEqualsExcludingNulls(rawConversion, actualObject);
        } catch (JSONException jsonException) {
            fail("Test Data failure: " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void fromJsonString_toMap_createsExpectedMap() {
        Map<String, Object> paymentIntentMap = mPaymentIntent.toMap();
        assertMapEquals(paymentIntentMap, EXAMPLE_PAYMENT_INTENT_MAP);
    }

    @Test
    public void getAuthorizationUrl_whenProvidedBadUrl_doesNotCrash() {
        PaymentIntent paymentIntent = PaymentIntent.fromString(
                EXAMPLE_PAYMENT_INTENT_SOURCE_WITH_BAD_AUTH_URL);

        Uri authUrl = paymentIntent.getAuthorizationUrl();

        assertEquals(BAD_URL, authUrl.getEncodedPath());
    }

    @Test
    public void parseIdFromClientSecret_parsesCorrectly() {
        String clientSecret = "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA";

        String id = PaymentIntent.parseIdFromClientSecret(clientSecret);

        assertEquals("pi_1CkiBMLENEVhOs7YMtUehLau", id);
    }

}
