package com.stripe.android

import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import com.stripe.android.view.BillingAddressFields
import com.stripe.android.view.PaymentFlowActivityStarter
import com.stripe.android.view.ShippingInfoWidget

internal object PaymentSessionFixtures {
    internal val CONFIG = PaymentSessionConfig.Builder()
        // hide the phone field on the shipping information form
        .setHiddenShippingInfoFields(
            ShippingInfoWidget.CustomizableShippingField.Line2
        )
        // make the address line 2 field optional
        .setOptionalShippingInfoFields(
            ShippingInfoWidget.CustomizableShippingField.Phone
        )
        // specify an address to pre-populate the shipping information form
        .setPrepopulatedShippingInfo(
            ShippingInformation(
                Address.Builder()
                    .setLine1("123 Market St")
                    .setCity("San Francisco")
                    .setState("CA")
                    .setPostalCode("94107")
                    .setCountry("US")
                    .build(),
                "Jenny Rosen",
                "4158675309"
            )
        )
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
        // Enable PaymentMethod Deletion from PaymentMethodActivity
        // This is default behavior
        .setCanDeletePaymentMethods(true)
        .setShippingInformationValidator(FakeShippingInformationValidator())
        .setShippingMethodsFactory(FakeShippingMethodsFactory())
        .build()

    internal val PAYMENT_SESSION_DATA = PaymentSessionData(CONFIG)

    internal val PAYMENT_FLOW_ARGS = PaymentFlowActivityStarter.Args(
        paymentSessionConfig = CONFIG,
        paymentSessionData = PAYMENT_SESSION_DATA
    )

    private class FakeShippingInformationValidator : PaymentSessionConfig.ShippingInformationValidator {
        override fun isValid(shippingInformation: ShippingInformation): Boolean {
            return true
        }

        override fun getErrorMessage(shippingInformation: ShippingInformation): String {
            return ""
        }
    }

    private class FakeShippingMethodsFactory : PaymentSessionConfig.ShippingMethodsFactory {
        override fun create(shippingInformation: ShippingInformation): List<ShippingMethod> {
            return listOf(
                ShippingMethod(
                    "UPS Ground",
                    "ups-ground",
                    0,
                    "USD",
                    "Arrives in 3-5 days"
                ),
                ShippingMethod(
                    "FedEx",
                    "fedex",
                    599,
                    "USD",
                    "Arrives tomorrow"
                )
            )
        }
    }
}
