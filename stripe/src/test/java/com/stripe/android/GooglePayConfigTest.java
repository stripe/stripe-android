package com.stripe.android;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class GooglePayConfigTest {

    @Test
    public void getTokenizationSpecification() throws JSONException {
        PaymentConfiguration.init(ApplicationProvider.getApplicationContext(),
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);
        final JSONObject tokenizationSpec =
                new GooglePayConfig(ApplicationProvider.getApplicationContext())
                        .getTokenizationSpecification();
        final JSONObject params = tokenizationSpec.getJSONObject("parameters");
        assertEquals("stripe",
                params.getString("gateway"));
        assertEquals(ApiVersion.get().getCode(),
                params.getString("stripe:version"));
        assertEquals(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                params.getString("stripe:publishableKey"));
    }
}