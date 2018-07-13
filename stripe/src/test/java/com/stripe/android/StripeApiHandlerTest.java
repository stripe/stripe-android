package com.stripe.android;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
import java.util.List;
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
@Config(constants = BuildConfig.class, sdk = 23)
public class StripeApiHandlerTest {

    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";

    @Test
    public void testGetApiUrl() {
        String tokensApi = StripeApiHandler.getApiUrl();
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
        assertEquals("https://api.stripe.com/v1/customers/" + customerId + "/sources/" + sourceId,
                deleteSourceUrl);
    }

    @Test
    public void getHeaders_withAllRequestOptions_properlyMapsRequestOptions() {
        String fakePublicKey = "fake_public_key";
        String idempotencyKey = "idempotency_rules";
        String stripeAccount = "acct_123abc";
        String apiVersion = "2011-11-11";
        RequestOptions requestOptions = RequestOptions.builder(fakePublicKey)
                .setIdempotencyKey(idempotencyKey)
                .setApiVersion(apiVersion)
                .setStripeAccount(stripeAccount)
                .build();
        Map<String, String> headerMap = StripeApiHandler.getHeaders(requestOptions);

        assertNotNull(headerMap);
        assertEquals("Bearer " + fakePublicKey, headerMap.get("Authorization"));
        assertEquals(idempotencyKey, headerMap.get("Idempotency-Key"));
        assertEquals(apiVersion, headerMap.get("Stripe-Version"));
        assertEquals(stripeAccount, headerMap.get("Stripe-Account"));
    }

    @Test
    public void getHeaders_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        RequestOptions requestOptions = RequestOptions.builder("some_key").build();
        Map<String, String> headerMap = StripeApiHandler.getHeaders(requestOptions);

        assertNotNull(headerMap);
        assertFalse(headerMap.containsKey("Idempotency-Key"));
        assertFalse(headerMap.containsKey("Stripe-Version"));
        assertFalse(headerMap.containsKey("Stripe-Account"));
        assertTrue(headerMap.containsKey("Authorization"));
    }

    @Test
    public void getHeaders_containsPropertyMapValues() {
        RequestOptions requestOptions = RequestOptions.builder("some_key").build();
        Map<String, String> headerMap = StripeApiHandler.getHeaders(requestOptions);

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
        Map<String, String> headerMap = StripeApiHandler.getHeaders(requestOptions);
        assertNotNull(headerMap);

        final String expectedUserAgent =
                String.format("Stripe/v1 AndroidBindings/%s", BuildConfig.VERSION_NAME);
        assertEquals(expectedUserAgent, headerMap.get("User-Agent"));
        assertEquals("application/json", headerMap.get("Accept"));
        assertEquals("UTF-8", headerMap.get("Accept-Charset"));
    }

    @Test
    public void createQuery_withCardData_createsProperQueryString() {
        Card card = new Card.Builder("4242424242424242", 8, 2019, "123").build();
        Map<String, Object> cardMap = StripeNetworkUtils.hashMapFromCard(
                RuntimeEnvironment.application, card);
        String expectedValue = "product_usage=&card%5Bnumber%5D=4242424242424242&card%5B" +
                "cvc%5D=123&card%5Bexp_month%5D=8&card%5Bexp_year%5D=2019";
        try {
            String query = StripeApiHandler.createQuery(cardMap);
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
            Source source = StripeApiHandler.createSource(
                    provider,
                    RuntimeEnvironment.application.getApplicationContext(),
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
    public void createSource_withConnectAccount_keepsHeaderInAccount() {
        try {
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
            Source source = StripeApiHandler.createSource(
                    provider,
                    RuntimeEnvironment.application.getApplicationContext(),
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

            StripeResponse response = testStripeResponseListener.mStripeResponse;
            assertNotNull(response);
            assertNotNull(response.getResponseHeaders());
            assertTrue(response.getResponseHeaders().containsKey("Stripe-Account"));
            List<String> accounts = response.getResponseHeaders().get("Stripe-Account");
            assertEquals(1, accounts.size());
            assertEquals(connectAccountId, accounts.get(0));
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

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
            PaymentIntent paymentIntent = StripeApiHandler.confirmPaymentIntent(
                    null,
                    RuntimeEnvironment.application.getApplicationContext(),
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
            PaymentIntent paymentIntent = StripeApiHandler.confirmPaymentIntent(
                    null,
                    RuntimeEnvironment.application.getApplicationContext(),
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

    public void disabled_confirmRetrieve_withSourceId_canSuccessfulRetrieve() {
        String clientSecret = "temporarily put a private key here simulate the backend";
        String publicKey = "put a public key that matches the private key here";
        try {

            PaymentIntentParams paymentIntentParams = PaymentIntentParams.createRetrievePaymentIntentParams(
                    clientSecret
            );
            PaymentIntent paymentIntent = StripeApiHandler.retrievePaymentIntent(
                    null,
                    RuntimeEnvironment.application.getApplicationContext(),
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
            Source source = StripeApiHandler.createSource(
                    null,
                    RuntimeEnvironment.application.getApplicationContext(),
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
        public void onStripeResponse(StripeResponse response) {
            mStripeResponse = response;
        }
    }

    private static class TestLoggingListener implements StripeApiHandler.LoggingResponseListener {
        boolean mShouldLogTest;
        StripeResponse mStripeResponse;
        StripeException mStripeException;

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
