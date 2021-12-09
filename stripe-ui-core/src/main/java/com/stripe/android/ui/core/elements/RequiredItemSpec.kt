package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

/**
 * Identifies a field that can be made hidden.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface RequiredItemSpec {
    val identifier: IdentifierSpec
}
