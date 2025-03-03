package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import com.stripe.android.uicore.stripeColors

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun H6Text(
    text: String,
    modifier: Modifier = Modifier,
    includeFontPadding: Boolean = true,
) {
    Text(
        text = text,
        color = MaterialTheme.stripeColors.subtitle,
        style = MaterialTheme.typography.h6.copy(
            platformStyle = PlatformTextStyle(
                includeFontPadding = includeFontPadding,
            )
        ),
        modifier = modifier,
    )
}
