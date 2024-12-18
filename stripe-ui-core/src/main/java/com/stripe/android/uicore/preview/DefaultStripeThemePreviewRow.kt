package com.stripe.android.uicore.preview

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.DefaultStripeTheme
import com.stripe.android.uicore.stripeColors

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun DefaultStripeThemePreviewRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DefaultStripeTheme {
        Row(
            modifier = modifier.background(color = MaterialTheme.stripeColors.component)
        ) {
            content()
        }
    }
}
