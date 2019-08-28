package com.stripe.android;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GooglePayConfigTest {

    @Test
    public void getTokenizationSpecification() throws JSONException {
        PaymentConfiguration.init(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);
        final JSONObject tokenizationSpec = new GooglePayConfig().getTokenizationSpecification();
        final JSONObject params = tokenizationSpec.getJSONObject("parameters");
        assertEquals("stripe",
                params.getString("gateway"));
        assertEquals(ApiVersion.get().getCode(),
                params.getString("stripe:version"));
        assertEquals(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                params.getString("stripe:publishableKey"));
    }
}