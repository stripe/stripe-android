package com.stripe.android.paymentsheet.utils

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.R as PaymentsCoreR

internal fun List<SupportedPaymentMethod>.isOnlyOneNonCardPaymentMethod() =
    this.size == 1 && this.first().code != PaymentMethod.Type.Card.code

internal fun List<SupportedPaymentMethod>.addPaymentMethodTitle(): ResolvableString =
    if (singleOrNull()?.code == PaymentMethod.Type.Card.code) {
        PaymentsCoreR.string.stripe_title_add_a_card.resolvableString
    } else {
        R.string.stripe_paymentsheet_choose_payment_method.resolvableString
    }
