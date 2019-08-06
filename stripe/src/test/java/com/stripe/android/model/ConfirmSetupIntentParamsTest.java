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

    private static final PaymentMethodCreateParams PM_CREATE_PARAMS =
            PaymentMethodCreateParams.create(
                    new PaymentMethodCreateParams.Card.Builder()
                            .setNumber("4242424242424242")
                            .setExpiryMonth(1)
                            .setExpiryYear(2024)
                            .setCvc("111")
                            .build(),
                    null
            );

    @Test
    public void shouldUseStripeSdk_withPaymentMethodId() {
        final ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        "pm_123", "client_secret", "return_url");
        assertFalse(confirmSetupIntentParams.shouldUseStripeSdk());

        assertTrue(confirmSetupIntentParams
                .withShouldUseStripeSdk(true)
                .shouldUseStripeSdk());
    }

    @Test
    public void shouldUseStripeSdk_withPaymentMethodCreateParams() {
        final ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        PM_CREATE_PARAMS,
                        "client_secret",
                        "return_url"
                );
        assertFalse(confirmSetupIntentParams.shouldUseStripeSdk());

        assertTrue(confirmSetupIntentParams
                .withShouldUseStripeSdk(true)
                .shouldUseStripeSdk());
    }

    @Test
    public void toBuilder_withPaymentMethodId_shouldCreateEqualObject() {
        final ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        "pm_123", "client_secret", "return_url");
        assertEquals(confirmSetupIntentParams,
                confirmSetupIntentParams.toBuilder().build());
    }

    @Test
    public void toBuilder_withPaymentMethodCreateParams_shouldCreateEqualObject() {
        final ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        PM_CREATE_PARAMS, "client_secret", "return_url");
        assertEquals(confirmSetupIntentParams,
                confirmSetupIntentParams.toBuilder().build());
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
        final ConfirmSetupIntentParams confirmSetupIntentParams =
                ConfirmSetupIntentParams.create(
                        PM_CREATE_PARAMS,
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
}
