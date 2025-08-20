package com.stripe.android.link.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.LinkTheme
import com.stripe.android.link.theme.LinkThemeConfig.contentOnPrimaryButton
import com.stripe.android.link.theme.PrimaryButtonHeight
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R as uiCoreR

@Composable
internal fun PrimaryButton(
    modifier: Modifier = Modifier,
    label: String,
    state: PrimaryButtonState,
    onButtonClick: () -> Unit,
    allowedDisabledClicks: Boolean = false,
    onDisabledButtonClick: () -> Unit = {},
    @DrawableRes iconStart: Int? = null,
    @DrawableRes iconEnd: Int? = null
) {
    CompositionLocalProvider(
        LocalContentAlpha provides
            if (state == PrimaryButtonState.Disabled) ContentAlpha.disabled else ContentAlpha.high
    ) {
        Box(modifier) {
            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .height(PrimaryButtonHeight)
                    .fillMaxWidth()
                    .testTag(PrimaryButtonTag),
                enabled = state == PrimaryButtonState.Enabled,
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                shape = LinkTheme.shapes.default,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = LinkTheme.colors.buttonBrand,
                    disabledBackgroundColor = LinkTheme.colors.buttonBrand,
                )
            ) {
                PrimaryContent(
                    state = state,
                    label = label,
                    iconStart = iconStart,
                    iconEnd = iconEnd,
                )
            }

            DisabledButton(
                state = state,
                allowedDisabledClicks = allowedDisabledClicks,
                onDisabledButtonClick = onDisabledButtonClick,
            )
        }
    }
}

@Composable
private fun PrimaryContent(
    state: PrimaryButtonState,
    label: String,
    @DrawableRes iconStart: Int? = null,
    @DrawableRes iconEnd: Int? = null
) {
    when (state) {
        PrimaryButtonState.Processing -> LinkSpinner(
            modifier = Modifier
                .size(20.dp)
                .semantics { testTag = ProgressIndicatorTestTag },
            backgroundColor = LinkTheme.colors.surfaceBackdrop.copy(alpha = 0.1f),
            strokeWidth = 4.dp,
            filledColor = LinkTheme.colors.contentOnPrimaryButton,
        )
        PrimaryButtonState.Completed -> Icon(
            painter = painterResource(id = R.drawable.stripe_link_complete),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .semantics {
                    testTag = CompletedIconTestTag
                },
            tint = LinkTheme.colors.contentOnPrimaryButton
        )
        else -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButtonIcon(iconStart)
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = LinkTheme.colors.contentOnPrimaryButton
                    .copy(alpha = LocalContentAlpha.current),
                textAlign = TextAlign.Center,
                style = LinkTheme.typography.bodyEmphasized,
            )
            PrimaryButtonIcon(iconEnd)
        }
    }
}

@Composable
private fun BoxScope.DisabledButton(
    state: PrimaryButtonState,
    allowedDisabledClicks: Boolean,
    onDisabledButtonClick: () -> Unit,
) {
    if (state == PrimaryButtonState.Disabled && allowedDisabledClicks) {
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { onDisabledButtonClick.invoke() }
                }
        )
    }
}

@Composable
private fun PrimaryButtonIcon(
    @DrawableRes icon: Int?
) {
    Box(
        modifier = Modifier
            .width(PrimaryButtonIconWidth)
            .height(PrimaryButtonIconHeight),
        contentAlignment = Alignment.Center
    ) {
        icon?.let { icon ->
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier
                    .width(PrimaryButtonIconWidth)
                    .height(PrimaryButtonIconHeight),
                tint = LinkTheme.colors.contentOnPrimaryButton.copy(alpha = LocalContentAlpha.current)
            )
        }
    }
}

/**
 * Represent the possible states for the primary button on a Link screen.
 *
 * @property isBlocking Whether being in this state should block user interaction with all other
 *                      UI elements.
 */
internal enum class PrimaryButtonState(val isBlocking: Boolean) {
    Enabled(false),
    Disabled(false),
    Processing(true),
    Completed(true)
}

internal fun completePaymentButtonLabel(
    stripeIntent: StripeIntent,
    linkLaunchMode: LinkLaunchMode,
): ResolvableString = when (linkLaunchMode) {
    is LinkLaunchMode.Full,
    is LinkLaunchMode.Confirmation -> when (stripeIntent) {
        is PaymentIntent -> {
            Amount(
                requireNotNull(stripeIntent.amount),
                requireNotNull(stripeIntent.currency)
            ).buildPayButtonLabel()
        }
        is SetupIntent -> {
            uiCoreR.string.stripe_continue_button_label.resolvableString
        }
    }
    is LinkLaunchMode.PaymentMethodSelection,
    is LinkLaunchMode.Authentication -> uiCoreR.string.stripe_continue_button_label.resolvableString
}

private val PrimaryButtonIconWidth = 13.dp
private val PrimaryButtonIconHeight = 16.dp
internal const val ProgressIndicatorTestTag = "CircularProgressIndicator"
internal const val CompletedIconTestTag = "CompletedIcon"
internal const val PrimaryButtonTag = "PrimaryButtonTag"

@Composable
@Preview
private fun PrimaryButtonPreview() {
    DefaultLinkTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryButton(
                label = "Testing",
                state = PrimaryButtonState.Enabled,
                onButtonClick = { },
                iconEnd = uiCoreR.drawable.stripe_ic_lock
            )
            PrimaryButton(
                label = "Testing",
                state = PrimaryButtonState.Processing,
                onButtonClick = { },
                iconEnd = uiCoreR.drawable.stripe_ic_lock
            )
        }
    }
}
