package com.stripe.android.customersheet.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.PrimaryButton
import com.stripe.android.customersheet.CustomerSheetViewAction
import com.stripe.android.customersheet.CustomerSheetViewState
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.PaymentOptions
import com.stripe.android.paymentsheet.ui.PaymentSheetScaffold
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.H4Text

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun CustomerSheetScreen(
    viewState: CustomerSheetViewState,
    modifier: Modifier = Modifier,
    viewActionHandler: (CustomerSheetViewAction) -> Unit = {},
    paymentMethodNameProvider: (PaymentMethodCode?) -> String,
) {
    val bottomPadding = dimensionResource(R.dimen.stripe_paymentsheet_button_container_spacing_bottom)

    PaymentSheetScaffold(
        topBar = {
            PaymentSheetTopBar(
                screen = viewState.screen,
                showEditMenu = viewState.showEditMenu,
                isLiveMode = viewState.isLiveMode,
                isProcessing = viewState.isProcessing,
                isEditing = viewState.isEditing,
                handleBackPressed = {
                    viewActionHandler(
                        CustomerSheetViewAction.OnBackPressed
                    )
                },
                toggleEditing = {
                    viewActionHandler(CustomerSheetViewAction.OnEditPressed)
                },
            )
        },
        content = {
            AnimatedContent(
                targetState = viewState
            ) { targetState ->
                when (targetState) {
                    is CustomerSheetViewState.Loading -> {
                        Loading()
                    }
                    is CustomerSheetViewState.SelectPaymentMethod -> {
                        SelectPaymentMethod(
                            viewState = targetState,
                            viewActionHandler = viewActionHandler,
                            paymentMethodNameProvider = paymentMethodNameProvider,
                        )
                    }
                    is CustomerSheetViewState.AddPaymentMethod -> {
                        AddCard(
                            viewState = targetState,
                            viewActionHandler = viewActionHandler,
                        )
                    }
                }
            }
        },
        modifier = modifier.padding(bottom = bottomPadding)
    )
}

@Composable
internal fun Loading(
    modifier: Modifier = Modifier,
) {
    val padding = dimensionResource(
        R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )
    Box(
        modifier = modifier
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
    paymentMethodNameProvider: (PaymentMethodCode?) -> String,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(
        modifier = modifier
    ) {
        H4Text(
            text = viewState.title ?: stringResource(
                R.string.stripe_paymentsheet_select_payment_method
            ),
            modifier = Modifier
                .padding(bottom = 20.dp)
                .padding(horizontal = horizontalPadding)
        )

        PaymentOptions(
            state = PaymentOptionsStateFactory.create(
                paymentMethods = viewState.savedPaymentMethods,
                showGooglePay = viewState.isGooglePayEnabled,
                showLink = false,
                currentSelection = viewState.paymentSelection,
                nameProvider = paymentMethodNameProvider,
            ),
            isEditing = viewState.isEditing,
            isProcessing = viewState.isProcessing,
            onAddCardPressed = { viewActionHandler(CustomerSheetViewAction.OnAddCardPressed) },
            onItemSelected = { viewActionHandler(CustomerSheetViewAction.OnItemSelected(it)) },
            onItemRemoved = { viewActionHandler(CustomerSheetViewAction.OnItemRemoved(it)) },
            modifier = Modifier.padding(bottom = 2.dp),
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
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .padding(horizontal = horizontalPadding),
                )
            }
        }
    }
}

@Composable
internal fun AddCard(
    viewState: CustomerSheetViewState.AddPaymentMethod,
    viewActionHandler: (CustomerSheetViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    val viewData by viewState.formViewDataFlow.collectAsState(initial = FormViewModel.ViewData())

    LaunchedEffect(viewData.completeFormValues) {
        viewActionHandler(
            CustomerSheetViewAction.OnFormValuesChanged(
                viewData.completeFormValues
            )
        )
    }

    Column(
        modifier = modifier.padding(horizontal = horizontalPadding)
    ) {
        H4Text(
            text = stringResource(
                R.string.stripe_paymentsheet_add_payment_method_title
            ),
            modifier = Modifier
                .padding(bottom = 20.dp)
        )

        FormUI(
            hiddenIdentifiers = viewData.hiddenIdentifiers,
            enabled = viewState.enabled,
            elements = viewData.elements,
            lastTextFieldIdentifier = viewData.lastTextFieldIdentifier,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        PrimaryButton(
            // TODO (jameswoo) add to lokalize
            label = "Add",
            isEnabled = true,
            onButtonClick = {
                viewActionHandler(CustomerSheetViewAction.OnPrimaryButtonPressed)
            },
        )
    }
}
