package com.stripe.android;

import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link StripeApiHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class StripeApiHandlerTest {

    private static final String STRIPE_ACCOUNT_RESPONSE_HEADER = "Stripe-Account";

    private static final Card CARD = Card.create("4242424242424242", 1, 2050, "123");

    @NonNull private final StripeApiHandler mApiHandler =
            new StripeApiHandler(ApplicationProvider.getApplicationContext(), null);

    @Mock private RequestExecutor mRequestExecutor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetApiUrl() {
        String tokensApi = StripeApiHandler.getTokensUrl();
        assertEquals("https://api.stripe.com/v1/tokens", tokensApi);
    }

    @Test
    public void testGetSourcesUrl() {
        String sourcesUrl = StripeApiHandler.getSourcesUrl();
        assertEquals("https://api.stripe.com/v1/sources", sourcesUrl);
    }

    @Test
    public void testGetRetrieveSourceUrl() {
        String sourceUrlWithId = StripeApiHandler.getRetrieveSourceApiUrl("abc123");
        assertEquals("https://api.stripe.com/v1/sources/abc123", sourceUrlWithId);
    }

    @Test
    public void testGetRequestTokenApiUrl() {
        String tokenId = "tok_sample";
        String requestApi = StripeApiHandler.getRetrieveTokenApiUrl(tokenId);
        assertEquals("https://api.stripe.com/v1/tokens/" + tokenId, requestApi);
    }

    @Test
    public void testGetRetrieveCustomerUrl() {
        String customerId = "cus_123abc";
        String customerRequestUrl = StripeApiHandler.getRetrieveCustomerUrl(customerId);
        assertEquals("https://api.stripe.com/v1/customers/" + customerId, customerRequestUrl);
    }

    @Test
    public void testGetAddCustomerSourceUrl() {
        String customerId = "cus_123abc";
        String addSourceUrl = StripeApiHandler.getAddCustomerSourceUrl(customerId);
        assertEquals("https://api.stripe.com/v1/customers/" + customerId + "/sources",
                addSourceUrl);
    }

    @Test
    public void testGetDeleteCustomerSourceUrl() {
        String customerId = "cus_123abc";
        String sourceId = "src_456xyz";
        String deleteSourceUrl = StripeApiHandler.getDeleteCustomerSourceUrl(customerId, sourceId);
        assertEquals("https://api.stripe.com/v1/customers/" + customerId + "/sources/"
                        + sourceId,
                deleteSourceUrl);
    }

    @Test
    public void testGetAttachPaymentMethodUrl() {
        final String paymentMethodId = "pm_1ETDEa2eZvKYlo2CN5828c52";
        final String attachUrl = StripeApiHandler.getAttachPaymentMethodUrl(paymentMethodId);
        final String expectedUrl = String.join("", "https://api.stripe.com/v1/payment_methods/",
                paymentMethodId, "/attach");
        assertEquals(expectedUrl, attachUrl);
    }

    @Test
    public void testGetDetachPaymentMethodUrl() {
        final String paymentMethodId = "pm_1ETDEa2eZvKYlo2CN5828c52";
        final String detachUrl = mApiHandler.getDetachPaymentMethodUrl(paymentMethodId);
        final String expectedUrl = String.join("", "https://api.stripe.com/v1/payment_methods/",
                paymentMethodId, "/detach");
        assertEquals(expectedUrl, detachUrl);
    }

    @Test
    public void testGetPaymentMethodsUrl() {
        assertEquals("https://api.stripe.com/v1/payment_methods",
                StripeApiHandler.getPaymentMethodsUrl());
    }

    @Test
    public void testGetIssuingCardPinUrl() {
        assertEquals("https://api.stripe.com/v1/issuing/cards/card123/pin",
                StripeApiHandler.getIssuingCardPinUrl("card123"));
    }

    @Test
    public void testRetrievePaymentIntentUrl() {
        assertEquals("https://api.stripe.com/v1/payment_intents/pi123",
                StripeApiHandler.getRetrievePaymentIntentUrl("pi123"));
    }

    @Test
    public void testConfirmPaymentIntentUrl() {
        assertEquals("https://api.stripe.com/v1/payment_intents/pi123/confirm",
                StripeApiHandler.getConfirmPaymentIntentUrl("pi123"));
    }

    @Test
    public void createSource_shouldLogSourceCreation_andReturnSource()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        final Source source = mApiHandler.createSource(SourceParams.createCardParams(CARD),
                ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY));

        // Check that we get a token back; we don't care about its fields for this test.
        assertNotNull(source);
    }

    @Test
    public void createSource_withConnectAccount_keepsHeaderInAccount()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        final String connectAccountId = "acct_1Acj2PBUgO3KuWzz";
        final Source source = mApiHandler.createSource(SourceParams.createCardParams(CARD),
                ApiRequest.Options.create(ApiKeyFixtures.CONNECTED_ACCOUNT_PUBLISHABLE_KEY, connectAccountId));

        // Check that we get a source back; we don't care about its fields for this test.
        assertNotNull(source);
    }

    @Test
    public void start3ds2Auth_withInvalidSource_shouldThrowInvalidRequestException() {
        final Stripe3ds2AuthParams authParams = new Stripe3ds2AuthParams(
                "src_invalid",
                "1.0.0",
                "3DS_LOA_SDK_STIN_12345",
                UUID.randomUUID().toString(),
                "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAtMjU2In0.nid2Q-Ii21cSPHBaszR5KSXz866yX9I7AthLKpfWZoc7RIfz11UJ1EHuvIRDIyqqJ8txNUKKoL4keqMTqK5Yc5TqsxMn0nML8pZaPn40nXsJm_HFv3zMeOtRR7UTewsDWIgf5J-A6bhowIOmvKPCJRxspn_Cmja-YpgFWTp08uoJvqgntgg1lHmI1kh1UV6DuseYFUfuQlICTqC3TspAzah2CALWZORF_QtSeHc_RuqK02wOQMs-7079jRuSdBXvI6dQnL5ESH25wHHosfjHMZ9vtdUFNJo9J35UI1sdWFDzzj8k7bt0BupZhyeU0PSM9EHP-yv01-MQ9eslPTVNbFJ9YOHtq8WamvlKDr1sKxz6Ac_gUM8NgEcPP9SafPVxDd4H1Fwb5-4NYu2AD4xoAgMWE-YtzvfIFXZcU46NDoi6Xum3cHJqTH0UaOhBoqJJft9XZXYW80fjts-v28TkA76-QPF7CTDM6KbupvBkSoRq218eJLEywySXgCwf-Q95fsBtnnyhKcvfRaByq5kT7PH3DYD1rCQLexJ76A79kurre9pDjTKAv85G9DNkOFuVUYnNB3QGFReCcF9wzkGnZXdfkgN2BkB6n94bbkEyjbRb5r37XH6oRagx2fWLVj7kC5baeIwUPVb5kV_x4Kle7C-FPY1Obz4U7s6SVRnLGXY.IP9OcQx5uZxBRluOpn1m6Q.w-Ko5Qg6r-KCmKnprXEbKA7wV-SdLNDAKqjtuku6hda_0crOPRCPU4nn26Yxj7EG.p01pl8CKukuXzjLeY3a_Ew",
                "{\"kty\":\"EC\",\"use\":\"sig\",\"crv\":\"P-256\",\"kid\":\"b23da28b-d611-46a8-93af-44ad57ce9c9d\",\"x\":\"hSwyaaAp3ppSGkpt7d9G8wnp3aIXelsZVo05EPpqetg\",\"y\":\"OUVOv9xPh5RYWapla0oz3vCJWRRXlDmppy5BGNeSl-A\"}",
                "2.1.0",
                10,

                // TODO(mshafrir): change to "stripe://payment-auth-return"
                null
        );

        final InvalidRequestException invalidRequestException = assertThrows(
                InvalidRequestException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        mApiHandler.start3ds2Auth(authParams,
                                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY);
                    }
                });

        assertEquals("source", invalidRequestException.getParam());
        assertEquals("resource_missing", invalidRequestException.getErrorCode());
    }

    @Test
    public void complete3ds2Auth_withInvalidSource_shouldThrowInvalidRequestException() {
        final InvalidRequestException invalidRequestException = assertThrows(
                InvalidRequestException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        mApiHandler.complete3ds2Auth("src_123",
                                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY);
                    }
                });
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, invalidRequestException.getStatusCode());
        assertEquals("source", invalidRequestException.getParam());
        assertEquals("resource_missing", invalidRequestException.getErrorCode());
    }

    @Test
    public void logApiCall_shouldReturnSuccessful() {
        // This is the one and only test where we actually log something, because
        // we are testing whether or not we log.
        mApiHandler.logApiCall(new HashMap<String, Object>(),
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY);
    }

    @Test
    public void requestData_withConnectAccount_shouldReturnCorrectResponseHeaders()
            throws CardException, APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        final String connectAccountId = "acct_1Acj2PBUgO3KuWzz";
        final StripeResponse response = mApiHandler.makeApiRequest(
                ApiRequest.createPost(
                        StripeApiHandler.getSourcesUrl(),
                        SourceParams.createCardParams(CARD).toParamMap(),
                        ApiRequest.Options.create(
                                ApiKeyFixtures.CONNECTED_ACCOUNT_PUBLISHABLE_KEY,
                                connectAccountId
                        ),
                        null)
        );
        assertNotNull(response);

        final Map<String, List<String>> responseHeaders = response.getResponseHeaders();
        assertNotNull(responseHeaders);

        final List<String> accounts;

        // the Stripe API response will either have a 'Stripe-Account' or 'stripe-account' header,
        // so we need to check both
        if (responseHeaders.containsKey(STRIPE_ACCOUNT_RESPONSE_HEADER)) {
            accounts = responseHeaders.get(STRIPE_ACCOUNT_RESPONSE_HEADER);
        } else if (responseHeaders.containsKey(
                STRIPE_ACCOUNT_RESPONSE_HEADER.toLowerCase(Locale.ROOT))) {
            accounts = responseHeaders.get(STRIPE_ACCOUNT_RESPONSE_HEADER.toLowerCase(Locale.ROOT));
        } else {
            fail("Stripe API response should contain 'Stripe-Account' header");
            accounts = null;
        }

        assertNotNull(accounts);
        assertEquals(1, accounts.size());
        assertEquals(connectAccountId, accounts.get(0));
    }

    @Ignore("requires a secret key")
    public void disabled_confirmPaymentIntent_withSourceData_canSuccessfulConfirm()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        String clientSecret = "temporarily put a private key here simulate the backend";
        String publishableKey = "put a public key that matches the private key here";

        final PaymentIntentParams paymentIntentParams =
                PaymentIntentParams.createConfirmPaymentIntentWithSourceDataParams(
                        SourceParams.createCardParams(CARD),
                        clientSecret,
                        "yourapp://post-authentication-return-url"
                );
        final PaymentIntent paymentIntent = mApiHandler.confirmPaymentIntent(
                paymentIntentParams, ApiRequest.Options.create(publishableKey));

        assertNotNull(paymentIntent);
    }

    @Ignore("requires a secret key")
    public void disabled_confirmPaymentIntent_withSourceId_canSuccessfulConfirm()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        String clientSecret = "temporarily put a private key here simulate the backend";
        String publishableKey = "put a public key that matches the private key here";
        String sourceId = "id of the source created on the backend";

        final PaymentIntentParams paymentIntentParams =
                PaymentIntentParams.createConfirmPaymentIntentWithSourceIdParams(
                        sourceId,
                        clientSecret,
                        "yourapp://post-authentication-return-url"
                );
        final PaymentIntent paymentIntent = mApiHandler.confirmPaymentIntent(
                paymentIntentParams, ApiRequest.Options.create(publishableKey));
        assertNotNull(paymentIntent);
    }

    @Ignore("requires a secret key")
    public void disabled_confirmRetrieve_withSourceId_canSuccessfulRetrieve()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        String clientSecret = "temporarily put a private key here simulate the backend";
        String publishableKey = "put a public key that matches the private key here";

        final PaymentIntent paymentIntent = mApiHandler.retrievePaymentIntent(
                PaymentIntentParams.createRetrievePaymentIntentParams(clientSecret),
                ApiRequest.Options.create(publishableKey)
        );
        assertNotNull(paymentIntent);
    }

    @Test
    public void createSource_withNonLoggingListener_doesNotLogButDoesCreateSource()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        final StripeApiHandler apiHandler = new StripeApiHandler(
                ApplicationProvider.getApplicationContext(),
                new RequestExecutor(),
                false,
                null);
        final Source source = apiHandler.createSource(SourceParams.createCardParams(CARD),
                ApiRequest.Options.create(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY));

        // Check that we get a token back; we don't care about its fields for this test.
        assertNotNull(source);
    }

    @Test
    public void logApiCall_whenShouldLogRequestIsFalse_doesNotCreateAConnection() {
        final StripeApiHandler apiHandler = new StripeApiHandler(
                ApplicationProvider.getApplicationContext(),
                mRequestExecutor,
                false,
                null);
        apiHandler.logApiCall(new HashMap<String, Object>(), ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY);
        verifyNoMoreInteractions(mRequestExecutor);
    }

    @Test
    public void getPaymentMethods_whenPopulated_returnsExpectedList()
            throws StripeException, UnsupportedEncodingException {
        final String responseBody =
                "{\n" +
                "    \"object\": \"list\",\n" +
                "    \"data\": [\n" +
                "        {\n" +
                "            \"id\": \"pm_1EVNYJCRMbs6FrXfG8n52JaK\",\n" +
                "            \"object\": \"payment_method\",\n" +
                "            \"billing_details\": {\n" +
                "                \"address\": {\n" +
                "                    \"city\": null,\n" +
                "                    \"country\": null,\n" +
                "                    \"line1\": null,\n" +
                "                    \"line2\": null,\n" +
                "                    \"postal_code\": null,\n" +
                "                    \"state\": null\n" +
                "                },\n" +
                "                \"email\": null,\n" +
                "                \"name\": null,\n" +
                "                \"phone\": null\n" +
                "            },\n" +
                "            \"card\": {\n" +
                "                \"brand\": \"visa\",\n" +
                "                \"checks\": {\n" +
                "                    \"address_line1_check\": null,\n" +
                "                    \"address_postal_code_check\": null,\n" +
                "                    \"cvc_check\": null\n" +
                "                },\n" +
                "                \"country\": \"US\",\n" +
                "                \"exp_month\": 5,\n" +
                "                \"exp_year\": 2020,\n" +
                "                \"fingerprint\": \"atmHgDo9nxHpQJiw\",\n" +
                "                \"funding\": \"credit\",\n" +
                "                \"generated_from\": null,\n" +
                "                \"last4\": \"4242\",\n" +
                "                \"three_d_secure_usage\": {\n" +
                "                    \"supported\": true\n" +
                "                },\n" +
                "                \"wallet\": null\n" +
                "            },\n" +
                "            \"created\": 1556736791,\n" +
                "            \"customer\": \"cus_EzHwfOXxvAwRIW\",\n" +
                "            \"livemode\": false,\n" +
                "            \"metadata\": {},\n" +
                "            \"type\": \"card\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": \"pm_1EVNXtCRMbs6FrXfTlZGIdGq\",\n" +
                "            \"object\": \"payment_method\",\n" +
                "            \"billing_details\": {\n" +
                "                \"address\": {\n" +
                "                    \"city\": null,\n" +
                "                    \"country\": null,\n" +
                "                    \"line1\": null,\n" +
                "                    \"line2\": null,\n" +
                "                    \"postal_code\": null,\n" +
                "                    \"state\": null\n" +
                "                },\n" +
                "                \"email\": null,\n" +
                "                \"name\": null,\n" +
                "                \"phone\": null\n" +
                "            },\n" +
                "            \"card\": {\n" +
                "                \"brand\": \"visa\",\n" +
                "                \"checks\": {\n" +
                "                    \"address_line1_check\": null,\n" +
                "                    \"address_postal_code_check\": null,\n" +
                "                    \"cvc_check\": null\n" +
                "                },\n" +
                "                \"country\": \"US\",\n" +
                "                \"exp_month\": 5,\n" +
                "                \"exp_year\": 2020,\n" +
                "                \"fingerprint\": \"atmHgDo9nxHpQJiw\",\n" +
                "                \"funding\": \"credit\",\n" +
                "                \"generated_from\": null,\n" +
                "                \"last4\": \"4242\",\n" +
                "                \"three_d_secure_usage\": {\n" +
                "                    \"supported\": true\n" +
                "                },\n" +
                "                \"wallet\": null\n" +
                "            },\n" +
                "            \"created\": 1556736765,\n" +
                "            \"customer\": \"cus_EzHwfOXxvAwRIW\",\n" +
                "            \"livemode\": false,\n" +
                "            \"metadata\": {},\n" +
                "            \"type\": \"card\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": \"src_1EVO8DCRMbs6FrXf2Dspj49a\",\n" +
                "            \"object\": \"payment_method\",\n" +
                "            \"billing_details\": {\n" +
                "                \"address\": {\n" +
                "                    \"city\": null,\n" +
                "                    \"country\": null,\n" +
                "                    \"line1\": null,\n" +
                "                    \"line2\": null,\n" +
                "                    \"postal_code\": null,\n" +
                "                    \"state\": null\n" +
                "                },\n" +
                "                \"email\": null,\n" +
                "                \"name\": null,\n" +
                "                \"phone\": null\n" +
                "            },\n" +
                "            \"card\": {\n" +
                "                \"brand\": \"visa\",\n" +
                "                \"checks\": {\n" +
                "                    \"address_line1_check\": null,\n" +
                "                    \"address_postal_code_check\": null,\n" +
                "                    \"cvc_check\": null\n" +
                "                },\n" +
                "                \"country\": \"US\",\n" +
                "                \"exp_month\": 5,\n" +
                "                \"exp_year\": 2020,\n" +
                "                \"fingerprint\": \"Ep3vs1pdQAjtri7D\",\n" +
                "                \"funding\": \"credit\",\n" +
                "                \"generated_from\": null,\n" +
                "                \"last4\": \"3063\",\n" +
                "                \"three_d_secure_usage\": {\n" +
                "                    \"supported\": true\n" +
                "                },\n" +
                "                \"wallet\": null\n" +
                "            },\n" +
                "            \"created\": 1556739017,\n" +
                "            \"customer\": \"cus_EzHwfOXxvAwRIW\",\n" +
                "            \"livemode\": false,\n" +
                "            \"metadata\": {},\n" +
                "            \"type\": \"card\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"has_more\": false,\n" +
                "    \"url\": \"/v1/payment_methods\"\n" +
                "}";
        final StripeResponse stripeResponse =
                new StripeResponse(200, responseBody, null);
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("customer", "cus_123");
        queryParams.put("type", PaymentMethod.Type.Card.code);

        final ApiRequest.Options options = ApiRequest.Options
                .create(ApiKeyFixtures.FAKE_EPHEMERAL_KEY);
        final String url = ApiRequest.createGet(StripeApiHandler.getPaymentMethodsUrl(),
                queryParams, options, null)
                .getUrl();

        when(mRequestExecutor.execute(argThat(
                new ApiRequestMatcher(
                        StripeRequest.Method.GET,
                        url,
                        options,
                        queryParams))))
                .thenReturn(stripeResponse);
        final StripeApiHandler apiHandler = new StripeApiHandler(
                ApplicationProvider.getApplicationContext(),
                mRequestExecutor,
                false,
                null);
        final List<PaymentMethod> paymentMethods = apiHandler
                .getPaymentMethods("cus_123", PaymentMethod.Type.Card.code,
                        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, new ArrayList<String>(),
                        ApiKeyFixtures.FAKE_EPHEMERAL_KEY);
        assertEquals(3, paymentMethods.size());
        assertEquals("pm_1EVNYJCRMbs6FrXfG8n52JaK", paymentMethods.get(0).id);
        assertEquals("pm_1EVNXtCRMbs6FrXfTlZGIdGq", paymentMethods.get(1).id);
        assertEquals("src_1EVO8DCRMbs6FrXf2Dspj49a", paymentMethods.get(2).id);
    }

    @Test
    public void getPaymentMethods_whenNotPopulated_returnsEmptydList()
            throws StripeException, UnsupportedEncodingException {
        final String responseBody =
                "{\n" +
                        "    \"object\": \"list\",\n" +
                        "    \"data\": [\n" +
                        "    ],\n" +
                        "    \"has_more\": false,\n" +
                        "    \"url\": \"/v1/payment_methods\"\n" +
                        "}";
        final StripeResponse stripeResponse =
                new StripeResponse(200, responseBody, null);
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("customer", "cus_123");
        queryParams.put("type", PaymentMethod.Type.Card.code);

        final ApiRequest.Options options = ApiRequest.Options
                .create(ApiKeyFixtures.FAKE_EPHEMERAL_KEY);
        final String url = ApiRequest.createGet(
                StripeApiHandler.getPaymentMethodsUrl(),
                queryParams,
                options,
                null)
                .getUrl();

        when(mRequestExecutor.execute(argThat(
                new ApiRequestMatcher(
                        StripeRequest.Method.GET,
                        url,
                        options,
                        queryParams))))
                .thenReturn(stripeResponse);
        final StripeApiHandler apiHandler = new StripeApiHandler(
                ApplicationProvider.getApplicationContext(),
                mRequestExecutor,
                false,
                null);
        final List<PaymentMethod> paymentMethods = apiHandler
                .getPaymentMethods("cus_123", PaymentMethod.Type.Card.code,
                        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY, new ArrayList<String>(),
                        ApiKeyFixtures.FAKE_EPHEMERAL_KEY);
        assertTrue(paymentMethods.isEmpty());
    }
}
