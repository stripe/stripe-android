package com.stripe.android.core

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Holds API credentials (publishable key and optional Stripe account ID) for use with
 * payment UI components. When not provided, components fall back to
 * [PaymentConfiguration.getInstance].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
        fun isLiveMode(): Boolean {
            return !publishableKey.startsWith("pk_test")
        }
    }
}