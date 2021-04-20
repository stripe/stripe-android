package com.stripe.android.payments

import android.content.Context

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
