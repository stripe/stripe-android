package com.stripe.form

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun ContentBox(
    spec: ContentSpec,
    modifier: Modifier = Modifier,
) {
    spec.Content(modifier)
}
