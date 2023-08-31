package com.stripe.android.customersheet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.common.ui.PrimaryButton
import com.stripe.android.customersheet.CustomerSheetACHV2Flag
import com.stripe.android.customersheet.CustomerSheetViewAction
import com.stripe.android.customersheet.CustomerSheetViewState
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.PaymentElement
import com.stripe.android.paymentsheet.ui.PaymentOptions
import com.stripe.android.paymentsheet.ui.PaymentSheetScaffold
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html

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
                state = viewState.topBarState,
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
            Box(modifier = Modifier.animateContentSize()) {
                when (viewState) {
                    is CustomerSheetViewState.Loading -> {
                        BottomSheetLoadingIndicator()
                    }
                    is CustomerSheetViewState.SelectPaymentMethod -> {
                        SelectPaymentMethod(
                            viewState = viewState,
                            viewActionHandler = viewActionHandler,
                            paymentMethodNameProvider = paymentMethodNameProvider,
                        )
                    }
                    is CustomerSheetViewState.AddPaymentMethod -> {
                        if (CustomerSheetACHV2Flag) {
                            AddCardWithPaymentElement(
                                viewState = viewState,
                                viewActionHandler = viewActionHandler,
                            )
                        } else {
                            AddCard(
                                viewState = viewState,
                                viewActionHandler = viewActionHandler,
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.padding(bottom = bottomPadding)
    )
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
                id = R.string.stripe_paymentsheet_manage_your_payment_methods
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

        AnimatedVisibility(visible = viewState.primaryButtonVisible) {
            viewState.primaryButtonLabel?.let {
                PrimaryButton(
                    label = it,
                    isEnabled = viewState.primaryButtonEnabled,
                    isLoading = viewState.isProcessing,
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

    Column(
        modifier = modifier.padding(horizontal = horizontalPadding),
    ) {
        H4Text(
            text = stringResource(id = R.string.stripe_paymentsheet_save_a_new_payment_method),
            modifier = Modifier
                .padding(bottom = 8.dp)
        )

        FormUI(
            hiddenIdentifiers = viewState.formViewData.hiddenIdentifiers,
            enabled = viewState.enabled,
            elements = viewState.formViewData.elements,
            lastTextFieldIdentifier = viewState.formViewData.lastTextFieldIdentifier,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        AnimatedVisibility(visible = viewState.errorMessage != null) {
            viewState.errorMessage?.let { error ->
                ErrorMessage(error = error)
            }
        }

        PrimaryButton(
            label = stringResource(id = R.string.stripe_paymentsheet_save),
            isEnabled = viewState.primaryButtonUiState.enabled,
            isLoading = viewState.isProcessing,
            onButtonClick = {
                viewActionHandler(CustomerSheetViewAction.OnPrimaryButtonPressed)
            },
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
internal fun AddCardWithPaymentElement(
    viewState: CustomerSheetViewState.AddPaymentMethod,
    viewActionHandler: (CustomerSheetViewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(
        modifier = modifier,
    ) {
        H4Text(
            text = stringResource(id = R.string.stripe_paymentsheet_save_a_new_payment_method),
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
        )

        PaymentElement(
            primaryButtonState = viewState.primaryButtonState,
            onBehalfOf = null,
            stripeIntent = viewState.stripeIntent,
            isCompleteFlow = false,
            savedPaymentSelection = null,
            shippingDetails = null,
            enabled = viewState.enabled,
            supportedPaymentMethods = viewState.supportedPaymentMethods,
            selectedItem = viewState.selectedPaymentMethod,
            showLinkInlineSignup = false,
            linkConfigurationCoordinator = null,
            onItemSelectedListener = {
                viewActionHandler(CustomerSheetViewAction.OnPaymentOptionFormItemSelected(it))
            },
            onLinkSignupStateChanged = { _, _ -> },
            confirmUSBankAccount = {
                viewActionHandler(
                    CustomerSheetViewAction.OnConfirmUSBankAccount(it)
                )
            },
            updateCustomPrimaryButtonUiState = { block ->
                viewActionHandler(
                    CustomerSheetViewAction.OnUpdatePrimaryButtonUiState(
                        block(viewState.primaryButtonUiState)
                    )
                )
            },
            updatePrimaryButton = { text, enabled, shouldShowProcessing, onClick ->
                viewActionHandler(
                    CustomerSheetViewAction.OnUpdatePrimaryButton(
                        text = text,
                        enabled = enabled,
                        shouldShowProcessing = shouldShowProcessing,
                        onClick = onClick,
                    )
                )
            },
            updateMandateText = {
                viewActionHandler(CustomerSheetViewAction.OnUpdateMandateText(it))
            },
            onError = {
                viewActionHandler(CustomerSheetViewAction.OnAddPaymentMethodError(it))
            },
            formArguments = viewState.formArguments,
            formViewData = viewState.formViewData,
        )

        AnimatedVisibility(visible = viewState.errorMessage != null) {
            viewState.errorMessage?.let { error ->
                ErrorMessage(
                    error = error,
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
            }
        }

        PrimaryButton(
            label = viewState.primaryButtonUiState.label,
            isEnabled = viewState.primaryButtonUiState.enabled,
            isLoading = viewState.isProcessing,
            onButtonClick = {
                viewActionHandler(CustomerSheetViewAction.OnPrimaryButtonPressed)
            },
            modifier = Modifier
                .padding(top = 10.dp)
                .padding(horizontal = horizontalPadding),
        )

        AnimatedVisibility(visible = viewState.mandateText != null) {
            viewState.mandateText?.let { mandate ->
                Html(
                    html = mandate,
                    color = MaterialTheme.stripeColors.subtitle,
                    style = MaterialTheme.typography.body1.copy(textAlign = TextAlign.Center),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .padding(horizontal = horizontalPadding),
                )
            }
        }
    }
}
