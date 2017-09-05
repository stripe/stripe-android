package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertJsonEquals;
import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;

import static org.junit.Assert.fail;

/**
 * Test class for {@link ShippingMethod}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class ShippingMethodTest {

    static final String EXAMPLE_JSON_SHIPPING_ADDRESS = "{" +
            "\"amount\": 599," +
            "\"currency_code\": \"USD\",\n" +
            "\"detail\": \"Arrives tomorrow\"," +
            "\"identifier\": \"fedex\"," +
            "\"label\": \"FedEx\"" +
            "}";

    private static final Map<String, Object> EXAMPLE_MAP_SHIPPING_ADDRESS= new HashMap<String, Object>() {{
        put("amount", 599L);
        put("currency_code", "USD");
        put("detail", "Arrives tomorrow");
        put("identifier", "fedex");
        put("label", "FedEx");
    }};

    private ShippingMethod mShippingMethod =  new ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD");

    @Test
    public void testJSONConversion() {
        try {
            JSONObject rawConversion = new JSONObject(EXAMPLE_JSON_SHIPPING_ADDRESS);
            assertJsonEquals(mShippingMethod.toJson(), rawConversion);
        } catch (JSONException jsonException) {
            fail("Test Data failure: " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void testMapCreation() {
        assertMapEquals(mShippingMethod.toMap(), EXAMPLE_MAP_SHIPPING_ADDRESS);
    }
}
