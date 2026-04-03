package com.stripe.android.core.browser

import androidx.annotation.RestrictTo

/**
 * Representation of the device's browser capabilities. Used for determining how to handle
 * browser-based payment authentication.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class BrowserCapabilities {
    CustomTabs,
    Unknown
}
