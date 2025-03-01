package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
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
import com.google.pay.button.ButtonTheme
import com.google.pay.button.ButtonType
import com.google.pay.button.PayButton
import com.stripe.android.CardBrandFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import org.json.JSONArray

@Composable
internal fun GooglePayButton(
    state: PrimaryButton.State?,
    allowCreditCards: Boolean,
    buttonType: GooglePayButtonType,
    billingAddressParameters: GooglePayJsonFactory.BillingAddressParameters?,
    isEnabled: Boolean,
    onPressed: () -> Unit,
    modifier: Modifier = Modifier,
    cardBrandFilter: CardBrandFilter
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
                GooglePayJsonFactory(context, cardBrandFilter = cardBrandFilter).createCardPaymentMethod(
                    billingAddressParameters = billingAddressParameters,
                    allowCreditCards = allowCreditCards
                )
            ).toString()
        }
    }

    val buttonTheme = if (isSystemInDarkTheme()) {
        ButtonTheme.Light
    } else {
        ButtonTheme.Dark
    }

    when (state) {
        null,
        is PrimaryButton.State.Ready -> PayButton(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onPressed)
                .testTag(GOOGLE_PAY_BUTTON_TEST_TAG),
            allowedPaymentMethods = allowedPaymentMethods,
            type = buttonType.toComposeButtonType(),
            theme = buttonTheme,
            radius = PrimaryButtonTheme.shape.cornerRadius,
            enabled = isEnabled,
            onClick = onPressed,
        )
        is PrimaryButton.State.StartProcessing,
        is PrimaryButton.State.FinishProcessing -> GooglePrimaryButton(
            modifier = modifier,
            state = state,
        )
    }
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

private fun GooglePayButtonType.toComposeButtonType(): ButtonType {
    return when (this) {
        GooglePayButtonType.Book -> ButtonType.Book
        GooglePayButtonType.Buy -> ButtonType.Buy
        GooglePayButtonType.Checkout -> ButtonType.Checkout
        GooglePayButtonType.Donate -> ButtonType.Donate
        GooglePayButtonType.Order -> ButtonType.Order
        GooglePayButtonType.Pay -> ButtonType.Pay
        GooglePayButtonType.Plain -> ButtonType.Plain
        GooglePayButtonType.Subscribe -> ButtonType.Subscribe
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val GOOGLE_PAY_BUTTON_TEST_TAG = "google-pay-button"

internal const val GOOGLE_PAY_PRIMARY_BUTTON_TEST_TAG = "google-pay-primary-button"
