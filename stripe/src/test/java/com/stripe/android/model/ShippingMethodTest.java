package com.stripe.android.model;

import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.testharness.JsonTestUtils.assertMapEquals;
import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link ShippingMethod}
 */
public class ShippingMethodTest {

    private static final Map<String, Object> EXAMPLE_MAP_SHIPPING_ADDRESS =
            new HashMap<String, Object>() {{
                put("amount", 599L);
                put("currency_code", "USD");
                put("detail", "Arrives tomorrow");
                put("identifier", "fedex");
                put("label", "FedEx");
            }};

    private final static ShippingMethod SHIPPING_METHOD = createShippingMethod();

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
