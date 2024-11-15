package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.model.LinkConsumerIncentive
import com.stripe.android.model.PaymentMethod
import com.stripe.android.uicore.format.CurrencyFormatter
import kotlinx.parcelize.Parcelize
import java.util.Locale
import kotlin.math.roundToInt

internal fun LinkConsumerIncentive.toPaymentMethodIncentive(): PaymentMethodIncentive? {
    return if (incentiveParams.amountFlat != null && incentiveParams.currency != null) {
        PaymentMethodIncentive(
            identifier = incentiveParams.paymentMethod,
            type = mapToType(incentiveParams.paymentMethod),
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
            type = mapToType(incentiveParams.paymentMethod),
            displayText = "${(incentiveParams.amountPercent!! * 100).roundToInt()}%",
        )
    } else {
        null
    }
}

private fun mapToType(identifier: String): PaymentMethod.Type? {
    return when (identifier) {
        "link_instant_debits" -> PaymentMethod.Type.Link
        else -> null
    }
}

@Parcelize
internal data class PaymentMethodIncentive(
    val identifier: String,
    val type: PaymentMethod.Type?,
    val displayText: String,
) : Parcelable
