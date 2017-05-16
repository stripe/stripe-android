package com.stripe.android.net;

import com.stripe.android.BuildConfig;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.util.StripeNetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.UnsupportedEncodingException;
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
    public void getHeaders_withAllRequestOptions_properlyMapsRequestOptions() {
        String fakePublicKey = "fake_public_key";
        String idempotencyKey = "idempotency_rules";
        String apiVersion = "2011-11-11";
        RequestOptions requestOptions = RequestOptions.builder(fakePublicKey)
                .setIdempotencyKey(idempotencyKey)
                .setApiVersion(apiVersion)
                .build();
        Map<String, String> headerMap = StripeApiHandler.getHeaders(requestOptions);

        assertNotNull(headerMap);
        assertEquals("Bearer " + fakePublicKey, headerMap.get("Authorization"));
        assertEquals(idempotencyKey, headerMap.get("Idempotency-Key"));
        assertEquals(apiVersion, headerMap.get("Stripe-Version"));
    }

    @Test
    public void getHeaders_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        RequestOptions requestOptions = RequestOptions.builder("some_key").build();
        Map<String, String> headerMap = StripeApiHandler.getHeaders(requestOptions);

        assertNotNull(headerMap);
        assertFalse(headerMap.containsKey("Idempotency-Key"));
        assertFalse(headerMap.containsKey("Stripe-Version"));
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

            Card card = new Card("4242424242424242", 1, 2050, "123");
            Source source = StripeApiHandler.createSourceOnServer(
                    SourceParams.createCardParams(card),
                    FUNCTIONAL_SOURCE_PUBLISHABLE_KEY,
                    testLoggingListener);

            // Check that we get a token back; we don't care about its fields for this test.
            assertNotNull(source);

            assertNull(testLoggingListener.mStripeException);
            assertNotNull(testLoggingListener.mStripeResponse);
            assertEquals(200, testLoggingListener.mStripeResponse.getResponseCode());

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
            Source source = StripeApiHandler.createSourceOnServer(
                    SourceParams.createCardParams(card),
                    FUNCTIONAL_SOURCE_PUBLISHABLE_KEY,
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

    private static class TestLoggingListener implements StripeApiHandler.LoggingResponseListener {
        boolean mShouldLogTest;
        StripeResponse mStripeResponse;
        StripeException mStripeException;

        public TestLoggingListener(boolean shouldLogTest) {
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
