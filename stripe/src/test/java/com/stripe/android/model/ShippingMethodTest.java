package com.stripe.android.model;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertJsonEquals;
import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test class for {@link ShippingMethod}
 */
@RunWith(RobolectricTestRunner.class)
public class ShippingMethodTest {

    private static final String EXAMPLE_JSON_SHIPPING_ADDRESS = "{" +
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

    private final static ShippingMethod SHIPPING_METHOD = createShippingMethod();

    @Test
    public void testJSONConversion() {
        try {
            JSONObject rawConversion = new JSONObject(EXAMPLE_JSON_SHIPPING_ADDRESS);
            assertJsonEquals(SHIPPING_METHOD.toJson(), rawConversion);
        } catch (JSONException jsonException) {
            fail("Test Data failure: " + jsonException.getLocalizedMessage());
        }
    }

    @Test
    public void testMapCreation() {
        assertMapEquals(SHIPPING_METHOD.toMap(), EXAMPLE_MAP_SHIPPING_ADDRESS);
    }

    @Test
    public void testEquals() {
        assertEquals(SHIPPING_METHOD, createShippingMethod());
    }

    @Test
    public void testHashcode() {
        assertEquals(SHIPPING_METHOD.hashCode(), createShippingMethod().hashCode());
    }

    @NonNull
    private static ShippingMethod createShippingMethod() {
        return new ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD");
    }
}
