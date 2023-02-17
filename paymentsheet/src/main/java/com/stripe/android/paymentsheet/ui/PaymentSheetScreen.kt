package com.stripe.android.paymentsheet.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.compose.ui.zIndex
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
    val scrollState = rememberScrollState()

    val targetElevation by remember {
        derivedStateOf {
            if (scrollState.value > 0) {
                8.dp
            } else {
                0.dp
            }
        }
    }

    val elevation by animateDpAsState(targetValue = targetElevation)
    val contentVisible by viewModel.contentVisible.collectAsState()

    DismissKeyboardOnProcessing(viewModel)

    Column {
        // We need to set a z-index to make sure that the Surface's elevation shadow is rendered
        // correctly above the screen content.
        Surface(elevation = elevation, modifier = Modifier.zIndex(1f)) {
            PaymentSheetTopBar(viewModel)
        }

        if (contentVisible) {
            PaymentSheetScreenContent(
                viewModel = viewModel,
                modifier = modifier.verticalScroll(scrollState),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DismissKeyboardOnProcessing(viewModel: PaymentSheetViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val processing by viewModel.processing.collectAsState()
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
        )

        notes?.let { text ->
            Html(
                html = text,
                color = MaterialTheme.stripeColors.subtitle,
                style = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Center),
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

    Column(
        modifier = modifier.padding(horizontal = padding),
    ) {
        if (containerState.showGooglePay) {
            GooglePayButton(
                state = googlePayButtonState?.convert(),
                isEnabled = buttonsEnabled,
                onPressed = viewModel::checkoutWithGooglePay,
                modifier = Modifier.padding(top = 7.dp),
            )
        }

        if (containerState.showLink) {
            LinkButton(
                email = email,
                enabled = buttonsEnabled,
                onClick = viewModel::handleLinkPressed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .requiredHeight(48.dp),
            )
        }

        googlePayButtonState?.errorMessage?.let { error ->
            ErrorMessage(
                error = error.message,
                modifier = Modifier.padding(vertical = 3.dp, horizontal = 1.dp),
            )
        }

        if (containerState.shouldShow) {
            val text = stringResource(containerState.dividerTextResource)
            GooglePayDividerUi(text)
        }
    }
}
