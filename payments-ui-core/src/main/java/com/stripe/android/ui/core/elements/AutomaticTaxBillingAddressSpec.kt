package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

/**
 * Runtime-only form item for automatic-tax billing address collection. It is never serialized.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AutomaticTaxBillingAddressSpec(
    val allowedCountryCodes: Set<String>,
)
