package com.stripe.android.link.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.stripe.android.link.theme.PrimaryButtonHeight
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes

@Composable
internal fun SecondaryButton(
    enabled: Boolean,
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(PrimaryButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.linkShapes.medium,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            disabledBackgroundColor = MaterialTheme.colors.secondary
        )
    ) {
        CompositionLocalProvider(
            LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled
        ) {
            Text(
                text = label,
                color = MaterialTheme.linkColors.secondaryButtonLabel
                    .copy(alpha = LocalContentAlpha.current)
            )
        }
    }
}
