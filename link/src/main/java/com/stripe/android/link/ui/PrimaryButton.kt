package com.stripe.android.link.ui

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.R
import com.stripe.android.link.theme.HorizontalPadding
import com.stripe.android.link.theme.linkColors
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.ui.core.Amount

internal enum class PrimaryButtonState {
    Enabled,
    Disabled,
    Processing
}

internal const val progressIndicatorTestTag = "CircularProgressIndicator"

internal fun primaryButtonLabel(
    args: LinkActivityContract.Args,
    resources: Resources
) = if (args.completePayment) {
    when (args.stripeIntent) {
        is PaymentIntent -> Amount(
            requireNotNull(args.stripeIntent.amount),
            requireNotNull(args.stripeIntent.currency)
        ).buildPayButtonLabel(resources)
        is SetupIntent -> resources.getString(R.string.stripe_setup_button_label)
    }
} else {
    resources.getString(R.string.stripe_continue_button_label)
}

@Composable
internal fun PrimaryButton(
    label: String,
    state: PrimaryButtonState,
    @DrawableRes icon: Int? = null,
    onButtonClick: () -> Unit
) {
    val isEnabled = state == PrimaryButtonState.Enabled

    CompositionLocalProvider(
        LocalContentAlpha provides if (isEnabled) ContentAlpha.high else ContentAlpha.disabled,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isEnabled,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    disabledBackgroundColor = MaterialTheme.colors.primary
                )
            ) {
                if (state == PrimaryButtonState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .semantics {
                                testTag = progressIndicatorTestTag
                            },
                        color = MaterialTheme.linkColors.buttonLabel,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = label,
                        color = MaterialTheme.linkColors.buttonLabel
                            .copy(alpha = LocalContentAlpha.current)
                    )
                }
            }
            if (icon != null && state != PrimaryButtonState.Processing) {
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
internal fun PayAnotherWayButton(
    enabled: Boolean,
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
            backgroundColor = MaterialTheme.colors.secondary
        )
    ) {
        Text(
            text = stringResource(id = R.string.wallet_pay_another_way),
            color = MaterialTheme.linkColors.secondaryButtonLabel
        )
    }
}
