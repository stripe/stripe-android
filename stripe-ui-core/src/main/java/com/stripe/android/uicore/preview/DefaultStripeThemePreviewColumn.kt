package com.stripe.android.uicore.preview

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.DefaultStripeTheme
import com.stripe.android.uicore.stripeColors

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun DefaultStripeThemePreviewColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit,
) {
    DefaultStripeTheme {
        Column(
            modifier = modifier.background(color = MaterialTheme.stripeColors.component),
            verticalArrangement = verticalArrangement
        ) {
            content()
        }
    }
}
