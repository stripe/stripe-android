package com.stripe.android.model;

import org.junit.Test;

import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfirmSetupIntentParamsTest {

    @Test
    public void shouldUseStripeSdk() {
        final ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        "pm_123", "client_secret", "return_url");
        assertFalse(confirmSetupIntentParams.shouldUseStripeSdk());

        assertTrue(confirmSetupIntentParams
                .withShouldUseStripeSdk(true)
                .shouldUseStripeSdk());
    }

    @Test
    public void create_withPaymentMethodId_shouldPopulateParamMapCorrectly() {
        final ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        "pm_12345",
                        "client_secret",
                        null
                );
        final Map<String, Object> params = confirmSetupIntentParams.toParamMap();
        assertNull(params.get(ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_DATA));
        assertEquals("pm_12345",
                params.get(ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_ID));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void create_withPaymentMethodCreateParams_shouldPopulateParamMapCorrectly() {
        final PaymentMethodCreateParams.Card expectedCard =
                new PaymentMethodCreateParams.Card.Builder()
                        .setNumber("4242424242424242")
                        .setCvc("123")
                        .setExpiryMonth(8)
                        .setExpiryYear(2019)
                        .build();

        final ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        PaymentMethodCreateParams.create(expectedCard, null),
                        "client_secret",
                        null
                );
        final Map<String, Object> params = confirmSetupIntentParams.toParamMap();
        assertNull(params.get(ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_ID));
        final Map<String, Object> paymentMethodData = (Map<String, Object>)
                Objects.requireNonNull(
                        params.get(ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_DATA));
        assertEquals("card", paymentMethodData.get("type"));
        assertNotNull(paymentMethodData.get("card"));
    }

    @Test
    public void create_withShouldUseStripeSdk_shouldKeepPaymentMethodData() {
        final PaymentMethodCreateParams.Card expectedCard =
                new PaymentMethodCreateParams.Card.Builder()
                        .setNumber("4242424242424242")
                        .setCvc("123")
                        .setExpiryMonth(8)
                        .setExpiryYear(2019)
                        .build();

        ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        PaymentMethodCreateParams.create(expectedCard, null),
                        "client_secret",
                        null
                );

        assertEquals(false, confirmSetupIntentParams.shouldUseStripeSdk());
        confirmSetupIntentParams = confirmSetupIntentParams.withShouldUseStripeSdk(true);
        assertEquals(true, confirmSetupIntentParams.shouldUseStripeSdk());


        // Ensures that payment method data is still present after the withShouldUseStripeSdk call
        final Map<String, Object> params = confirmSetupIntentParams.toParamMap();
        assertNull(params.get(ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_ID));
        final Map<String, Object> paymentMethodData = (Map<String, Object>)
                Objects.requireNonNull(
                        params.get(ConfirmStripeIntentParams.API_PARAM_PAYMENT_METHOD_DATA));
        assertEquals("card", paymentMethodData.get("type"));
        assertNotNull(paymentMethodData.get("card"));
    }
}
