package com.stripe.android

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Opt-in annotation for [ApiConfiguration], which is currently in preview.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ApiConfigurationPreview

/**
 * Holds API credentials (publishable key and optional Stripe account ID) for use with
 * payment UI components. When not provided, components fall back to
 * [PaymentConfiguration.getInstance].
 */
@ApiConfigurationPreview
@Parcelize
class ApiConfiguration private constructor(
    internal val state: State,
) : Parcelable {
    /**
     * Creates an [ApiConfiguration] with the given publishable key.
     *
     * @param publishableKey Your Stripe publishable key.
     */
    constructor(publishableKey: String) : this(State(publishableKey = publishableKey))

    /**
     * Builder for [ApiConfiguration].
     */
    class Builder(private val publishableKey: String) {
        private var stripeAccountId: String? = null

        /**
         * Sets the Stripe account ID for connected account requests.
         */
        fun stripeAccountId(id: String): Builder = apply {
            this.stripeAccountId = id
        }

        /**
         * Builds the [ApiConfiguration].
         */
        fun build(): ApiConfiguration = ApiConfiguration(
            State(
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
            )
        )
    }

    @Parcelize
    internal data class State(
        val publishableKey: String,
        val stripeAccountId: String? = null,
    ) : Parcelable
}
