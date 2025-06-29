package com.stripe.android.paymentsheet.utils

import com.stripe.android.paymentsheet.PaymentSheet.GooglePayConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.GooglePayConfiguration.ButtonType
import com.stripe.android.paymentsheet.PaymentSheet.GooglePayConfiguration.Environment

fun GooglePayConfiguration.prefillCreate(
    environment: Environment = this.environment,
    countryCode: String = this.countryCode,
    currencyCode: String? = this.currencyCode,
    amount: Long? = this.amount,
    label: String? = this.label,
    buttonType: ButtonType = this.buttonType
): GooglePayConfiguration {
    return GooglePayConfiguration(
        environment = environment,
        countryCode = countryCode,
        currencyCode = currencyCode,
        amount = amount,
        label = label,
        buttonType = buttonType,
    )
}
