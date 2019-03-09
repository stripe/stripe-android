package com.stripe.android;

import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for {@link StripeApiHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public class StripeApiHandlerTest {

    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";

    private static final String STRIPE_ACCOUNT_RESPONSE_HEADER = "stripe-account";

    private StripeApiHandler mApiHandler;

    @Before
    public void before() {
        mApiHandler = new StripeApiHandler();
    }

    @Test
    public void testGetApiUrl() {
        String tokensApi = mApiHandler.getTokensUrl();
        assertEquals("https://api.stripe.com/v1/tokens", tokensApi);
    }

    @Test
    public void testGetSourcesUrl() {
        String sourcesUrl = mApiHandler.getSourcesUrl();
        assertEquals("https://api.stripe.com/v1/sources", sourcesUrl);
    }

    @Test
    public void testGetRetrieveSourceUrl() {
        String sourceUrlWithId = mApiHandler.getRetrieveSourceApiUrl("abc123");
        assertEquals("https://api.stripe.com/v1/sources/abc123", sourceUrlWithId);
    }

    @Test
    public void testGetRequestTokenApiUrl() {
        String tokenId = "tok_sample";
        String requestApi = mApiHandler.getRetrieveTokenApiUrl(tokenId);
        assertEquals("https://api.stripe.com/v1/tokens/" + tokenId, requestApi);
    }

    @Test
    public void testGetRetrieveCustomerUrl() {
        String customerId = "cus_123abc";
        String customerRequestUrl = mApiHandler.getRetrieveCustomerUrl(customerId);
        assertEquals("https://api.stripe.com/v1/customers/" + customerId, customerRequestUrl);
    }

    @Test
    public void testGetAddCustomerSourceUrl() {
        String customerId = "cus_123abc";
        String addSourceUrl = mApiHandler.getAddCustomerSourceUrl(customerId);
        assertEquals("https://api.stripe.com/v1/customers/" + customerId + "/sources",
                addSourceUrl);
    }

    @Test
    public void testGetDeleteCustomerSourceUrl() {
        String customerId = "cus_123abc";
        String sourceId = "src_456xyz";
        String deleteSourceUrl = mApiHandler.getDeleteCustomerSourceUrl(customerId, sourceId);
        assertEquals("https://api.stripe.com/v1/customers/" + customerId + "/sources/" + sourceId,
                deleteSourceUrl);
    }

    @Test
    public void getHeaders_withAllRequestOptions_properlyMapsRequestOptions() {
        String fakePublicKey = "fake_public_key";
        String idempotencyKey = "idempotency_rules";
        String stripeAccount = "acct_123abc";
        RequestOptions requestOptions = RequestOptions.builder(fakePublicKey)
                .setIdempotencyKey(idempotencyKey)
                .setStripeAccount(stripeAccount)
                .build();
        Map<String, String> headerMap = mApiHandler.getHeaders(requestOptions);

        assertNotNull(headerMap);
        assertEquals("Bearer " + fakePublicKey, headerMap.get("Authorization"));
        assertEquals(idempotencyKey, headerMap.get("Idempotency-Key"));
        assertEquals(ApiVersion.DEFAULT_API_VERSION, headerMap.get("Stripe-Version"));
        assertEquals(stripeAccount, headerMap.get("Stripe-Account"));
    }

    @Test
    public void getHeaders_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        final RequestOptions requestOptions = RequestOptions.builder("some_key")
                .build();
        Map<String, String> headerMap = mApiHandler.getHeaders(requestOptions);

        assertNotNull(headerMap);
        assertFalse(headerMap.containsKey("Idempotency-Key"));
        assertTrue(headerMap.containsKey("Stripe-Version"));
        assertFalse(headerMap.containsKey("Stripe-Account"));
        assertTrue(headerMap.containsKey("Authorization"));
    }

    @Test
    public void getHeaders_containsPropertyMapValues() {
        RequestOptions requestOptions = RequestOptions.builder("some_key").build();
        Map<String, String> headerMap = mApiHandler.getHeaders(requestOptions);

        assertNotNull(headerMap);
        String userAgentRawString = headerMap.get("X-Stripe-Client-User-Agent");
        try {
            JSONObject mapObject = new JSONObject(userAgentRawString);
            assertEquals(BuildConfig.VERSION_NAME, mapObject.getString("bindings.version"));
            assertEquals("Java", mapObject.getString("lang"));
            assertEquals("Stripe", mapObject.getString("publisher"));
            assertEquals("android", mapObject.getString("os.name"));
            assertTrue(mapObject.has("java.version"));
        } catch (JSONException jsonException) {
            fail("Failed to get a parsable JsonObject for the user agent.");
        }
    }

    @Test
    public void getHeaders_correctlyAddsExpectedAdditionalParameters() {
        RequestOptions requestOptions = RequestOptions.builder("some_key").build();
        Map<String, String> headerMap = mApiHandler.getHeaders(requestOptions);
        assertNotNull(headerMap);

        final String expectedUserAgent =
                String.format(Locale.ROOT, "Stripe/v1 AndroidBindings/%s", BuildConfig.VERSION_NAME);
        assertEquals(expectedUserAgent, headerMap.get("User-Agent"));
        assertEquals("application/json", headerMap.get("Accept"));
        assertEquals("UTF-8", headerMap.get("Accept-Charset"));
    }

    @Test
    public void createQuery_withCardData_createsProperQueryString() {
        Card card = new Card.Builder("4242424242424242", 8, 2019, "123").build();
        Map<String, Object> cardMap = StripeNetworkUtils.hashMapFromCard(
                ApplicationProvider.getApplicationContext(), card);
        String expectedValue = "product_usage=&card%5Bnumber%5D=4242424242424242&card%5B" +
                "cvc%5D=123&card%5Bexp_month%5D=8&card%5Bexp_year%5D=2019";
        try {
            String query = mApiHandler.createQuery(cardMap);
            assertEquals(expectedValue, query);
        } catch (UnsupportedEncodingException unsupportedCodingException) {
            fail("Encoding error with card object");
        } catch (InvalidRequestException invalidRequest) {
            fail("Invalid request error when encoding card query: "
                    + invalidRequest.getLocalizedMessage());
        }
    }

    @Test
    public void createSource_shouldLogSourceCreation_andReturnSource() {
        try {
            // This is the one and only test where we actually log something, because
            // we are testing whether or not we log.
            TestLoggingListener testLoggingListener = new TestLoggingListener(true);

            StripeNetworkUtils.UidProvider provider = new StripeNetworkUtils.UidProvider() {
                @Override
                public String getUid() {
                    return "abc123";
                }

                @Override
                public String getPackageName() {
                    return "com.example.main";
                }
            };

            Card card = new Card("4242424242424242", 1, 2050, "123");
            Source source = mApiHandler.createSource(
                    provider,
                    ApplicationProvider.getApplicationContext(),
                    SourceParams.createCardParams(card),
                    FUNCTIONAL_SOURCE_PUBLISHABLE_KEY,
                    null,
                    testLoggingListener);

            // Check that we get a token back; we don't care about its fields for this test.
            assertNotNull(source);

            assertNull(testLoggingListener.mStripeException);
            assertNotNull(testLoggingListener.mStripeResponse);
            Assert.assertEquals(200, testLoggingListener.mStripeResponse.getResponseCode());

        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSource_withConnectAccount_keepsHeaderInAccount()
            throws APIException, AuthenticationException, InvalidRequestException,
            APIConnectionException {
        // This is the one and only test where we actually log something, because
        // we are testing whether or not we log.
        TestLoggingListener testLoggingListener = new TestLoggingListener(true);
        TestStripeResponseListener testStripeResponseListener =
                new TestStripeResponseListener();

        StripeNetworkUtils.UidProvider provider = new StripeNetworkUtils.UidProvider() {
            @Override
            public String getUid() {
                return "abc123";
            }

            @Override
            public String getPackageName() {
                return "com.example.main";
            }
        };

        final String connectAccountId = "acct_1Acj2PBUgO3KuWzz";
        Card card = new Card("4242424242424242", 1, 2050, "123");
        Source source = mApiHandler.createSource(
                provider,
                ApplicationProvider.getApplicationContext(),
                SourceParams.createCardParams(card),
                "pk_test_fdjfCYpGSwAX24KUEiuaAAWX",
                connectAccountId,
                testLoggingListener,
                testStripeResponseListener);

        // Check that we get a source back; we don't care about its fields for this test.
        assertNotNull(source);

        assertNull(testLoggingListener.mStripeException);
        assertNotNull(testLoggingListener.mStripeResponse);
        assertEquals(200, testLoggingListener.mStripeResponse.getResponseCode());

        final StripeResponse response = testStripeResponseListener.mStripeResponse;
        assertNotNull(response);

        final Map<String, List<String>> responseHeaders = response.getResponseHeaders();
        assertNotNull(responseHeaders);
        assertTrue(responseHeaders.containsKey(STRIPE_ACCOUNT_RESPONSE_HEADER));

        final List<String> accounts = responseHeaders.get(STRIPE_ACCOUNT_RESPONSE_HEADER);
        assertNotNull(accounts);
        assertEquals(1, accounts.size());
        assertEquals(connectAccountId, accounts.get(0));
    }

    @Ignore
    public void disabled_confirmPaymentIntent_withSourceData_canSuccessfulConfirm() {
        String clientSecret = "temporarily put a private key here simulate the backend";
        String publicKey = "put a public key that matches the private key here";
        try {

            Card card = new Card("4242424242424242", 1, 2050, "123");
            PaymentIntentParams paymentIntentParams = PaymentIntentParams.createConfirmPaymentIntentWithSourceDataParams(
                    SourceParams.createCardParams(card),
                    clientSecret,
                    null
            );
            PaymentIntent paymentIntent = mApiHandler.confirmPaymentIntent(
                    null,
                    ApplicationProvider.getApplicationContext(),
                    paymentIntentParams,
                    publicKey,
                    null,
                    null);

            assertNotNull(paymentIntent);
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

    @Ignore
    public void disabled_confirmPaymentIntent_withSourceId_canSuccessfulConfirm() {
        String clientSecret = "temporarily put a private key here simulate the backend";
        String publicKey = "put a public key that matches the private key here";
        String sourceId = "id of the source created on the backend";
        try {
            PaymentIntentParams paymentIntentParams = PaymentIntentParams.createConfirmPaymentIntentWithSourceIdParams(
                    sourceId,
                    clientSecret,
                    null
            );
            PaymentIntent paymentIntent = mApiHandler.confirmPaymentIntent(
                    null,
                    ApplicationProvider.getApplicationContext(),
                    paymentIntentParams,
                    publicKey,
                    null,
                    null);
            assertNotNull(paymentIntent);
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

    @Ignore
    public void disabled_confirmRetrieve_withSourceId_canSuccessfulRetrieve() {
        String clientSecret = "temporarily put a private key here simulate the backend";
        String publicKey = "put a public key that matches the private key here";
        try {

            PaymentIntentParams paymentIntentParams = PaymentIntentParams.createRetrievePaymentIntentParams(
                    clientSecret
            );
            PaymentIntent paymentIntent = mApiHandler.retrievePaymentIntent(
                    ApplicationProvider.getApplicationContext(),
                    paymentIntentParams,
                    publicKey,
                    null,
                    null);
            assertNotNull(paymentIntent);
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSource_withNonLoggingListener_doesNotLogButDoesCreateSource() {
        try {
            TestLoggingListener testLoggingListener = new TestLoggingListener(false);

            Card card = new Card("4242424242424242", 1, 2050, "123");
            Source source = mApiHandler.createSource(
                    null,
                    ApplicationProvider.getApplicationContext(),
                    SourceParams.createCardParams(card),
                    FUNCTIONAL_SOURCE_PUBLISHABLE_KEY,
                    null,
                    testLoggingListener);

            // Check that we get a token back; we don't care about its fields for this test.
            assertNotNull(source);

            assertNull(testLoggingListener.mStripeException);
            assertNull(testLoggingListener.mStripeResponse);
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

    private static class TestStripeResponseListener
            implements StripeApiHandler.StripeResponseListener {
        StripeResponse mStripeResponse;

        @Override
        public void onStripeResponse(@NonNull StripeResponse response) {
            mStripeResponse = response;
        }
    }

    private static class TestLoggingListener implements StripeApiHandler.LoggingResponseListener {
        private boolean mShouldLogTest;
        private StripeResponse mStripeResponse;
        private StripeException mStripeException;

        TestLoggingListener(boolean shouldLogTest) {
            mShouldLogTest = shouldLogTest;
        }

        @Override
        public boolean shouldLogTest() {
            return mShouldLogTest;
        }

        @Override
        public void onLoggingResponse(StripeResponse response) {
            mStripeResponse = response;
        }

        @Override
        public void onStripeException(StripeException exception) {
            mStripeException = exception;
        }
    }
}
