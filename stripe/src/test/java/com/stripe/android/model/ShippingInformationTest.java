package com.stripe.android.model;

import androidx.annotation.NonNull;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShippingInformationTest {

    @Test
    public void testEquals() {
        assertEquals(createShippingInformation(), createShippingInformation());
    }

    @NonNull
    private ShippingInformation createShippingInformation() {
        return new ShippingInformation(Address.fromString(AddressTest.JSON_ADDRESS),
                "home", "555-123-4567");
    }
}