package com.stripe.android;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.model.Card;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class StripeRequestTest {
    private static final Card CARD =
            new Card("4242424242424242", 1, 2050, "123");

    @Test
    public void getHeaders_withAllRequestOptions_properlyMapsRequestOptions() {
        final String fakePublicKey = "fake_public_key";
        final String stripeAccount = "acct_123abc";
        final Map<String, String> headerMap =
                StripeRequest.createGet(StripeApiHandler.getSourcesUrl(),
                        RequestOptions.createForApi(fakePublicKey, stripeAccount))
                        .getHeaders(ApiVersion.getDefault());

        assertNotNull(headerMap);
        assertEquals("Bearer " + fakePublicKey, headerMap.get("Authorization"));
        assertEquals(ApiVersion.DEFAULT_API_VERSION, headerMap.get("Stripe-Version"));
        assertEquals(stripeAccount, headerMap.get("Stripe-Account"));
    }

    @Test
    public void getHeaders_withOnlyRequiredOptions_doesNotAddEmptyOptions() {
        final Map<String, String> headerMap =
                StripeRequest.createGet(StripeApiHandler.getSourcesUrl(),
                        RequestOptions.createForApi("some_key"))
                        .getHeaders(ApiVersion.getDefault());

        assertTrue(headerMap.containsKey("Stripe-Version"));
        assertFalse(headerMap.containsKey("Stripe-Account"));
        assertTrue(headerMap.containsKey("Authorization"));
    }

    @Test
    public void getHeaders_containsPropertyMapValues() throws JSONException {
        final Map<String, String> headerMap =
                StripeRequest.createGet(StripeApiHandler.getSourcesUrl(),
                        RequestOptions.createForApi("some_key"))
                        .getHeaders(ApiVersion.getDefault());

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
        final Map<String, String> headerMap =
                StripeRequest.createGet(StripeApiHandler.getSourcesUrl(),
                        RequestOptions.createForApi("some_key"))
                        .getHeaders(ApiVersion.getDefault());

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
        final String query = StripeRequest.createGet(StripeApiHandler.getSourcesUrl(), cardMap,
                RequestOptions.createForApi("some_key"))
                .createQuery();
        assertEquals(expectedValue, query);
    }

    @Test
    public void getContentType_withRequestTypeApi() {
        final String contentType = StripeRequest.createGet(StripeApiHandler.getSourcesUrl(),
                RequestOptions.createForApi("some_key"))
                .getContentType();
        assertEquals("application/x-www-form-urlencoded; charset=UTF-8", contentType);
    }

    @Test
    public void getContentType_withRequestTypeFingerprinting() {
        final String contentType = StripeRequest.createGet(StripeApiHandler.getSourcesUrl(),
                RequestOptions.createForFingerprinting("guid"))
                .getContentType();
        assertEquals("application/json; charset=UTF-8", contentType);
    }
}
