package com.stripe.android.payments

import android.content.Context

/**
 * The SDK's "default" `return_url`. When a PaymentIntent or SetupIntent is confirmed without
 * a custom `return_url` via the SDK, [value] will be used instead.
 */
internal data class DefaultReturnUrl(
    private val packageName: String
) {
    /**
     * Must match the pattern used in [StripeBrowserProxyReturnActivity]'s intent filter.
     */
    val value: String get() = PREFIX + packageName

    companion object {
        const val PREFIX = "stripesdk://payment_return_url/"

        fun create(context: Context) = DefaultReturnUrl(context.packageName)
    }
}
