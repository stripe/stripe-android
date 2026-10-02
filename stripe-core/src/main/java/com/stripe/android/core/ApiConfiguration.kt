package com.stripe.android.core

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Holds API credentials and optional beta headers for use with payment UI components.
 * When not provided, components fall back to [PaymentConfiguration.getInstance].
 */
class ApiConfiguration(
    private val publishableKey: String,
) {
    private var stripeAccountId: String? = null
    private var betas: Set<String> = emptySet()

    fun stripeAccountId(stripeAccountId: String?) = apply {
        this.stripeAccountId = stripeAccountId
    }

    /**
     * Sets beta version tokens to include in the `Stripe-Version` header.
     *
     * Only use beta tokens provided by Stripe. Calling this method replaces any previously set
     * tokens. Passing an empty set clears them.
     */
    fun betas(betas: Set<String>) = apply {
        this.betas = betas.toSet()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun build() = State(
        publishableKey = publishableKey,
        stripeAccountId = stripeAccountId,
        betas = betas,
    )

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class State(
        val publishableKey: String,
        val stripeAccountId: String?,
        val betas: Set<String> = emptySet(),
    ) : Parcelable {
        fun isLiveMode(): Boolean {
            return !publishableKey.startsWith("pk_test")
        }
    }
}
