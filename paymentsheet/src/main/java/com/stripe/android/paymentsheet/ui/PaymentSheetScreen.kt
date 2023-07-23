@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.stripe.android.link.ui.LinkButton
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.StripeFragmentPaymentSheetPrimaryButtonBinding
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

@Composable
internal fun PaymentSheetScreen(
    viewModel: PaymentSheetViewModel,
    modifier: Modifier = Modifier,
) {
    val contentVisible by viewModel.contentVisible.collectAsState()
    val processing by viewModel.processing.collectAsState()

    val topBarState by viewModel.topBarState.collectAsState()

    DismissKeyboardOnProcessing(processing)

    PaymentSheetScaffold(
        topBar = {
            PaymentSheetTopBar(
                state = topBarState,
                handleBackPressed = viewModel::handleBackPressed,
                toggleEditing = viewModel::toggleEditing,
            )
        },
        content = { scrollModifier ->
            if (contentVisible) {
                PaymentSheetScreenContent(
                    viewModel = viewModel,
                    modifier = scrollModifier,
                )
            } else {
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DismissKeyboardOnProcessing(processing: Boolean) {
    val keyboardController = LocalSoftwareKeyboardController.current

    if (processing) {
        LaunchedEffect(Unit) {
            keyboardController?.hide()
        }
    }
}

@Composable
internal fun PaymentSheetScreenContent(
    viewModel: PaymentSheetViewModel,
    modifier: Modifier = Modifier,
) {
    val headerText by viewModel.headerText.collectAsState(null)
    val walletsState by viewModel.walletsState.collectAsState()
    val buyButtonState by viewModel.buyButtonState.collectAsState(initial = null)
    val currentScreen by viewModel.currentScreen.collectAsState()
    val notes by viewModel.notesText.collectAsState()

    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(modifier) {
        headerText?.let { text ->
            H4Text(
                text = stringResource(text),
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        walletsState?.let { state ->
            Wallet(
                state = state,
                onGooglePayPressed = viewModel::checkoutWithGooglePay,
                onLinkPressed = viewModel::handleLinkPressed,
            )
        }

        currentScreen.Content(
            viewModel = viewModel,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        buyButtonState?.errorMessage?.let { error ->
            ErrorMessage(
                error = error.message,
                modifier = Modifier.padding(vertical = 2.dp, horizontal = 20.dp),
            )
        }

        AndroidViewBinding(
            factory = StripeFragmentPaymentSheetPrimaryButtonBinding::inflate,
            modifier = Modifier.testTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG),
        )

        notes?.let { text ->
            Html(
                html = text,
                color = MaterialTheme.stripeColors.subtitle,
                style = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        PaymentSheetContentPadding()
    }
}

@Composable
internal fun Wallet(
    state: WalletsState,
    onGooglePayPressed: () -> Unit,
    onLinkPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val padding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(modifier = modifier.padding(horizontal = padding)) {
        state.googlePay?.let { googlePay ->
            GooglePayButton(
                state = googlePay.buttonState?.convert(),
                allowCreditCards = googlePay.allowCreditCards,
                billingAddressParameters = googlePay.billingAddressParameters,
                isEnabled = state.buttonsEnabled,
                onPressed = onGooglePayPressed,
            )
        }

        state.link?.let {
            if (state.googlePay != null) {
                Spacer(modifier = Modifier.requiredHeight(8.dp))
            }

            LinkButton(
                email = null,
                enabled = state.buttonsEnabled,
                onClick = onLinkPressed,
            )
        }

        state.googlePay?.buttonState?.errorMessage?.let { error ->
            ErrorMessage(
                error = error.message,
                modifier = Modifier.padding(vertical = 3.dp, horizontal = 1.dp),
            )
        }

        val text = stringResource(state.dividerTextResource)
        GooglePayDividerUi(text)
    }
}

const val PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG = "PRIMARY_BUTTON"
