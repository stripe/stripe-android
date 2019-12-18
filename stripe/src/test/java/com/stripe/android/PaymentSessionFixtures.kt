package com.stripe.android

import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.view.BillingAddressFields
import com.stripe.android.view.ShippingInfoWidget

internal object PaymentSessionFixtures {
    internal val PAYMENT_SESSION_CONFIG = PaymentSessionConfig.Builder()

        // hide the phone field on the shipping information form
        .setHiddenShippingInfoFields(
            ShippingInfoWidget.CustomizableShippingField.ADDRESS_LINE_TWO_FIELD
        )

        // make the address line 2 field optional
        .setOptionalShippingInfoFields(
            ShippingInfoWidget.CustomizableShippingField.PHONE_FIELD
        )

        // specify an address to pre-populate the shipping information form
        .setPrepopulatedShippingInfo(ShippingInformation(
            Address.Builder()
                .setLine1("123 Market St")
                .setCity("San Francisco")
                .setState("CA")
                .setPostalCode("94107")
                .setCountry("US")
                .build(),
            "Jenny Rosen",
            "4158675309"
        ))

        // collect shipping information
        .setShippingInfoRequired(true)

        // collect shipping method
        .setShippingMethodsRequired(true)

        // specify the payment method types that the customer can use;
        // defaults to PaymentMethod.Type.Card
        .setPaymentMethodTypes(
            listOf(PaymentMethod.Type.Card)
        )

        // only allowed US and Canada shipping addresses
        .setAllowedShippingCountryCodes(
            setOf("US", "CA")
        )

        .setBillingAddressFields(BillingAddressFields.Full)

        .setShouldPrefetchCustomer(true)

        .build()

    internal val PAYMENT_SESSION_DATA = PaymentSessionData(PAYMENT_SESSION_CONFIG)
}
