package com.stripe.android.paymentsheet.ui

import android.content.res.ColorStateList
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.pay.button.ButtonTheme.Dark
import com.google.pay.button.ButtonTheme.Light
import com.google.pay.button.ButtonType
import com.google.pay.button.PayButton
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.convertDpToPx
import org.json.JSONArray

@Composable
internal fun GooglePayButton(
    state: PrimaryButton.State,
    allowCreditCards: Boolean,
    billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters?,
    isEnabled: Boolean,
    onPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val height = dimensionResource(R.dimen.stripe_paymentsheet_primary_button_height)

    val cornerRadius = remember {
        context.convertDpToPx(
            StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
        ).toInt()
    }

    val allowedPaymentMethods = remember(allowCreditCards, billingAddressParameters) {
        JSONArray().put(
            GooglePayJsonFactory(context).createCardPaymentMethod(
                billingAddressParameters = billingAddressParameters,
                allowCreditCards = allowCreditCards,
            )
        ).toString()
    }

    val alpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.5f,
        label = "GooglePayButtonAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(height)
            .testTag(GOOGLE_PAY_BUTTON_TEST_TAG),
    ) {
        when (state) {
            is PrimaryButton.State.Ready -> {
                PayButton(
                    onClick = {
                        if (isEnabled) {
                            onPressed()
                        }
                    },
                    allowedPaymentMethods = allowedPaymentMethods,
                    theme = if (isSystemInDarkTheme()) Light else Dark,
                    type = ButtonType.Buy,
                    radius = maxOf(1, cornerRadius).dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(alpha)
                        .testTag(GOOGLE_PAY_BUTTON_PAY_BUTTON_TEST_TAG),
                )
            }
            is PrimaryButton.State.StartProcessing,
            is PrimaryButton.State.FinishProcessing -> {
                PrimaryButtonWrapper(
                    state = state,
                    isEnabled = isEnabled,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(GOOGLE_PAY_BUTTON_PRIMARY_BUTTON_TEST_TAG),
                )
            }
        }
    }
}

@Composable
private fun PrimaryButtonWrapper(
    state: PrimaryButton.State?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            PrimaryButton(context).apply {
                setAppearanceConfiguration(
                    StripeTheme.primaryButtonStyle,
                    null
                )
                val backgroundColor = ContextCompat.getColor(
                    context,
                    R.color.stripe_paymentsheet_googlepay_primary_button_background_color
                )
                finishedBackgroundColor = backgroundColor
                backgroundTintList = ColorStateList.valueOf(backgroundColor)
                setLockIconDrawable(
                    R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_lock
                )
                setIndicatorColor(
                    ContextCompat.getColor(
                        context,
                        R.color.stripe_paymentsheet_googlepay_primary_button_tint_color,
                    )
                )
                setConfirmedIconDrawable(
                    R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_checkmark
                )
                setDefaultLabelColor(
                    ContextCompat.getColor(
                        context,
                        R.color.stripe_paymentsheet_googlepay_primary_button_tint_color
                    )
                )
            }
        },
        update = { primaryButton ->
            primaryButton.updateState(state)
            primaryButton.isEnabled = isEnabled
        },
        modifier = modifier,
    )
}

internal const val GOOGLE_PAY_BUTTON_TEST_TAG = "google-pay-button"
internal const val GOOGLE_PAY_BUTTON_PAY_BUTTON_TEST_TAG = "google-pay-button-pay-button"
internal const val GOOGLE_PAY_BUTTON_PRIMARY_BUTTON_TEST_TAG = "google-pay-button-primary-button"
