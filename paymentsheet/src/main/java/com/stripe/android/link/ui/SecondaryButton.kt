package com.stripe.android.link.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme

private const val DisabledAlpha = 0.38f

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
            .height(56.dp),
        enabled = enabled,
        shape = LinkTheme.shapes.default,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        )
    ) {
        Text(
            text = label,
            color = LinkTheme.colors.textBrand
                .copy(alpha = if (enabled) 1f else DisabledAlpha),
            style = LinkTheme.typography.bodyEmphasized,
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
