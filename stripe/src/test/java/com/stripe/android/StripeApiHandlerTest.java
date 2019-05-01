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

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link StripeApiHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class StripeApiHandlerTest {

    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";

    private static final String STRIPE_ACCOUNT_RESPONSE_HEADER = "Stripe-Account";

    private static final Card CARD =
            new Card("4242424242424242", 1, 2050, "123");

    @NonNull private final StripeApiHandler mApiHandler =
            new StripeApiHandler(ApplicationProvider.getApplicationContext());

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
    public void getHeaders_withAllRequestOptions_properlyMapsRequestOptions() {
        String fakePublicKey = "fake_public_key";
        String idempotencyKey = "idempotency_rules";
        String stripeAccount = "acct_123abc";
        final RequestOptions requestOptions = RequestOptions.builder(fakePublicKey)
                .setIdempotencyKey(idempotencyKey)
                .setStripeAccount(stripeAccount)
                .build();
        final Map<String, String> headerMap = new RequestExecutor.ConnectionFactory()
                .getHeaders(requestOptions);

        assertNotNull(headerMap);
        assertEquals("Bearer " + fakePublicKey, headerMap.get("Authorization"));
        assertEquals(idempotencyKey, headerMap.get("Idempotency-Key"));
        assertEquals(ApiVersion.DEFAULT_API_VERSION, headerMap.get("Stripe-Version"));
        assertEquals(stripeAccount, headerMap.get("Stripe-Account"));
    }

    @Test
    public void getHeaders_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        final Map<String, String> headerMap = new RequestExecutor.ConnectionFactory()
                .getHeaders(RequestOptions.builder("some_key")
                        .build());

        assertFalse(headerMap.containsKey("Idempotency-Key"));
        assertTrue(headerMap.containsKey("Stripe-Version"));
        assertFalse(headerMap.containsKey("Stripe-Account"));
        assertTrue(headerMap.containsKey("Authorization"));
    }

    @Test
    public void getHeaders_containsPropertyMapValues() throws JSONException {
        final Map<String, String> headerMap = new RequestExecutor.ConnectionFactory()
                .getHeaders(RequestOptions.builder("some_key")
                        .build());

        final String userAgentRawString = headerMap.get("X-Stripe-Client-User-Agent");
        final JSONObject mapObject = new JSONObject(userAgentRawString);
        assertEquals(BuildConfig.VERSION_NAME, mapObject.getString("bindings.version"));
        assertEquals("Java", mapObject.getString("lang"));
        assertEquals("Stripe", mapObject.getString("publisher"));
        assertEquals("android", mapObject.getString("os.name"));
        assertTrue(mapObject.has("java.version"));
    }

    @Test
    public void getHeaders_correctlyAddsExpectedAdditionalParameters() {
        final Map<String, String> headerMap = new RequestExecutor.ConnectionFactory()
                .getHeaders(RequestOptions.builder("some_key")
                        .build());

        final String expectedUserAgent =
                String.format(Locale.ROOT, "Stripe/v1 AndroidBindings/%s",
                        BuildConfig.VERSION_NAME);
        assertEquals(expectedUserAgent, headerMap.get("User-Agent"));
        assertEquals("application/json", headerMap.get("Accept"));
        assertEquals("UTF-8", headerMap.get("Accept-Charset"));
    }

    @Test
    public void createQuery_withCardData_createsProperQueryString()
            throws UnsupportedEncodingException, InvalidRequestException {
        final Map<String, Object> cardMap =
                new StripeNetworkUtils(ApplicationProvider.getApplicationContext())
                        .hashMapFromCard(CARD);
        final String expectedValue = "product_usage=&card%5Bnumber%5D=4242424242424242&card%5B" +
                "cvc%5D=123&card%5Bexp_month%5D=1&card%5Bexp_year%5D=2050";
        final String query = new RequestExecutor.ConnectionFactory().createQuery(cardMap);
        assertEquals(expectedValue, query);
    }

    @Test
    public void createSource_shouldLogSourceCreation_andReturnSource()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        final Source source = mApiHandler.createSource(
                SourceParams.createCardParams(CARD),
                FUNCTIONAL_SOURCE_PUBLISHABLE_KEY,
                null
        );

        // Check that we get a token back; we don't care about its fields for this test.
        assertNotNull(source);
    }

    @Test
    public void createSource_withConnectAccount_keepsHeaderInAccount()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        final String connectAccountId = "acct_1Acj2PBUgO3KuWzz";
        final Source source = mApiHandler.createSource(
                SourceParams.createCardParams(CARD),
                "pk_test_fdjfCYpGSwAX24KUEiuaAAWX",
                connectAccountId
        );

        // Check that we get a source back; we don't care about its fields for this test.
        assertNotNull(source);
    }

    @Test
    public void start3ds2Auth_withInvalidSource_shouldThrowInvalidRequestException()
            throws APIConnectionException, APIException, CardException,
            AuthenticationException {
        final Stripe3DS2AuthParams authParams = new Stripe3DS2AuthParams(
                "src_invalid",
                "1.0.0",
                UUID.randomUUID().toString(),
                "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiUlNBLU9BRVAtMjU2In0.nid2Q-Ii21cSPHBaszR5KSXz866yX9I7AthLKpfWZoc7RIfz11UJ1EHuvIRDIyqqJ8txNUKKoL4keqMTqK5Yc5TqsxMn0nML8pZaPn40nXsJm_HFv3zMeOtRR7UTewsDWIgf5J-A6bhowIOmvKPCJRxspn_Cmja-YpgFWTp08uoJvqgntgg1lHmI1kh1UV6DuseYFUfuQlICTqC3TspAzah2CALWZORF_QtSeHc_RuqK02wOQMs-7079jRuSdBXvI6dQnL5ESH25wHHosfjHMZ9vtdUFNJo9J35UI1sdWFDzzj8k7bt0BupZhyeU0PSM9EHP-yv01-MQ9eslPTVNbFJ9YOHtq8WamvlKDr1sKxz6Ac_gUM8NgEcPP9SafPVxDd4H1Fwb5-4NYu2AD4xoAgMWE-YtzvfIFXZcU46NDoi6Xum3cHJqTH0UaOhBoqJJft9XZXYW80fjts-v28TkA76-QPF7CTDM6KbupvBkSoRq218eJLEywySXgCwf-Q95fsBtnnyhKcvfRaByq5kT7PH3DYD1rCQLexJ76A79kurre9pDjTKAv85G9DNkOFuVUYnNB3QGFReCcF9wzkGnZXdfkgN2BkB6n94bbkEyjbRb5r37XH6oRagx2fWLVj7kC5baeIwUPVb5kV_x4Kle7C-FPY1Obz4U7s6SVRnLGXY.IP9OcQx5uZxBRluOpn1m6Q.w-Ko5Qg6r-KCmKnprXEbKA7wV-SdLNDAKqjtuku6hda_0crOPRCPU4nn26Yxj7EG.p01pl8CKukuXzjLeY3a_Ew",
                "{\"kty\":\"EC\",\"use\":\"sig\",\"crv\":\"P-256\",\"kid\":\"b23da28b-d611-46a8-93af-44ad57ce9c9d\",\"x\":\"hSwyaaAp3ppSGkpt7d9G8wnp3aIXelsZVo05EPpqetg\",\"y\":\"OUVOv9xPh5RYWapla0oz3vCJWRRXlDmppy5BGNeSl-A\"}",
                "2.1.0",
                10
        );

        try {
            mApiHandler.start3ds2Auth(authParams, "pk_test_fdjfCYpGSwAX24KUEiuaAAWX");
            fail("Expected InvalidRequestException");
        } catch (InvalidRequestException e) {
            assertEquals("source", e.getParam());
            assertEquals("resource_missing", e.getErrorCode());
        }
    }

    @Test
    public void logApiCall_shouldReturnSuccessful() {
        // This is the one and only test where we actually log something, because
        // we are testing whether or not we log.
        final boolean isSuccessful = mApiHandler.logApiCall(
                new HashMap<String, Object>(),
                RequestOptions.builder("pk_test_fdjfCYpGSwAX24KUEiuaAAWX")
                        .build()
        );
        assertTrue(isSuccessful);
    }

    @Test
    public void requestData_withConnectAccount_shouldReturnCorrectResponseHeaders()
            throws CardException, APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        final String connectAccountId = "acct_1Acj2PBUgO3KuWzz";
        final StripeResponse response = mApiHandler.requestData(
                RequestExecutor.RestMethod.POST, StripeApiHandler.getSourcesUrl(),
                SourceParams.createCardParams(CARD).toParamMap(),
                RequestOptions.builder("pk_test_fdjfCYpGSwAX24KUEiuaAAWX",
                        connectAccountId, RequestOptions.TYPE_QUERY)
                        .build()
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
        String publicKey = "put a public key that matches the private key here";

        final PaymentIntentParams paymentIntentParams =
                PaymentIntentParams.createConfirmPaymentIntentWithSourceDataParams(
                        SourceParams.createCardParams(CARD),
                        clientSecret,
                        "yourapp://post-authentication-return-url"
                );
        final PaymentIntent paymentIntent = mApiHandler.confirmPaymentIntent(
                paymentIntentParams,
                publicKey,
                null
        );

        assertNotNull(paymentIntent);
    }

    @Ignore("requires a secret key")
    public void disabled_confirmPaymentIntent_withSourceId_canSuccessfulConfirm()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        String clientSecret = "temporarily put a private key here simulate the backend";
        String publicKey = "put a public key that matches the private key here";
        String sourceId = "id of the source created on the backend";

        final PaymentIntentParams paymentIntentParams =
                PaymentIntentParams.createConfirmPaymentIntentWithSourceIdParams(
                        sourceId,
                        clientSecret,
                        "yourapp://post-authentication-return-url"
                );
        final PaymentIntent paymentIntent = mApiHandler.confirmPaymentIntent(
                paymentIntentParams,
                publicKey,
                null
        );
        assertNotNull(paymentIntent);
    }

    @Ignore("requires a secret key")
    public void disabled_confirmRetrieve_withSourceId_canSuccessfulRetrieve()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        String clientSecret = "temporarily put a private key here simulate the backend";
        String publicKey = "put a public key that matches the private key here";

        final PaymentIntent paymentIntent = mApiHandler.retrievePaymentIntent(
                PaymentIntentParams.createRetrievePaymentIntentParams(clientSecret),
                publicKey,
                null
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
                false
        );
        final Source source = apiHandler.createSource(
                SourceParams.createCardParams(CARD),
                FUNCTIONAL_SOURCE_PUBLISHABLE_KEY,
                null
        );

        // Check that we get a token back; we don't care about its fields for this test.
        assertNotNull(source);
    }

    @Test
    public void logApiCall_whenShouldLogRequestIsFalse_doesNotCreateAConnection() {
        final StripeApiHandler apiHandler = new StripeApiHandler(
                ApplicationProvider.getApplicationContext(),
                mRequestExecutor,
                false
        );
        apiHandler.logApiCall(new HashMap<String, Object>(),
                RequestOptions.builder("some_key")
                        .build());
        verifyNoMoreInteractions(mRequestExecutor);
    }

    @Test
    public void getPaymentMethods_whenPopulated_returnsExpectedList()
            throws StripeException, JSONException {
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
        final StripeResponse stripeResponse = new StripeResponse(200, responseBody, null);
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("customer", "cus_123");
        queryParams.put("type", PaymentMethod.Type.Card.code);
        when(mRequestExecutor.execute(
                eq(RequestExecutor.RestMethod.GET),
                eq(StripeApiHandler.getPaymentMethodsUrl()),
                eq(queryParams),
                ArgumentMatchers.<RequestOptions>any()))
                .thenReturn(stripeResponse);
        final StripeApiHandler apiHandler = new StripeApiHandler(
                ApplicationProvider.getApplicationContext(),
                mRequestExecutor,
                false
        );
        final List<PaymentMethod> paymentMethods = apiHandler
                .getPaymentMethods("cus_123", PaymentMethod.Type.Card.code,
                        FUNCTIONAL_SOURCE_PUBLISHABLE_KEY, new ArrayList<String>(),
                        "secret");
        assertEquals(3, paymentMethods.size());
        assertEquals("pm_1EVNYJCRMbs6FrXfG8n52JaK", paymentMethods.get(0).id);
        assertEquals("pm_1EVNXtCRMbs6FrXfTlZGIdGq", paymentMethods.get(1).id);
        assertEquals("src_1EVO8DCRMbs6FrXf2Dspj49a", paymentMethods.get(2).id);
    }

    @Test
    public void getPaymentMethods_whenNotPopulated_returnsEmptydList()
            throws StripeException, JSONException {
        final String responseBody =
                "{\n" +
                        "    \"object\": \"list\",\n" +
                        "    \"data\": [\n" +
                        "    ],\n" +
                        "    \"has_more\": false,\n" +
                        "    \"url\": \"/v1/payment_methods\"\n" +
                        "}";
        final StripeResponse stripeResponse = new StripeResponse(200, responseBody, null);
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("customer", "cus_123");
        queryParams.put("type", PaymentMethod.Type.Card.code);
        when(mRequestExecutor.execute(
                eq(RequestExecutor.RestMethod.GET),
                eq(StripeApiHandler.getPaymentMethodsUrl()),
                eq(queryParams),
                ArgumentMatchers.<RequestOptions>any()))
                .thenReturn(stripeResponse);
        final StripeApiHandler apiHandler = new StripeApiHandler(
                ApplicationProvider.getApplicationContext(),
                mRequestExecutor,
                false
        );
        final List<PaymentMethod> paymentMethods = apiHandler
                .getPaymentMethods("cus_123", PaymentMethod.Type.Card.code,
                        FUNCTIONAL_SOURCE_PUBLISHABLE_KEY, new ArrayList<String>(),
                        "secret");
        assertTrue(paymentMethods.isEmpty());
    }
}
