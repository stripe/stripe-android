package com.stripe.android.link.ui

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.linkColors
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.ui.core.Amount

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
    Completed(true);

    companion object {
        // The delay between showing the [Completed] state and dismissing the screen.
        const val COMPLETED_DELAY_MS = 1000L
    }
}

internal const val progressIndicatorTestTag = "CircularProgressIndicator"
internal const val completedIconTestTag = "CompletedIcon"

internal fun primaryButtonLabel(
    args: LinkActivityContract.Args,
    resources: Resources
) = when (args.stripeIntent) {
    is PaymentIntent -> Amount(
        requireNotNull(args.stripeIntent.amount),
        requireNotNull(args.stripeIntent.currency)
    ).buildPayButtonLabel(resources)
    is SetupIntent -> resources.getString(R.string.stripe_setup_button_label)
}

@Composable
@Preview
private fun PrimaryButton() {
    DefaultLinkTheme {
        PrimaryButton(
            label = "Testing",
            state = PrimaryButtonState.Enabled,
            icon = R.drawable.stripe_ic_lock,
            onButtonClick = { }
        )
    }
}

@Composable
internal fun PrimaryButton(
    label: String,
    state: PrimaryButtonState,
    @DrawableRes icon: Int? = null,
    onButtonClick: () -> Unit
) {
    CompositionLocalProvider(
        LocalContentAlpha provides
            if (state == PrimaryButtonState.Disabled) ContentAlpha.disabled else ContentAlpha.high
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state == PrimaryButtonState.Enabled,
                shape = MaterialTheme.shapes.medium,
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
                                testTag = progressIndicatorTestTag
                            },
                        color = MaterialTheme.linkColors.buttonLabel,
                        strokeWidth = 2.dp
                    )
                    PrimaryButtonState.Completed -> Icon(
                        painter = painterResource(id = R.drawable.ic_link_complete),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .semantics {
                                testTag = completedIconTestTag
                            },
                        tint = MaterialTheme.linkColors.buttonLabel
                    )
                    else -> Text(
                        text = label,
                        color = MaterialTheme.linkColors.buttonLabel
                            .copy(alpha = LocalContentAlpha.current)
                    )
                }
            }
            // Show icon only when button label is visible
            if (icon != null &&
                state in setOf(PrimaryButtonState.Enabled, PrimaryButtonState.Disabled)
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier
                        .height(16.dp)
                        // width should be 13dp and must include the horizontal padding
                        .width(13.dp + 40.dp)
                        .padding(horizontal = HorizontalPadding),
                    tint = MaterialTheme.linkColors.buttonLabel.copy(alpha = LocalContentAlpha.current)
                )
            }
        }
    }
}

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
            .height(56.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
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
