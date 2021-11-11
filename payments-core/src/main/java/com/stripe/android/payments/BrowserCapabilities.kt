package com.stripe.android.payments

/**
 * Representation of the device's browser capabilities. Used for determining how to handle
 * browser-based payment authentication.
 */
internal enum class BrowserCapabilities {
    CustomTabs,
    Unknown
}
