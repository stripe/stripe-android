package com.stripe.android.link.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.PrimaryButtonHeight
import com.stripe.android.link.theme.linkColors
import com.stripe.android.link.theme.linkShapes
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R

@Composable
internal fun PrimaryButton(
    modifier: Modifier = Modifier,
    label: String,
    state: PrimaryButtonState,
    onButtonClick: () -> Unit,
    @DrawableRes iconStart: Int? = null,
    @DrawableRes iconEnd: Int? = null
) {
    CompositionLocalProvider(
        LocalContentAlpha provides
            if (state == PrimaryButtonState.Disabled) ContentAlpha.disabled else ContentAlpha.high
    ) {
        Box(modifier.padding(vertical = 16.dp)) {
            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .height(PrimaryButtonHeight)
                    .fillMaxWidth(),
                enabled = state == PrimaryButtonState.Enabled,
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                shape = MaterialTheme.linkShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    disabledBackgroundColor = MaterialTheme.colors.primary
                )
            ) {
                when (state) {
                    PrimaryButtonState.Processing -> CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .semantics {
                                testTag = ProgressIndicatorTestTag
                            },
                        color = MaterialTheme.linkColors.buttonLabel,
                        strokeWidth = 2.dp
                    )
                    PrimaryButtonState.Completed -> Icon(
                        painter = painterResource(id = com.stripe.android.link.R.drawable.stripe_link_complete),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .semantics {
                                testTag = CompletedIconTestTag
                            },
                        tint = MaterialTheme.linkColors.buttonLabel
                    )
                    else -> Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrimaryButtonIcon(iconStart)
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.linkColors.buttonLabel
                                .copy(alpha = LocalContentAlpha.current),
                            textAlign = TextAlign.Center
                        )
                        PrimaryButtonIcon(iconEnd)
                    }
                }
            }
        }
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
                tint = MaterialTheme.linkColors.buttonLabel.copy(alpha = LocalContentAlpha.current)
            )
        }
    }
}

internal fun completePaymentButtonLabel(
    stripeIntent: StripeIntent,
) = when (stripeIntent) {
    is PaymentIntent -> {
        Amount(
            requireNotNull(stripeIntent.amount),
            requireNotNull(stripeIntent.currency)
        ).buildPayButtonLabel()
    }
    is SetupIntent -> R.string.stripe_setup_button_label.resolvableString
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

private val PrimaryButtonIconWidth = 13.dp
private val PrimaryButtonIconHeight = 16.dp
internal const val ProgressIndicatorTestTag = "CircularProgressIndicator"
internal const val CompletedIconTestTag = "CompletedIcon"

@Composable
@Preview
private fun PrimaryButtonPreview() {
    DefaultLinkTheme {
        PrimaryButton(
            label = "Testing",
            state = PrimaryButtonState.Enabled,
            onButtonClick = { },
            iconEnd = R.drawable.stripe_ic_lock
        )
    }
}
