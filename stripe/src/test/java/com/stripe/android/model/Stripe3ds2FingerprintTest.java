package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
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
    public void create_with3ds2SdkData_shouldCreateObject() {
        final PaymentIntent.SdkData sdkData = PaymentIntentFixtures.PI_REQUIRES_3DS2
                .getStripeSdkData();
        assertNotNull(sdkData);
        final Stripe3ds2Fingerprint stripe3ds2Fingerprint = Stripe3ds2Fingerprint.create(sdkData);
        assertEquals("src_1EceOlCRMbs6FrXf2hqrI1g5",
                stripe3ds2Fingerprint.source);
        assertEquals(Stripe3ds2Fingerprint.DirectoryServer.Visa,
                stripe3ds2Fingerprint.directoryServer);
        assertEquals("e64bb72f-60ac-4845-b8b6-47cfdb0f73aa",
                stripe3ds2Fingerprint.serverTransactionId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_with3ds1SdkData_shouldThrowException() {
        final PaymentIntent.SdkData sdkData = PaymentIntentFixtures.PI_REQUIRES_3DS1
                .getStripeSdkData();
        assertNotNull(sdkData);
        Stripe3ds2Fingerprint.create(sdkData);
    }

    @Test
    public void create_withVisaDsJson_shouldCreateObject() throws JSONException {
        final Stripe3ds2Fingerprint stripe3ds2Fingerprint =
                Stripe3ds2Fingerprint.create(new JSONObject(VISA_DS_JSON));
        assertEquals("src_1EZl3YCRMbs6FrXfYgJXF46w",
                stripe3ds2Fingerprint.source);
        assertEquals(Stripe3ds2Fingerprint.DirectoryServer.Visa,
                stripe3ds2Fingerprint.directoryServer);
        assertEquals("b31807ca-e7d6-4685-a7e3-53fbd2962135",
                stripe3ds2Fingerprint.serverTransactionId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_withUnknownDsJson_shouldThrowException() throws JSONException {
        Stripe3ds2Fingerprint.create(new JSONObject(UNKNOWN_DS_JSON));
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_withEmptyJson_shouldThrowException() throws JSONException {
        Stripe3ds2Fingerprint.create(new JSONObject());
    }
}