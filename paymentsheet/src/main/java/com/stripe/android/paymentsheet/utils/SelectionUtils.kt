@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package com.stripe.android.paymentsheet.utils

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun PaymentSelection.getSetAsDefaultPaymentMethodFromPaymentSelection(): Boolean? {
    return when (this) {
        is PaymentSelection.New.Card -> {
            (this.paymentMethodExtraParams as? PaymentMethodExtraParams.Card)?.setAsDefault
        }
        is PaymentSelection.New.USBankAccount -> {
            (this.paymentMethodExtraParams as? PaymentMethodExtraParams.USBankAccount)?.setAsDefault
        }
        else -> {
            null
        }
    }
}
