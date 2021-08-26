package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ListPaymentMethodsParams(
    private val customerId: String,
    internal val paymentMethodType: PaymentMethod.Type,
    private val limit: Int? = null,
    private val endingBefore: String? = null,
    private val startingAfter: String? = null
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return listOf(
            PARAM_CUSTOMER to customerId,
            PARAM_TYPE to paymentMethodType.code,
            PARAM_LIMIT to limit,
            PARAM_ENDING_BEFORE to endingBefore,
            PARAM_STARTING_AFTER to startingAfter
        ).fold(emptyMap()) { acc, (key, value) ->
            acc.plus(
                value?.let { mapOf(key to it) }.orEmpty()
            )
        }
    }

    private companion object {
        private const val PARAM_CUSTOMER = "customer"
        private const val PARAM_TYPE = "type"
        private const val PARAM_LIMIT = "limit"
        private const val PARAM_ENDING_BEFORE = "ending_before"
        private const val PARAM_STARTING_AFTER = "starting_after"
    }
}
