package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

/**
 * Configuration interface specifically for CVC text fields.
 * This extends [CardDetailsTextFieldConfig] to provide domain-specific constraints
 * for CVC input fields while allowing for testability with fake implementations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CvcTextFieldConfig : CardDetailsTextFieldConfig
