package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface IncentiveEligibilitySession : Parcelable {

    val id: String
    val elementsSessionId: String

    fun toParamMap(): Map<String, Any>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class PaymentIntent(
        override val id: String,
        override val elementsSessionId: String,
    ) : IncentiveEligibilitySession {

        override fun toParamMap(): Map<String, Any> {
            return mapOf(
                "financial_incentive" to mapOf(
                    "payment_intent" to id,
                    "elements_session_id" to elementsSessionId,
                )
            )
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class SetupIntent(
        override val id: String,
        override val elementsSessionId: String,
    ) : IncentiveEligibilitySession {

        override fun toParamMap(): Map<String, Any> {
            return mapOf(
                "financial_incentive" to mapOf(
                    "setup_intent" to id,
                    "elements_session_id" to elementsSessionId,
                )
            )
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class DeferredIntent(
        override val id: String,
    ) : IncentiveEligibilitySession {

        override val elementsSessionId: String
            get() = id

        override fun toParamMap(): Map<String, Any> {
            return mapOf("financial_incentive[elements_session_id]" to id)
        }
    }
}
