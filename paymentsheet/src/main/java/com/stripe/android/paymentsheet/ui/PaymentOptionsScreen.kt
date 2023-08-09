package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.Content
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBackgroundColor
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html
import com.stripe.android.utils.rememberActivity

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
    val activity = rememberActivity {
        "PaymentOptionsScreenContent must be called in the context of an Activity"
    }

    val headerText by viewModel.headerText.collectAsState(initial = null)
    val currentScreen by viewModel.currentScreen.collectAsState()

    val errorMessage by viewModel.error.collectAsState(initial = null)
    val notesText by viewModel.notesText.collectAsState()

    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    val continueButtonPadding = PaddingValues(
        top = dimensionResource(R.dimen.stripe_paymentsheet_button_container_spacing),
        start = horizontalPadding,
        end = horizontalPadding,
    )

    val uiState by viewModel.primaryButtonUiState.collectAsState()

    Column(modifier) {
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

        uiState?.let { uiState ->
            PrimaryButton(
                uiState = uiState,
                state = PrimaryButton.State.Ready,
                style = StripeTheme.primaryButtonStyle,
                background = Color(
                    viewModel.config?.primaryButtonColor?.defaultColor
                        ?: StripeTheme.primaryButtonStyle.getBackgroundColor(activity.baseContext)
                ),
                modifier = Modifier
                    .padding(continueButtonPadding)
                    .testTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG),
            )
        }

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

        PaymentSheetContentPadding()
    }
}
