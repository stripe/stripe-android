package com.stripe.android.checkout.analytics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * The redacted configuration payload, for activities that report Checkout operations without access
 * to the controller state. [params] holds only maps, lists, strings, booleans, and nulls.
 */
@Parcelize
internal data class CheckoutConfigurationAnalytics(
    val params: @RawValue Map<String, Any?>,
) : Parcelable
