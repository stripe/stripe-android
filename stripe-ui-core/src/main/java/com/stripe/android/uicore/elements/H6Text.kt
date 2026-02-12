package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import com.stripe.android.uicore.stripeColorScheme

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun H6Text(
    text: String,
    modifier: Modifier = Modifier,
    includeFontPadding: Boolean = true,
) {
    Text(
        text = text,
        color = MaterialTheme.stripeColorScheme.subtitle,
        style = MaterialTheme.typography.titleLarge.copy(
            platformStyle = PlatformTextStyle(
                includeFontPadding = includeFontPadding,
            )
        ),
        modifier = modifier,
    )
}
