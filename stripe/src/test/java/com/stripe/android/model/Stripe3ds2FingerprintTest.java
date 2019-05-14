package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Stripe3ds2FingerprintTest {

    private static final String JSON = "{\n" +
            "\t\"type\": \"stripe_3ds2_fingerprint\",\n" +
            "\t\"three_d_secure_2_source\": \"src_1EZl3YCRMbs6FrXfYgJXF46w\",\n" +
            "\t\"directory_server_name\": \"visa\",\n" +
            "\t\"server_transaction_id\": \"b31807ca-e7d6-4685-a7e3-53fbd2962135\",\n" +
            "\t\"three_ds_method_url\": \"\"\n" +
            "}";

    @Test
    public void create_withValidJson_shouldCreateObject() throws JSONException {
        final Stripe3ds2Fingerprint stripe3ds2Fingerprint =
                Stripe3ds2Fingerprint.create(new JSONObject(JSON));
        assertEquals("src_1EZl3YCRMbs6FrXfYgJXF46w",
                stripe3ds2Fingerprint.source);
        assertEquals("visa",
                stripe3ds2Fingerprint.directoryServerName);
        assertEquals("b31807ca-e7d6-4685-a7e3-53fbd2962135",
                stripe3ds2Fingerprint.serverTransactionId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_withEmptyJson_shouldThrowException() {
        Stripe3ds2Fingerprint.create(new JSONObject());
    }
}