package com.stripe.android.link.ui.nofundingsources

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.ui.PrimaryButton
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.ScrollableTopLevelColumn
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.strings.resolve

@Composable
internal fun NoFundingSourcesScreen(
    viewModel: NoFundingSourcesViewModel,
) {
    NoFundingSourcesScreen(
        state = viewModel.viewState,
        onPayAnotherWayClicked = viewModel::onPayAnotherWayClicked,
        onLogInToAnotherAccountClicked = viewModel::onLogInToAnotherAccountClicked,
    )
}

@Composable
internal fun NoFundingSourcesScreen(
    state: NoFundingSourcesViewState,
    onPayAnotherWayClicked: () -> Unit,
    onLogInToAnotherAccountClicked: () -> Unit,
) {
    ScrollableTopLevelColumn {
        Spacer(modifier = Modifier.height(48.dp))
        ErrorIcon()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.stripe_link_no_funding_sources_title, state.linkBrand.brandName()),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
            textAlign = TextAlign.Center,
            style = LinkTheme.typography.title,
            color = LinkTheme.colors.textPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.stripe_link_no_funding_sources_message, state.linkBrand.brandName()),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = LinkTheme.typography.body,
            color = LinkTheme.colors.textTertiary,
        )
        Spacer(modifier = Modifier.height(96.dp))
        Buttons(
            payAnotherWayLabel = state.payAnotherWayLabel,
            onPayAnotherWayClicked = onPayAnotherWayClicked,
            onLogInToAnotherAccountClicked = onLogInToAnotherAccountClicked,
        )
    }
}

@Composable
private fun ErrorIcon() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(color = LinkTheme.colors.iconCritical, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_ic_sail_warning_circle),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = LinkTheme.colors.iconWhite,
        )
    }
}

@Composable
private fun Buttons(
    payAnotherWayLabel: ResolvableString?,
    onPayAnotherWayClicked: () -> Unit,
    onLogInToAnotherAccountClicked: () -> Unit,
) {
    val logInToAnotherAccountLabel = stringResource(R.string.stripe_link_log_in_to_another_account)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (payAnotherWayLabel != null) {
            PrimaryButton(
                modifier = Modifier.testTag(NO_FUNDING_SOURCES_PAY_ANOTHER_WAY_BUTTON_TAG),
                label = payAnotherWayLabel.resolve(),
                state = PrimaryButtonState.Enabled,
                onButtonClick = onPayAnotherWayClicked,
            )
            OutlinedSecondaryButton(
                modifier = Modifier.testTag(NO_FUNDING_SOURCES_LOG_IN_TO_ANOTHER_ACCOUNT_BUTTON_TAG),
                label = logInToAnotherAccountLabel,
                onClick = onLogInToAnotherAccountClicked,
            )
        } else {
            // When the user can't leave Link, logging in to another account is the only action,
            // so it becomes the primary button.
            PrimaryButton(
                modifier = Modifier.testTag(NO_FUNDING_SOURCES_LOG_IN_TO_ANOTHER_ACCOUNT_BUTTON_TAG),
                label = logInToAnotherAccountLabel,
                state = PrimaryButtonState.Enabled,
                onButtonClick = onLogInToAnotherAccountClicked,
            )
        }
    }
}

@Composable
private fun OutlinedSecondaryButton(
    modifier: Modifier,
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(LinkTheme.shapes.primaryButtonHeight),
        shape = LinkTheme.shapes.primaryButton,
        border = BorderStroke(width = 1.dp, color = LinkTheme.colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = LinkTheme.colors.textPrimary,
        ),
    ) {
        Text(
            text = label,
            style = LinkTheme.typography.bodyEmphasized,
        )
    }
}

@PreviewLightDark
@Composable
internal fun NoFundingSourcesScreenPreview() {
    DefaultLinkTheme {
        NoFundingSourcesScreen(
            state = NoFundingSourcesViewState(
                linkBrand = LinkBrand.Link,
                payAnotherWayLabel = R.string.stripe_wallet_pay_another_way.resolvableString,
            ),
            onPayAnotherWayClicked = {},
            onLogInToAnotherAccountClicked = {},
        )
    }
}

@PreviewLightDark
@Composable
internal fun NoFundingSourcesScreenCannotContinueWithoutLinkPreview() {
    DefaultLinkTheme {
        NoFundingSourcesScreen(
            state = NoFundingSourcesViewState(
                linkBrand = LinkBrand.Link,
                payAnotherWayLabel = null,
            ),
            onPayAnotherWayClicked = {},
            onLogInToAnotherAccountClicked = {},
        )
    }
}

internal const val NO_FUNDING_SOURCES_PAY_ANOTHER_WAY_BUTTON_TAG = "no_funding_sources_pay_another_way_button"
internal const val NO_FUNDING_SOURCES_LOG_IN_TO_ANOTHER_ACCOUNT_BUTTON_TAG =
    "no_funding_sources_log_in_to_another_account_button"
