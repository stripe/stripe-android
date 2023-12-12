package com.stripe.android.paymentsheet.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.databinding.StripeFragmentPaymentOptionsPrimaryButtonBinding
import com.stripe.android.paymentsheet.navigation.Content
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.ui.core.elements.H4Text

@Composable
internal fun PaymentOptionsScreen(
    viewModel: PaymentOptionsViewModel,
    modifier: Modifier = Modifier,
) {
    val topBarState by viewModel.topBarState.collectAsState()

    PaymentSheetScaffold(
        topBar = {
            PaymentSheetTopBar(
                state = topBarState,
                handleBackPressed = viewModel::handleBackPressed,
                toggleEditing = viewModel::toggleEditing,
            )
        },
        content = {
            PaymentOptionsScreenContent(viewModel)
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
    val mandateText by viewModel.mandateText.collectAsState()

    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(modifier) {
        headerText?.let { text ->
            H4Text(
                text = stringResource(text),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        Box(modifier = Modifier.animateContentSize()) {
            currentScreen.Content(viewModel)
        }

        if (mandateText?.showAbovePrimaryButton == true) {
            Mandate(
                mandateText = mandateText?.text,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
        }

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

        if (mandateText?.showAbovePrimaryButton == false) {
            Mandate(
                mandateText = mandateText?.text,
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
        }

        PaymentSheetContentPadding()
    }
}
