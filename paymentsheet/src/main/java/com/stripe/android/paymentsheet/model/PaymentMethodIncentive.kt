package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.model.LinkConsumerIncentive
import com.stripe.android.uicore.format.CurrencyFormatter
import kotlinx.parcelize.Parcelize
import java.util.Locale
import kotlin.math.roundToInt

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun LinkConsumerIncentive.toPaymentMethodIncentive(): PaymentMethodIncentive? {
    return if (incentiveParams.amountFlat != null && incentiveParams.currency != null) {
        PaymentMethodIncentive(
            identifier = incentiveParams.paymentMethod,
            displayText = CurrencyFormatter.format(
                amount = incentiveParams.amountFlat!!,
                amountCurrencyCode = incentiveParams.currency!!,
                targetLocale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault(),
                compact = true,
            ),
        )
    } else if (incentiveParams.amountPercent != null) {
        PaymentMethodIncentive(
            identifier = incentiveParams.paymentMethod,
            displayText = "${(incentiveParams.amountPercent!! * 100).roundToInt()}%",
        )
    } else {
        null
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class PaymentMethodIncentive(
    val identifier: String,
    val displayText: String,
) : Parcelable
