package com.stripe.android.customersheet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.common.ui.PrimaryButton
import com.stripe.android.customersheet.CustomerSheetViewAction
import com.stripe.android.customersheet.CustomerSheetViewState
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.PaymentElement
import com.stripe.android.paymentsheet.ui.PaymentOptions
import com.stripe.android.paymentsheet.ui.PaymentSheetScaffold
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.utils.FeatureFlags.customerSheetACHv2
import kotlinx.coroutines.flow.flowOf
import javax.inject.Provider

@Composable
internal fun CustomerSheetScreen(
    viewState: CustomerSheetViewState,
    formViewModelSubComponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>?,
    modifier: Modifier = Modifier,
    viewActionHandler: (CustomerSheetViewAction) -> Unit = {},
    paymentMethodNameProvider: (PaymentMethodCode?) -> String,
) {
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
            Column(modifier = Modifier.animateContentSize()) {
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
                        PaymentSheetContentPadding()
                    }
                    is CustomerSheetViewState.AddPaymentMethod -> {
                        if (customerSheetACHv2.isEnabled) {
                            AddPaymentMethodWithPaymentElement(
                                viewState = viewState,
                                viewActionHandler = viewActionHandler,
                                formViewModelSubComponentBuilderProvider = formViewModelSubComponentBuilderProvider,
                            )
                        } else {
                            AddPaymentMethod(
                                viewState = viewState,
                                viewActionHandler = viewActionHandler,
                            )
                        }
                        PaymentSheetContentPadding()
                    }
                }
            }
        },
        modifier = modifier,
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
                // TODO(samer-stripe): Add CBC eligibility check to CustomerSheet
                isCbcEligible = false
            ),
            isEditing = viewState.isEditing,
            isProcessing = viewState.isProcessing,
            onAddCardPressed = { viewActionHandler(CustomerSheetViewAction.OnAddCardPressed) },
            onItemSelected = { viewActionHandler(CustomerSheetViewAction.OnItemSelected(it)) },
            onModifyItem = { viewActionHandler(CustomerSheetViewAction.OnModifyItem(it)) },
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
internal fun AddPaymentMethod(
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
                .padding(bottom = 20.dp)
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
            isEnabled = viewState.primaryButtonEnabled,
            isLoading = viewState.isProcessing,
            onButtonClick = {
                viewActionHandler(CustomerSheetViewAction.OnPrimaryButtonPressed)
            },
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
internal fun AddPaymentMethodWithPaymentElement(
    viewState: CustomerSheetViewState.AddPaymentMethod,
    viewActionHandler: (CustomerSheetViewAction) -> Unit,
    formViewModelSubComponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>?,
) {
    requireNotNull(formViewModelSubComponentBuilderProvider)
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    // TODO (jameswoo) make sure that the spacing is consistent with paymentsheet
    Column {
        H4Text(
            text = stringResource(id = R.string.stripe_paymentsheet_save_a_new_payment_method),
            modifier = Modifier
                .padding(bottom = 20.dp)
                .padding(horizontal = horizontalPadding)
        )

        PaymentElement(
            formViewModelSubComponentBuilderProvider = formViewModelSubComponentBuilderProvider,
            enabled = viewState.enabled,
            supportedPaymentMethods = viewState.supportedPaymentMethods,
            selectedItem = viewState.selectedPaymentMethod,
            showLinkInlineSignup = false,
            linkConfigurationCoordinator = null,
            showCheckboxFlow = flowOf(false),
            onItemSelectedListener = { },
            onLinkSignupStateChanged = { _, _ -> },
            formArguments = viewState.formArguments,
            usBankAccountFormArguments = viewState.usBankAccountFormArguments,
            onFormFieldValuesChanged = { }
        )

        AnimatedVisibility(visible = viewState.errorMessage != null) {
            viewState.errorMessage?.let { error ->
                ErrorMessage(
                    error = error,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )
            }
        }

        PrimaryButton(
            label = stringResource(id = R.string.stripe_paymentsheet_save),
            isEnabled = viewState.primaryButtonEnabled,
            isLoading = viewState.isProcessing,
            onButtonClick = {
                viewActionHandler(CustomerSheetViewAction.OnPrimaryButtonPressed)
            },
            modifier = Modifier
                .padding(top = 10.dp)
                .padding(horizontal = horizontalPadding),
        )
    }
}
