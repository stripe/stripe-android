package com.stripe.android.model;

import androidx.annotation.NonNull;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test class for {@link ShippingMethod}
 */
public class ShippingMethodTest {
    private final static ShippingMethod SHIPPING_METHOD = createShippingMethod();

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
