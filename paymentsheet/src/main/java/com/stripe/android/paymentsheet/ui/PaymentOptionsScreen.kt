package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.stripe.android.link.ui.verification.LinkVerificationDialog
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.StripeFragmentPaymentOptionsPrimaryButtonBinding
import com.stripe.android.paymentsheet.navigation.Content
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

@Composable
internal fun PaymentOptionsScreen(
    viewModel: PaymentOptionsViewModel,
    modifier: Modifier = Modifier,
) {
    PaymentSheetScaffold(
        topBar = { PaymentSheetTopBar(viewModel) },
        content = { scrollModifier ->
            PaymentOptionsScreenContent(viewModel, scrollModifier)
        },
        modifier = modifier,
    )
}

@Composable
internal fun PaymentOptionsScreenContent(
    viewModel: PaymentOptionsViewModel,
    modifier: Modifier = Modifier,
) {
    val headerText by viewModel.headerText.collectAsState(initial = null)
    val currentScreen by viewModel.currentScreen.collectAsState()

    val errorMessage by viewModel.error.collectAsState(initial = null)
    val notesText by viewModel.notesText.collectAsState()

    val showLinkDialog by viewModel.linkHandler.showLinkVerificationDialog.collectAsState()

    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)
    val bottomPadding = dimensionResource(R.dimen.stripe_paymentsheet_button_container_spacing_bottom)

    Column(
        modifier = modifier.padding(bottom = bottomPadding),
    ) {
        if (showLinkDialog) {
            LinkVerificationDialog(
                linkLauncher = viewModel.linkHandler.linkLauncher,
                onResult = viewModel.linkHandler::handleLinkVerificationResult,
            )
        }

        headerText?.let { text ->
            H4Text(
                text = stringResource(text),
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        currentScreen.Content(viewModel)

        errorMessage?.let { error ->
            ErrorMessage(
                error = error,
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        AndroidViewBinding(
            factory = StripeFragmentPaymentOptionsPrimaryButtonBinding::inflate,
            modifier = Modifier.testTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG),
        )

        notesText?.let { text ->
            Html(
                html = text,
                color = MaterialTheme.stripeColors.subtitle,
                style = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }
    }
}
