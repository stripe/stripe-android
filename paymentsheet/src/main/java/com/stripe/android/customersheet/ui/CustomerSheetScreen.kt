package com.stripe.android.customersheet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.PrimaryButton
import com.stripe.android.customersheet.CustomerSheetViewAction
import com.stripe.android.customersheet.CustomerSheetViewState
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.PaymentOptions
import com.stripe.android.paymentsheet.ui.PaymentSheetScaffold
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.ui.core.elements.H4Text

@Composable
internal fun CustomerSheetScreen(
    viewState: CustomerSheetViewState,
    viewActionHandler: (CustomerSheetViewAction) -> Unit = {}
) {
    when (viewState) {
        is CustomerSheetViewState.Loading -> {
            Loading()
        }
        is CustomerSheetViewState.SelectPaymentMethod -> {
            SelectPaymentMethod(viewState, viewActionHandler)
        }
    }
}

@Composable
internal fun Loading() {
    val padding = dimensionResource(
        R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun SelectPaymentMethod(
    viewState: CustomerSheetViewState.SelectPaymentMethod,
    viewActionHandler: (CustomerSheetViewAction) -> Unit,
) {
    val bottomPadding = dimensionResource(R.dimen.stripe_paymentsheet_button_container_spacing_bottom)

    PaymentSheetScaffold(
        topBar = {
            PaymentSheetTopBar(
                screen = PaymentSheetScreen.SelectSavedPaymentMethods,
                showEditMenu = viewState.isEditing,
                isLiveMode = viewState.isLiveMode,
                isProcessing = viewState.isProcessing,
                isEditing = viewState.isEditing,
                handleBackPressed = {
                    viewActionHandler(CustomerSheetViewAction.OnBackPressed)
                },
                toggleEditing = {
                    viewActionHandler(CustomerSheetViewAction.OnEditPressed)
                },
            )
        },
        content = {
            SelectPaymentMethodContent(
                viewState = viewState,
                viewActionHandler = viewActionHandler,
            )
        },
        modifier = Modifier.padding(bottom = bottomPadding)
    )
}

@Composable
internal fun SelectPaymentMethodContent(
    viewState: CustomerSheetViewState.SelectPaymentMethod,
    viewActionHandler: (CustomerSheetViewAction) -> Unit,
) {
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column {
        H4Text(
            text = viewState.title ?: stringResource(
                R.string.stripe_paymentsheet_select_payment_method
            ),
            modifier = Modifier
                .padding(bottom = 2.dp)
                .padding(horizontal = horizontalPadding)
        )

        PaymentOptions(
            state = PaymentOptionsState(
                items = viewState.paymentMethods,
                selectedIndex = viewState.paymentMethods.indexOfFirst {
                    it.paymentMethod.id == viewState.selectedPaymentMethodId
                },
            ),
            isEditing = viewState.isEditing,
            isProcessing = viewState.isProcessing,
            onAddCardPressed = { viewActionHandler(CustomerSheetViewAction.OnAddCardPressed) },
            onItemSelected = { viewActionHandler(CustomerSheetViewAction.OnItemSelected(it)) },
            onItemRemoved = { viewActionHandler(CustomerSheetViewAction.OnItemRemoved(it)) },
            modifier = Modifier.padding(bottom = 8.dp),
        )

        AnimatedVisibility(visible = viewState.errorMessage != null) {
            viewState.errorMessage?.let { error ->
                ErrorMessage(
                    error = error,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .padding(horizontal = horizontalPadding),
                )
            }
        }

        AnimatedVisibility(visible = viewState.primaryButtonLabel != null) {
            viewState.primaryButtonLabel?.let {
                PrimaryButton(
                    label = it,
                    isEnabled = viewState.primaryButtonEnabled,
                    onButtonClick = {
                        viewActionHandler(CustomerSheetViewAction.OnPrimaryButtonPressed)
                    },
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
            }
        }
    }
}
