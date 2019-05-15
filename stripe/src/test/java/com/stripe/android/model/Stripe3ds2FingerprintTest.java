package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class Stripe3ds2FingerprintTest {

    private static final String VISA_DS_JSON = "{\n" +
            "\t\"type\": \"stripe_3ds2_fingerprint\",\n" +
            "\t\"three_d_secure_2_source\": \"src_1EZl3YCRMbs6FrXfYgJXF46w\",\n" +
            "\t\"directory_server_name\": \"visa\",\n" +
            "\t\"server_transaction_id\": \"b31807ca-e7d6-4685-a7e3-53fbd2962135\",\n" +
            "\t\"three_ds_method_url\": \"\"\n" +
            "}";

    private static final String UNKNOWN_DS_JSON = "{\n" +
            "\t\"type\": \"stripe_3ds2_fingerprint\",\n" +
            "\t\"three_d_secure_2_source\": \"src_1EZl3YCRMbs6FrXfYgJXF46w\",\n" +
            "\t\"directory_server_name\": \"unknown\",\n" +
            "\t\"server_transaction_id\": \"b31807ca-e7d6-4685-a7e3-53fbd2962135\",\n" +
            "\t\"three_ds_method_url\": \"\"\n" +
            "}";

    @Test
    public void create_withVisaDsJson_shouldCreateObject() throws JSONException {
        final Stripe3ds2Fingerprint stripe3ds2Fingerprint =
                Stripe3ds2Fingerprint.create(new JSONObject(VISA_DS_JSON));
        assertEquals("src_1EZl3YCRMbs6FrXfYgJXF46w",
                stripe3ds2Fingerprint.source);
        assertEquals(Stripe3ds2Fingerprint.DirectoryServerName.VISA,
                stripe3ds2Fingerprint.directoryServerName);
        assertEquals("b31807ca-e7d6-4685-a7e3-53fbd2962135",
                stripe3ds2Fingerprint.serverTransactionId);
    }

    @Test
    public void create_withUnknownDsJson_shouldCreateObject() throws JSONException {
        final Stripe3ds2Fingerprint stripe3ds2Fingerprint =
                Stripe3ds2Fingerprint.create(new JSONObject(UNKNOWN_DS_JSON));
        assertEquals("src_1EZl3YCRMbs6FrXfYgJXF46w",
                stripe3ds2Fingerprint.source);
        assertNull(stripe3ds2Fingerprint.directoryServerName);
        assertEquals("b31807ca-e7d6-4685-a7e3-53fbd2962135",
                stripe3ds2Fingerprint.serverTransactionId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_withEmptyJson_shouldThrowException() throws JSONException {
        Stripe3ds2Fingerprint.create(new JSONObject());
    }
}