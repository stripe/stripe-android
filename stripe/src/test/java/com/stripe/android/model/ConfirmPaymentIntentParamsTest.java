package com.stripe.android.model;


import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.view.CardInputTestActivity.VALID_VISA_NO_SPACES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfirmPaymentIntentParamsTest {
    private static final Card FULL_FIELDS_VISA_CARD =
            new Card.Builder(VALID_VISA_NO_SPACES, 12, 2050, "123")
                    .name("Captain Cardholder")
                    .addressLine1("1 ABC Street")
                    .addressLine2("Apt. 123")
                    .addressCity("San Francisco")
                    .addressState("CA")
                    .addressZip("94107")
                    .addressState("US")
                    .currency("usd")
                    .build();

    private static final String CLIENT_SECRET =
            "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA";

    private static final String RETURN_URL = "stripe://return_url";
    private static final String SOURCE_ID = "src_123testsourceid";
    private static final String PM_ID = "pm_123456789";

    @Test
    public void createConfirmPaymentIntentWithSourceDataParams_withAllFields_hasExpectedFields() {
        final SourceParams sourceParams = SourceParams.createCardParams(FULL_FIELDS_VISA_CARD);
        final ConfirmPaymentIntentParams params = ConfirmPaymentIntentParams
                .createWithSourceParams(
                        sourceParams, CLIENT_SECRET, RETURN_URL);

        assertEquals(CLIENT_SECRET, params.getClientSecret());
        assertEquals(RETURN_URL, params.getReturnUrl());
        assertEquals(sourceParams, params.getSourceParams());
        assertFalse(params.shouldSavePaymentMethod());
    }

    @Test
    public void createConfirmPaymentIntentWithSourceIdParams_withAllFields_hasExpectedFields() {
        final ConfirmPaymentIntentParams params = ConfirmPaymentIntentParams
                .createWithSourceId(
                        SOURCE_ID, CLIENT_SECRET, RETURN_URL);

        assertEquals(CLIENT_SECRET, params.getClientSecret());
        assertEquals(RETURN_URL, params.getReturnUrl());
        assertEquals(SOURCE_ID, params.getSourceId());
        assertFalse(params.shouldSavePaymentMethod());
    }

    @Test
    public void createConfirmPaymentIntentWithSourceIdParams_withSavePaymentMethod_hasExpectedFields() {
        final ConfirmPaymentIntentParams params = ConfirmPaymentIntentParams
                .createWithSourceId(
                        SOURCE_ID, CLIENT_SECRET, RETURN_URL, true);

        assertEquals(CLIENT_SECRET, params.getClientSecret());
        assertEquals(RETURN_URL, params.getReturnUrl());
        assertEquals(SOURCE_ID, params.getSourceId());
        assertTrue(params.shouldSavePaymentMethod());

        assertEquals(Boolean.TRUE,
                params.toParamMap().get(ConfirmPaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }

    @Test
    public void createWithPaymentMethodCreateParams_hasExpectedFields() {
        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParamsFixtures.DEFAULT;
        final ConfirmPaymentIntentParams params = ConfirmPaymentIntentParams
                .createWithPaymentMethodCreateParams(paymentMethodCreateParams,
                        CLIENT_SECRET, RETURN_URL);

        assertEquals(CLIENT_SECRET, params.getClientSecret());
        assertEquals(RETURN_URL, params.getReturnUrl());
        assertEquals(paymentMethodCreateParams, params.getPaymentMethodCreateParams());
        assertFalse(params.shouldSavePaymentMethod());
    }

    @Test
    public void createConfirmPaymentIntentWithPaymentMethodId_hasExpectedFields() {
        final ConfirmPaymentIntentParams params = ConfirmPaymentIntentParams
                .createWithPaymentMethodId(
                        PM_ID, CLIENT_SECRET, RETURN_URL);

        assertEquals(CLIENT_SECRET, params.getClientSecret());
        assertEquals(RETURN_URL, params.getReturnUrl());
        assertEquals(PM_ID, params.getPaymentMethodId());
        assertFalse(params.shouldSavePaymentMethod());
    }

    @Test
    public void createConfirmPaymentIntentWithPaymentMethodCreateParams_withSavePaymentMethod_hasExpectedFields() {
        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParamsFixtures.DEFAULT;
        final ConfirmPaymentIntentParams params = ConfirmPaymentIntentParams
                .createWithPaymentMethodCreateParams(paymentMethodCreateParams,
                        CLIENT_SECRET, RETURN_URL, true);

        assertEquals(CLIENT_SECRET, params.getClientSecret());
        assertEquals(RETURN_URL, params.getReturnUrl());
        assertEquals(paymentMethodCreateParams, params.getPaymentMethodCreateParams());
        assertTrue(params.shouldSavePaymentMethod());
    }

    @Test
    public void createConfirmPaymentIntentWithPaymentMethodId_withSavePaymentMethod_hasExpectedFields() {
        final ConfirmPaymentIntentParams params = ConfirmPaymentIntentParams
                .createWithPaymentMethodId(
                        PM_ID, CLIENT_SECRET, RETURN_URL, true);

        assertEquals(CLIENT_SECRET, params.getClientSecret());
        assertEquals(RETURN_URL, params.getReturnUrl());
        assertEquals(PM_ID, params.getPaymentMethodId());
        assertTrue(params.shouldSavePaymentMethod());

        assertEquals(Boolean.TRUE,
                params.toParamMap().get(ConfirmPaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }

    @Test
    public void createWithSourceId_toParamMap_createsExpectedMap() {
        final ConfirmPaymentIntentParams confirmPaymentIntentParams = ConfirmPaymentIntentParams
                .createWithSourceId(
                        SOURCE_ID, CLIENT_SECRET, RETURN_URL);

        final Map<String, Object> paramMap = confirmPaymentIntentParams.toParamMap();

        assertEquals(paramMap.get(ConfirmPaymentIntentParams.API_PARAM_SOURCE_ID), SOURCE_ID);
        assertEquals(
                paramMap.get(ConfirmPaymentIntentParams.API_PARAM_CLIENT_SECRET), CLIENT_SECRET);
        assertEquals(
                paramMap.get(ConfirmPaymentIntentParams.API_PARAM_RETURN_URL), RETURN_URL);
        assertFalse(paramMap.containsKey(ConfirmPaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }

    @Test
    public void createWithPaymentMethodId_withoutReturnUrl_toParamMap_createsExpectedMap() {
        final ConfirmPaymentIntentParams confirmPaymentIntentParams = ConfirmPaymentIntentParams
                .createWithPaymentMethodId(PM_ID, CLIENT_SECRET);

        final Map<String, Object> paramMap = confirmPaymentIntentParams.toParamMap();

        assertEquals(paramMap.get(ConfirmPaymentIntentParams.API_PARAM_PAYMENT_METHOD_ID),
                PM_ID);
        assertEquals(
                paramMap.get(ConfirmPaymentIntentParams.API_PARAM_CLIENT_SECRET), CLIENT_SECRET);
        assertFalse(paramMap.containsKey(ConfirmPaymentIntentParams.API_PARAM_RETURN_URL));
        assertFalse(paramMap.containsKey(ConfirmPaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }

    @Test
    public void createWithPaymentMethodId_withReturnUrl_toParamMap_createsExpectedMap() {
        final ConfirmPaymentIntentParams confirmPaymentIntentParams = ConfirmPaymentIntentParams
                .createWithPaymentMethodId(PM_ID, CLIENT_SECRET, RETURN_URL);

        final Map<String, Object> paramMap = confirmPaymentIntentParams.toParamMap();

        assertEquals(paramMap.get(ConfirmPaymentIntentParams.API_PARAM_PAYMENT_METHOD_ID),
                PM_ID);
        assertEquals(
                paramMap.get(ConfirmPaymentIntentParams.API_PARAM_CLIENT_SECRET), CLIENT_SECRET);
        assertEquals(
                paramMap.get(ConfirmPaymentIntentParams.API_PARAM_RETURN_URL), RETURN_URL);
        assertFalse(paramMap.containsKey(ConfirmPaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }

    @Test
    public void toParamMap_whenExtraParamsProvided_createsExpectedMap() {
        final Map<String, Object> extraParams = new HashMap<>();
        String extraParamKey1 = "extra_param_key_1";
        String extraParamKey2 = "extra_param_key_2";
        String extraParamValue1 = "extra_param_value_1";
        String extraParamValue2 = "extra_param_value_2";
        extraParams.put(extraParamKey1, extraParamValue1);
        extraParams.put(extraParamKey2, extraParamValue2);

        final ConfirmPaymentIntentParams confirmPaymentIntentParams = ConfirmPaymentIntentParams
                .createWithPaymentMethodId("pm_123", CLIENT_SECRET,
                        RETURN_URL, false, extraParams);

        final Map<String, Object> paramMap = confirmPaymentIntentParams.toParamMap();

        assertEquals(
                paramMap.get(ConfirmPaymentIntentParams.API_PARAM_CLIENT_SECRET), CLIENT_SECRET);
        assertEquals(
                paramMap.get(extraParamKey1), extraParamValue1);
        assertEquals(
                paramMap.get(extraParamKey2), extraParamValue2);
        assertFalse(paramMap.containsKey(ConfirmPaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }

    @Test
    public void create_withClientSecret() {
        assertEquals("client_secret",
                ConfirmPaymentIntentParams.create("client_secret", "")
                        .getClientSecret());
    }

    @Test
    public void shouldUseStripeSdk() {
        final ConfirmPaymentIntentParams confirmPaymentIntentParams =
                ConfirmPaymentIntentParams.create("client_secret", "return_url");
        assertFalse(confirmPaymentIntentParams.shouldUseStripeSdk());

        assertTrue(confirmPaymentIntentParams
                .withShouldUseStripeSdk(true)
                .shouldUseStripeSdk());
    }

    @Test
    public void toBuilder_withPaymentMethodCreateParams_shouldCreateEqualObject() {
        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("key", "value");
        final ConfirmPaymentIntentParams params =
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                        PaymentMethodCreateParamsFixtures.DEFAULT, CLIENT_SECRET, RETURN_URL, true, extraParams
                );

        assertEquals(params, params.toBuilder().build());
    }
}
