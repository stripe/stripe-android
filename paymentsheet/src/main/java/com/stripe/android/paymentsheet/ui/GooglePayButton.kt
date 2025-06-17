package com.stripe.android.paymentsheet.ui

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.children
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions
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
                .semantics {
                    onClick {
                        onPressed()

                        true
                    }
                }
                .testTag(GOOGLE_PAY_BUTTON_TEST_TAG),
            allowedPaymentMethods = allowedPaymentMethods,
            type = buttonType.toComposeButtonType(),
            theme = buttonTheme,
            radius = PrimaryButtonTheme.shape.cornerRadius,
            height = PrimaryButtonTheme.shape.height,
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

private enum class ButtonTheme(val value: Int) {
    Dark(ButtonConstants.ButtonTheme.DARK),
    Light(ButtonConstants.ButtonTheme.LIGHT),
}

private enum class ButtonType(val value: Int) {
    Book(ButtonConstants.ButtonType.BOOK),
    Buy(ButtonConstants.ButtonType.BUY),
    Checkout(ButtonConstants.ButtonType.CHECKOUT),
    Donate(ButtonConstants.ButtonType.DONATE),
    Order(ButtonConstants.ButtonType.ORDER),
    Pay(ButtonConstants.ButtonType.PAY),
    Plain(ButtonConstants.ButtonType.PLAIN),
    Subscribe(ButtonConstants.ButtonType.SUBSCRIBE),
}

private const val FULL_ALPHA = 1f
private const val HALF_ALPHA = 0.5f

@Composable
private fun PayButton(
    onClick: () -> Unit,
    allowedPaymentMethods: String,
    modifier: Modifier = Modifier,
    theme: ButtonTheme = ButtonTheme.Dark,
    type: ButtonType = ButtonType.Buy,
    height: Dp? = null,
    radius: Dp = 100.dp,
    enabled: Boolean = true,
) {
    val radiusPixelValue = with(LocalDensity.current) { radius.toPx().toInt() }
    val heightPixelValue = with(LocalDensity.current) { height?.toPx()?.toInt() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            com.google.android.gms.wallet.button.PayButton(context).apply {
                this.initialize(
                    ButtonOptions.newBuilder()
                        .setButtonTheme(theme.value)
                        .setButtonType(type.value)
                        .setCornerRadius(radiusPixelValue)
                        .setAllowedPaymentMethods(allowedPaymentMethods)
                        .build()
                )
            }
        },
        update = { button ->
            button.apply {
                /*
                 * The Google Pay Button does not allow for direct control of its height so manually pull the layout
                 * to manage the height
                 */
                nestedView(depth = 2)?.run {
                    minimumHeight = heightPixelValue ?: minimumHeight
                }

                alpha = if (enabled) FULL_ALPHA else HALF_ALPHA
                isEnabled = enabled

                if (enabled) {
                    setOnClickListener { onClick() }
                } else {
                    setOnClickListener(null)
                }
            }
        }
    )
}

private fun ViewGroup.nestedView(depth: Int): View? {
    val view = children.firstOrNull()

    return if (depth == 0) {
        view
    } else {
        val viewGroup = view as? ViewGroup

        viewGroup?.nestedView(depth = depth - 1)
    }
}
