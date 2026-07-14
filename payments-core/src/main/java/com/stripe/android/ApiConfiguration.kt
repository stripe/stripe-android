package com.stripe.android

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Configuration options to be used for all API requests
 */
class ApiConfiguration(
    private val publishableKey: String,
) {
    private var stripeAccountId: String? = null

    fun stripeAccountId(stripeAccountId: String?) = apply {
        this.stripeAccountId = stripeAccountId
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun build() = State(
        publishableKey = publishableKey,
        stripeAccountId = stripeAccountId
    )

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class State(
        val publishableKey: String,
        val stripeAccountId: String?
    ) : Parcelable {

        fun toPaymentConfiguration() = PaymentConfiguration(publishableKey, stripeAccountId)
    }
}
