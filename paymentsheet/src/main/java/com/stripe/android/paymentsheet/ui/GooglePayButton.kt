package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.wallet.button.ButtonConstants.ButtonTheme
import com.google.android.gms.wallet.button.ButtonConstants.ButtonType
import com.google.android.gms.wallet.button.ButtonOptions
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.convertDpToPx
import org.json.JSONArray
import com.google.android.gms.wallet.button.PayButton as GmsWalletPayButton

@Composable
internal fun GooglePayButton(
    state: PrimaryButton.State?,
    allowCreditCards: Boolean,
    @ButtonType buttonType: Int,
    billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters?,
    isEnabled: Boolean,
    onPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current

    val allowedPaymentMethods = remember(
        context,
        isInspectionMode,
        billingAddressParameters,
        allowCreditCards
    ) {
        if (isInspectionMode) {
            ""
        } else {
            JSONArray().put(
                GooglePayJsonFactory(context).createCardPaymentMethod(
                    billingAddressParameters = billingAddressParameters,
                    allowCreditCards = allowCreditCards
                )
            ).toString()
        }
    }

    val buttonTheme = if (isSystemInDarkTheme()) {
        ButtonTheme.LIGHT
    } else {
        ButtonTheme.DARK
    }

    when (state) {
        null,
        is PrimaryButton.State.Ready -> ComposePayButton(
            modifier = modifier,
            allowedPaymentMethods = allowedPaymentMethods,
            buttonType = buttonType,
            buttonTheme = buttonTheme,
            radius = PrimaryButtonTheme.shape.cornerRadius,
            enabled = isEnabled,
            onPressed = onPressed,
        )
        is PrimaryButton.State.StartProcessing,
        is PrimaryButton.State.FinishProcessing -> GooglePrimaryButton(
            modifier = modifier,
            state = state,
        )
    }
}

@Composable
private fun ComposePayButton(
    allowedPaymentMethods: String,
    @ButtonType buttonType: Int,
    @ButtonTheme buttonTheme: Int,
    radius: Dp,
    enabled: Boolean,
    onPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth().testTag(GOOGLE_PAY_BUTTON_TEST_TAG),
        factory = { context -> GmsWalletPayButton(context) },
        update = { button ->
            button.apply {
                initialize(
                    ButtonOptions.newBuilder()
                        .setButtonType(buttonType)
                        .setButtonTheme(buttonTheme)
                        .setCornerRadius(button.context.convertDpToPx(radius).toInt())
                        .setAllowedPaymentMethods(allowedPaymentMethods)
                        .build()
                )

                alpha = if (enabled) FULL_ALPHA else HALF_ALPHA
                isEnabled = enabled

                if (enabled) {
                    setOnClickListener {
                        onPressed()
                    }
                } else {
                    setOnClickListener(null)
                }
            }
        }
    )
}

@Composable
private fun GooglePrimaryButton(
    modifier: Modifier = Modifier,
    state: PrimaryButton.State
) {
    val processingState = if (state is PrimaryButton.State.FinishProcessing) {
        PrimaryButtonProcessingState.Completed
    } else {
        PrimaryButtonProcessingState.Processing
    }

    PrimaryButtonTheme(
        colors = PrimaryButtonColors(
            background = colorResource(
                id = R.color.stripe_paymentsheet_googlepay_primary_button_background_color
            ),
            onBackground = colorResource(
                id = R.color.stripe_paymentsheet_googlepay_primary_button_tint_color
            ),
            successBackground = colorResource(
                id = R.color.stripe_paymentsheet_googlepay_primary_button_background_color
            ),
            onSuccessBackground = colorResource(
                id = R.color.stripe_paymentsheet_googlepay_primary_button_tint_color
            ),
        )
    ) {
        Box(modifier = modifier.testTag(GOOGLE_PAY_PRIMARY_BUTTON_TEST_TAG)) {
            PrimaryButton(
                label = "",
                locked = true,
                enabled = true,
                processingState = processingState,
                onProcessingCompleted = {
                    if (state is PrimaryButton.State.FinishProcessing) {
                        state.onComplete()
                    }
                }
            ) {}
        }
    }
}

private const val FULL_ALPHA = 1f
private const val HALF_ALPHA = 0.5f

internal const val GOOGLE_PAY_BUTTON_TEST_TAG = "google-pay-button"
internal const val GOOGLE_PAY_PRIMARY_BUTTON_TEST_TAG = "google-pay-primary-button"
