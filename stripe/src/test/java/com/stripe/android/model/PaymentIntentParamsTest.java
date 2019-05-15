package com.stripe.android.model;


import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.view.CardInputTestActivity.VALID_VISA_NO_SPACES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PaymentIntentParamsTest {
    private static final Card FULL_FIELDS_VISA_CARD =
            new Card(VALID_VISA_NO_SPACES,
                    12,
                    2050,
                    "123",
                    "Captain Cardholder",
                    "1 ABC Street",
                    "Apt. 123",
                    "San Francisco",
                    "CA",
                    "94107",
                    "US",
                    "usd",
                    null);

    private static final String TEST_CLIENT_SECRET =
            "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA";

    private static final String TEST_RETURN_URL = "stripe://return_url";
    private static final String TEST_SOURCE_ID = "src_123testsourceid";
    private static final String TEST_PAYMENT_METHOD_ID = "pm_123456789";

    @Test
    public void createConfirmPaymentIntentWithSourceDataParams_withAllFields_hasExpectedFields() {
        final SourceParams sourceParams = SourceParams.createCardParams(FULL_FIELDS_VISA_CARD);
        final PaymentIntentParams params = PaymentIntentParams
                .createConfirmPaymentIntentWithSourceDataParams(
                        sourceParams, TEST_CLIENT_SECRET, TEST_RETURN_URL);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertEquals(TEST_RETURN_URL, params.getReturnUrl());
        Assert.assertEquals(sourceParams, params.getSourceParams());
        assertFalse(params.shouldSavePaymentMethod());
    }

    @Test
    public void createConfirmPaymentIntentWithSourceIdParams_withAllFields_hasExpectedFields() {
        final PaymentIntentParams params = PaymentIntentParams
                .createConfirmPaymentIntentWithSourceIdParams(
                        TEST_SOURCE_ID, TEST_CLIENT_SECRET, TEST_RETURN_URL);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertEquals(TEST_RETURN_URL, params.getReturnUrl());
        Assert.assertEquals(TEST_SOURCE_ID, params.getSourceId());
        assertFalse(params.shouldSavePaymentMethod());
    }

    @Test
    public void createConfirmPaymentIntentWithSourceIdParams_withSavePaymentMethod_hasExpectedFields() {
        final PaymentIntentParams params = PaymentIntentParams
                .createConfirmPaymentIntentWithSourceIdParams(
                        TEST_SOURCE_ID, TEST_CLIENT_SECRET, TEST_RETURN_URL, true);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertEquals(TEST_RETURN_URL, params.getReturnUrl());
        Assert.assertEquals(TEST_SOURCE_ID, params.getSourceId());
        assertTrue(params.shouldSavePaymentMethod());

        assertEquals(Boolean.TRUE,
                params.toParamMap().get(PaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }

    @Test
    public void createRetrievePaymentIntentWithSourceIdParams_hasExpectedFields() {
        final PaymentIntentParams params = PaymentIntentParams
                .createRetrievePaymentIntentParams(TEST_CLIENT_SECRET);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertNull(params.getReturnUrl());
        Assert.assertNull(params.getExtraParams());
        Assert.assertNull(params.getSourceId());
        Assert.assertNull(params.getSourceParams());
        assertFalse(params.shouldSavePaymentMethod());
    }

    @Test
    public void createRetrievePaymentIntentwithPaymentMethodCreateParams_hasExpectedFields() {
        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(new PaymentMethodCreateParams.Card.Builder()
                        .build(), null);
        final PaymentIntentParams params = PaymentIntentParams
                .createConfirmPaymentIntentWithPaymentMethodCreateParams(paymentMethodCreateParams,
                        TEST_CLIENT_SECRET, TEST_RETURN_URL);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertEquals(TEST_RETURN_URL, params.getReturnUrl());
        Assert.assertEquals(paymentMethodCreateParams, params.getPaymentMethodCreateParams());
        assertFalse(params.shouldSavePaymentMethod());
    }

    @Test
    public void createConfirmPaymentIntentWithPaymentMethodId_hasExpectedFields() {
        final PaymentIntentParams params = PaymentIntentParams
                .createConfirmPaymentIntentWithPaymentMethodId(
                        TEST_PAYMENT_METHOD_ID, TEST_CLIENT_SECRET, TEST_RETURN_URL);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertEquals(TEST_RETURN_URL, params.getReturnUrl());
        Assert.assertEquals(TEST_PAYMENT_METHOD_ID, params.getPaymentMethodId());
        assertFalse(params.shouldSavePaymentMethod());
    }

    @Test
    public void createConfirmPaymentIntentWithPaymentMethodCreateParams_withSavePaymentMethod_hasExpectedFields() {
        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(new PaymentMethodCreateParams.Card.Builder()
                        .build(), null);
        final PaymentIntentParams params = PaymentIntentParams
                .createConfirmPaymentIntentWithPaymentMethodCreateParams(paymentMethodCreateParams,
                        TEST_CLIENT_SECRET, TEST_RETURN_URL, true);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertEquals(TEST_RETURN_URL, params.getReturnUrl());
        Assert.assertEquals(paymentMethodCreateParams, params.getPaymentMethodCreateParams());
        assertTrue(params.shouldSavePaymentMethod());
    }

    @Test
    public void createConfirmPaymentIntentWithPaymentMethodId_withSavePaymentMethod_hasExpectedFields() {
        final PaymentIntentParams params = PaymentIntentParams
                .createConfirmPaymentIntentWithPaymentMethodId(
                        TEST_PAYMENT_METHOD_ID, TEST_CLIENT_SECRET, TEST_RETURN_URL, true);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertEquals(TEST_RETURN_URL, params.getReturnUrl());
        Assert.assertEquals(TEST_PAYMENT_METHOD_ID, params.getPaymentMethodId());
        assertTrue(params.shouldSavePaymentMethod());

        assertEquals(Boolean.TRUE,
                params.toParamMap().get(PaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }

    @Test
    public void createCustomParams_withSourceId_toParamMap_createsExpectedMap() {
        final PaymentIntentParams paymentIntentParams = PaymentIntentParams.createCustomParams()
                .setReturnUrl(TEST_RETURN_URL)
                .setClientSecret(TEST_CLIENT_SECRET)
                .setSourceId(TEST_SOURCE_ID);

        final Map<String, Object> paramMap = paymentIntentParams.toParamMap();

        Assert.assertEquals(paramMap.get(PaymentIntentParams.API_PARAM_SOURCE_ID), TEST_SOURCE_ID);
        Assert.assertEquals(
                paramMap.get(PaymentIntentParams.API_PARAM_CLIENT_SECRET), TEST_CLIENT_SECRET);
        Assert.assertEquals(
                paramMap.get(PaymentIntentParams.API_PARAM_RETURN_URL), TEST_RETURN_URL);
        assertFalse(paramMap.containsKey(PaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }

    @Test
    public void createCustomParams_withPaymentMethodId_toParamMap_createsExpectedMap() {
        final PaymentIntentParams paymentIntentParams = PaymentIntentParams.createCustomParams()
                .setReturnUrl(TEST_RETURN_URL)
                .setClientSecret(TEST_CLIENT_SECRET)
                .setPaymentMethodId(TEST_PAYMENT_METHOD_ID);

        final Map<String, Object> paramMap = paymentIntentParams.toParamMap();

        Assert.assertEquals(paramMap.get(PaymentIntentParams.API_PARAM_PAYMENT_METHOD_ID),
                TEST_PAYMENT_METHOD_ID);
        Assert.assertEquals(
                paramMap.get(PaymentIntentParams.API_PARAM_CLIENT_SECRET), TEST_CLIENT_SECRET);
        Assert.assertEquals(
                paramMap.get(PaymentIntentParams.API_PARAM_RETURN_URL), TEST_RETURN_URL);
        assertFalse(paramMap.containsKey(PaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }

    @Test
    public void toParamMap_whenExtraParamsProvided_createsExpectedMap() {
        final PaymentIntentParams paymentIntentParams = PaymentIntentParams
                .createRetrievePaymentIntentParams(TEST_CLIENT_SECRET);
        final Map<String, Object> extraParams = new HashMap<>();
        String extraParamKey1 = "extra_param_key_1";
        String extraParamKey2 = "extra_param_key_2";
        String extraParamValue1 = "extra_param_value_1";
        String extraParamValue2 = "extra_param_value_2";
        extraParams.put(extraParamKey1, extraParamValue1);
        extraParams.put(extraParamKey2, extraParamValue2);
        paymentIntentParams.setExtraParams(extraParams);

        final Map<String, Object> paramMap = paymentIntentParams.toParamMap();

        Assert.assertEquals(
                paramMap.get(PaymentIntentParams.API_PARAM_CLIENT_SECRET), TEST_CLIENT_SECRET);
        Assert.assertEquals(
                paramMap.get(extraParamKey1), extraParamValue1);
        Assert.assertEquals(
                paramMap.get(extraParamKey2), extraParamValue2);
        assertFalse(paramMap.containsKey(PaymentIntentParams.API_PARAM_SAVE_PAYMENT_METHOD));
    }
}
