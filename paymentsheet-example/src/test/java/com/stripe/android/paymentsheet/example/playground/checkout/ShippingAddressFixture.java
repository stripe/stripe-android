package com.stripe.android.paymentsheet.example.playground.checkout;

import com.stripe.android.checkout.CheckoutController.Session;

final class ShippingAddressFixture {
    private ShippingAddressFixture() {
    }

    static Session.ShippingAddress create(
            String name,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country
    ) {
        return new Session.ShippingAddress(
                name,
                new Session.ShippingAddress.Address(city, country, line1, line2, postalCode, state)
        );
    }
}
