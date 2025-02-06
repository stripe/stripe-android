package com.stripe.android.financialconnections.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun TestModeBanner(
    enabled: Boolean,
    buttonLabel: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = stringResource(R.string.stripe_verification_inTestMode),
) {
    val contentAlpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = FinancialConnectionsTheme.colors.warningLight,
                shape = RoundedCornerShape(12.dp),
            )
            .alpha(contentAlpha)
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp,
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.stripe_ic_info),
            colorFilter = ColorFilter.tint(
                color = FinancialConnectionsTheme.colors.warning,
            ),
            contentDescription = null,
        )

        Text(
            text = description,
            style = FinancialConnectionsTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = buttonLabel,
            color = FinancialConnectionsTheme.colors.textAction,
            style = FinancialConnectionsTheme.typography.bodyMediumEmphasized,
            modifier = Modifier.clickable(enabled = enabled, onClick = onButtonClick)
        )
    }
}

@Preview(
    group = "Test Mode Banner",
    name = "Enabled",
)
@Composable
internal fun TestModeBannerPreviewEnabled() {
    FinancialConnectionsTheme {
        TestModeBanner(
            enabled = true,
            buttonLabel = "Use test code",
            onButtonClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(
    group = "Test Mode Banner",
    name = "Disabled",
)
@Composable
internal fun TestModeBannerPreviewDisabled() {
    FinancialConnectionsTheme {
        TestModeBanner(
            enabled = false,
            buttonLabel = "Use test code",
            onButtonClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
