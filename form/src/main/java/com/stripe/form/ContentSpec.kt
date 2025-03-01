package com.stripe.form

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
interface ContentSpec {
    @Composable
    fun Content(modifier: Modifier)
}
