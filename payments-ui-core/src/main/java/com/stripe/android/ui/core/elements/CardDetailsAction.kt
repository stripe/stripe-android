package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardDetailsAction {
    @Composable
    fun Content(enabled: Boolean)
}
