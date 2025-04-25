package com.stripe.android.link.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.PrimaryButtonHeight
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes

@Composable
internal fun SecondaryButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(PrimaryButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.linkShapes.medium,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            disabledBackgroundColor = MaterialTheme.colors.secondary
        )
    ) {
        Text(
            text = label,
            color = MaterialTheme.linkColors.secondaryButtonLabel
                .copy(alpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled)
        )
    }
}

@Composable
@Preview
private fun SecondaryButtonPreview() {
    DefaultLinkTheme {
        SecondaryButton(
            enabled = true,
            label = "Testing",
            onClick = {}
        )
    }
}
