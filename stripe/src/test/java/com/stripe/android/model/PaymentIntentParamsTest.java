package com.stripe.android.model;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.view.CardInputTestActivity.VALID_VISA_NO_SPACES;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class PaymentIntentParamsTest {
    private static Card FULL_FIELDS_VISA_CARD =
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
                    "usd");

    private static String TEST_CLIENT_SECRET =
            "pi_1CkiBMLENEVhOs7YMtUehLau_secret_s4O8SDh7s6spSmHDw1VaYPGZA";

    private static String TEST_RETURN_URL = "stripe://return_url";
    private static String TEST_SOURCE_ID = "src_123testsourceid";

    @Test
    public void createConfirmPaymentIntentWithSourceDataParams_withAllFields_hasExpectedFields() {
        SourceParams sourceParams = SourceParams.createCardParams(FULL_FIELDS_VISA_CARD);
        PaymentIntentParams params = PaymentIntentParams.createConfirmPaymentIntentWithSourceDataParams(
                sourceParams,
                TEST_CLIENT_SECRET,
                TEST_RETURN_URL);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertEquals(TEST_RETURN_URL, params.getReturnUrl());
        Assert.assertEquals(sourceParams, params.getSourceParams());
    }

    @Test
    public void createConfirmPaymentIntentWithSourceIdParams_withAllFields_hasExpectedFields() {
        PaymentIntentParams params = PaymentIntentParams.createConfirmPaymentIntentWithSourceIdParams(
                TEST_SOURCE_ID,
                TEST_CLIENT_SECRET,
                TEST_RETURN_URL);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertEquals(TEST_RETURN_URL, params.getReturnUrl());
        Assert.assertEquals(TEST_SOURCE_ID, params.getSourceId());
    }

    @Test
    public void createRetrievePaymentIntentWithSourceIdParams_hasExpectedFields() {
        PaymentIntentParams params = PaymentIntentParams.createRetrievePaymentIntentParams(
                TEST_CLIENT_SECRET);

        Assert.assertEquals(TEST_CLIENT_SECRET, params.getClientSecret());
        Assert.assertNull(params.getReturnUrl());
        Assert.assertNull(params.getExtraParams());
        Assert.assertNull(params.getSourceId());
        Assert.assertNull(params.getSourceParams());
    }

    @Test
    public void createCustomParams_toParamMap_createsExpectedMap() {
        PaymentIntentParams paymentIntentParams = PaymentIntentParams.createCustomParams();
        paymentIntentParams
                .setReturnUrl(TEST_RETURN_URL)
                .setClientSecret(TEST_CLIENT_SECRET)
                .setSourceId(TEST_SOURCE_ID);

        Map<String, Object> paramMap = paymentIntentParams.toParamMap();

        Assert.assertEquals(paramMap.get(PaymentIntentParams.API_PARAM_SOURCE_ID), TEST_SOURCE_ID);
        Assert.assertEquals(
                paramMap.get(PaymentIntentParams.API_PARAM_CLIENT_SECRET), TEST_CLIENT_SECRET);
        Assert.assertEquals(
                paramMap.get(PaymentIntentParams.API_PARAM_RETURN_URL), TEST_RETURN_URL);
    }

    @Test
    public void toParamMap_whenExtraParamsProvided_createsExpectedMap() {
        PaymentIntentParams paymentIntentParams = PaymentIntentParams.createRetrievePaymentIntentParams(
                TEST_CLIENT_SECRET);
        Map<String, Object> extraParams = new HashMap<>();
        String extraParamKey1 = "extra_param_key_1";
        String extraParamKey2 = "extra_param_key_2";
        String extraParamValue1 = "extra_param_value_1";
        String extraParamValue2 = "extra_param_value_2";
        extraParams.put(extraParamKey1, extraParamValue1);
        extraParams.put(extraParamKey2, extraParamValue2);
        paymentIntentParams.setExtraParams(extraParams);

        Map<String, Object> paramMap = paymentIntentParams.toParamMap();

        Assert.assertEquals(
                paramMap.get(PaymentIntentParams.API_PARAM_CLIENT_SECRET), TEST_CLIENT_SECRET);
        Assert.assertEquals(
                paramMap.get(extraParamKey1), extraParamValue1);
        Assert.assertEquals(
                paramMap.get(extraParamKey2), extraParamValue2);
    }

}
