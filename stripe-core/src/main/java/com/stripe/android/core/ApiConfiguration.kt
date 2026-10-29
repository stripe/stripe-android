package com.stripe.android.core

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Holds API credentials and optional beta API version tokens for use with
 * payment UI components. When not provided, components fall back to
 * [PaymentConfiguration.getInstance].
 *
 * @param publishableKey the Stripe publishable key for this payment session.
 */
@ApiConfigurationPreview
class ApiConfiguration(
    private val publishableKey: String,
) {
    private var stripeAccountId: String? = null
    private var betas: Set<String> = emptySet()

    /**
     * Sets the connected account ID for this payment session. Pass null to use no connected account.
     */
    fun stripeAccountId(stripeAccountId: String?) = apply {
        this.stripeAccountId = stripeAccountId
    }

    /**
     * Sets beta API version tokens for this payment session, for example `example_beta=v1`.
     * Replaces any previously supplied tokens. The supplied set is copied.
     */
    fun betas(betas: Set<String>) = apply {
        this.betas = betas.toSet()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun build() = State(
        publishableKey = publishableKey,
        stripeAccountId = stripeAccountId,
        betas = betas.toSet(),
    )

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class State(
        val publishableKey: String,
        val stripeAccountId: String?,
        val betas: Set<String>,
    ) : Parcelable {
        fun isLiveMode(): Boolean {
            return !publishableKey.startsWith("pk_test")
        }
    }
}
