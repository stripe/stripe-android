package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.IdentifierSpec
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Runtime-only form item that is resolved after server specs have been parsed and never serialized.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class AutomaticTaxBillingAddressSpec(
    val allowedCountryCodes: Set<String>,
) : FormItemSpec() {
    @IgnoredOnParcel
    override val apiPath: IdentifierSpec = IdentifierSpec.BillingAddress
}
