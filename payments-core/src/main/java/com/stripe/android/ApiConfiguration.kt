package com.stripe.android

import android.os.Parcelable
import androidx.annotation.RestrictTo
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