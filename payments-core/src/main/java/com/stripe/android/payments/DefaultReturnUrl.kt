package com.stripe.android.payments

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * The SDK's "default" `return_url`. When a PaymentIntent or SetupIntent is confirmed without
 * a custom `return_url` via the SDK, [value] will be used instead.
 */
internal data class DefaultReturnUrl(
    private val packageName: String
) {
    /**
     * Must match the pattern used in `StripeBrowserLauncherActivity`'s intent filter.
     */
    val value: String get() = "stripesdk://payment_return_url/$packageName"

    companion object {
        fun create(context: Context) = DefaultReturnUrl(context.packageName)
    }
}
