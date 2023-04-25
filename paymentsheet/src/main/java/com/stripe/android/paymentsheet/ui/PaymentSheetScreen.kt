@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
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
import com.stripe.android.link.ui.verification.LinkVerificationDialog
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.FragmentPaymentSheetPrimaryButtonBinding
import com.stripe.android.paymentsheet.state.WalletsContainerState
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

    DismissKeyboardOnProcessing(processing)

    PaymentSheetScaffold(
        topBar = { PaymentSheetTopBar(viewModel) },
        content = { scrollModifier ->
            if (contentVisible) {
                PaymentSheetScreenContent(
                    viewModel = viewModel,
                    modifier = scrollModifier,
                )
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
    val showLinkDialog by viewModel.linkHandler.showLinkVerificationDialog.collectAsState()

    val headerText by viewModel.headerText.collectAsState(null)
    val buyButtonState by viewModel.buyButtonState.collectAsState(initial = null)

    val currentScreen by viewModel.currentScreen.collectAsState()
    val notes by viewModel.notesText.collectAsState()

    val bottomPadding = dimensionResource(
        R.dimen.stripe_paymentsheet_button_container_spacing_bottom
    )

    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    if (showLinkDialog) {
        LinkVerificationDialog(
            linkLauncher = viewModel.linkHandler.linkLauncher,
            onResult = viewModel.linkHandler::handleLinkVerificationResult,
        )
    }

    Column(
        modifier = modifier.padding(bottom = bottomPadding),
    ) {
        headerText?.let { text ->
            H4Text(
                text = stringResource(text),
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        Wallet(viewModel)

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
            factory = FragmentPaymentSheetPrimaryButtonBinding::inflate,
            modifier = Modifier.testTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG),
        )

        notes?.let { text ->
            Html(
                html = text,
                color = MaterialTheme.stripeColors.subtitle,
                style = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Center),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
internal fun Wallet(
    viewModel: PaymentSheetViewModel,
    modifier: Modifier = Modifier,
) {
    val containerState by viewModel.walletsContainerState.collectAsState(
        initial = WalletsContainerState(),
    )

    val email by viewModel.linkHandler.linkLauncher.emailFlow.collectAsState(initial = null)
    val googlePayButtonState by viewModel.googlePayButtonState.collectAsState(initial = null)
    val buttonsEnabled by viewModel.buttonsEnabled.collectAsState(initial = false)

    val padding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    if (containerState.shouldShow) {
        Column(modifier = modifier.padding(horizontal = padding)) {
            if (containerState.showGooglePay) {
                GooglePayButton(
                    state = googlePayButtonState?.convert(),
                    allowCreditCards = containerState.googlePayAllowCreditCards,
                    billingAddressParameters = containerState.googlePayBillingAddressParameters,
                    isEnabled = buttonsEnabled,
                    onPressed = viewModel::checkoutWithGooglePay,
                )
            }

            if (containerState.showLink) {
                LinkButton(
                    email = email,
                    enabled = buttonsEnabled,
                    onClick = viewModel::handleLinkPressed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(48.dp),
                )
            }

            googlePayButtonState?.errorMessage?.let { error ->
                ErrorMessage(
                    error = error.message,
                    modifier = Modifier.padding(vertical = 3.dp, horizontal = 1.dp),
                )
            }

            val text = stringResource(containerState.dividerTextResource)
            GooglePayDividerUi(text)
        }
    }
}

const val PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG = "PRIMARY_BUTTON"
