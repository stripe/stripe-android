package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface IncentiveEligibilitySession : Parcelable {

    val id: String

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class PaymentIntent(override val id: String) : IncentiveEligibilitySession

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class SetupIntent(override val id: String) : IncentiveEligibilitySession

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class DeferredIntent(override val id: String) : IncentiveEligibilitySession

    fun toParamMap(): Map<String, String> {
        val key = when (this) {
            is PaymentIntent -> "financial_incentive[payment_intent]"
            is SetupIntent -> "financial_incentive[setup_intent]"
            is DeferredIntent -> "financial_incentive[elements_session_id]"
        }

        return mapOf(key to id)
    }
}
