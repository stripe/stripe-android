package com.stripe.android.googlepaylauncher

import com.stripe.android.GooglePayJsonFactory

internal fun GooglePayLauncher.BillingAddressConfig.convert() =
    GooglePayJsonFactory.BillingAddressParameters(
        isRequired,
        when (format) {
            GooglePayLauncher.BillingAddressConfig.Format.Min ->
                GooglePayJsonFactory.BillingAddressParameters.Format.Min
            GooglePayLauncher.BillingAddressConfig.Format.Full ->
                GooglePayJsonFactory.BillingAddressParameters.Format.Full
        },
        isPhoneNumberRequired
    )

internal fun GooglePayPaymentMethodLauncher.BillingAddressConfig.convert() =
    GooglePayJsonFactory.BillingAddressParameters(
        isRequired,
        when (format) {
            GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Min ->
                GooglePayJsonFactory.BillingAddressParameters.Format.Min
            GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full ->
                GooglePayJsonFactory.BillingAddressParameters.Format.Full
        },
        isPhoneNumberRequired
    )
