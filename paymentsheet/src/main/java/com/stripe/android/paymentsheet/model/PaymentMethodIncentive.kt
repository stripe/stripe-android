package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import com.stripe.android.model.LinkConsumerIncentive
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PaymentMethodIncentive(
    private val identifier: String,
    val displayText: String,
) : Parcelable {

    fun matches(code: PaymentMethodCode): Boolean {
        return identifier == "link_instant_debits" && code == PaymentMethod.Type.Link.code
    }
}

internal fun LinkConsumerIncentive.toPaymentMethodIncentive(): PaymentMethodIncentive? {
    return incentiveDisplayText?.let { displayText ->
        PaymentMethodIncentive(
            identifier = incentiveParams.paymentMethod,
            displayText = displayText,
        )
    }
}
