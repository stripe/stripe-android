package com.stripe.android.customersheet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import com.stripe.android.paymentsheet.ui.EditPaymentMethod
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.Mandate
import com.stripe.android.paymentsheet.ui.PaymentElement
import com.stripe.android.paymentsheet.ui.PaymentOptions
import com.stripe.android.paymentsheet.ui.PaymentSheetScaffold
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.ui.core.elements.events.CardNumberCompletedEventReporter
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.R as PaymentsCoreR

@Composable
internal fun CustomerSheetScreen(
    viewState: CustomerSheetViewState,
    displayAddForm: Boolean = true,
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
                        AddPaymentMethod(
                            viewState = viewState,
                            viewActionHandler = viewActionHandler,
                            displayForm = displayAddForm,
                        )
                        PaymentSheetContentPadding()
                    }
                    is CustomerSheetViewState.EditPaymentMethod -> {
                        EditPaymentMethod(
                            viewState = viewState,
                        )
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
                isCbcEligible = viewState.cbcEligibility is CardBrandChoiceEligibility.Eligible,
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
                        .testTag(CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG)
                        .padding(top = 20.dp)
                        .padding(horizontal = horizontalPadding),
                )
            }
        }

        AnimatedVisibility(visible = viewState.mandateText != null) {
            Mandate(
                mandateText = viewState.mandateText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }
    }
}

@Composable
@Suppress("LongMethod")
internal fun AddPaymentMethod(
    viewState: CustomerSheetViewState.AddPaymentMethod,
    viewActionHandler: (CustomerSheetViewAction) -> Unit,
    displayForm: Boolean,
) {
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    if (viewState.displayDismissConfirmationModal) {
        SimpleDialogElementUI(
            titleText = stringResource(id = R.string.stripe_confirm_close_form_title),
            messageText = stringResource(id = R.string.stripe_confirm_close_form_body),
            confirmText = stringResource(id = R.string.stripe_paymentsheet_close),
            dismissText = stringResource(id = PaymentsCoreR.string.stripe_cancel),
            destructive = true,
            onDismissListener = {
                viewActionHandler(CustomerSheetViewAction.OnCancelClose)
            },
            onConfirmListener = {
                viewActionHandler(CustomerSheetViewAction.OnDismissed)
            }
        )
    }

    // TODO (jameswoo) make sure that the spacing is consistent with paymentsheet
    Column {
        H4Text(
            text = stringResource(id = R.string.stripe_paymentsheet_save_a_new_payment_method),
            modifier = Modifier
                .padding(bottom = 4.dp)
                .padding(horizontal = horizontalPadding)
        )

        val eventReporter = remember(viewActionHandler) {
            DefaultCardNumberCompletedEventReporter(viewActionHandler)
        }

        if (displayForm) {
            CompositionLocalProvider(
                LocalCardNumberCompletedEventReporter provides eventReporter
            ) {
                PaymentElement(
                    enabled = viewState.enabled,
                    supportedPaymentMethods = viewState.supportedPaymentMethods,
                    selectedItem = viewState.selectedPaymentMethod,
                    formElements = viewState.formElements,
                    linkSignupMode = null,
                    linkConfigurationCoordinator = null,
                    onItemSelectedListener = {
                        viewActionHandler(CustomerSheetViewAction.OnAddPaymentMethodItemChanged(it))
                    },
                    onLinkSignupStateChanged = { _, _ -> },
                    formArguments = viewState.formArguments,
                    usBankAccountFormArguments = viewState.usBankAccountFormArguments,
                    onFormFieldValuesChanged = {
                        // This only gets emitted if form field values are complete
                        viewActionHandler(CustomerSheetViewAction.OnFormFieldValuesCompleted(it))
                    }
                )
            }
        }

        AnimatedVisibility(visible = viewState.errorMessage != null) {
            viewState.errorMessage?.let { error ->
                ErrorMessage(
                    error = error,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )
            }
        }

        if (viewState.showMandateAbovePrimaryButton) {
            Mandate(
                mandateText = viewState.mandateText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }

        PrimaryButton(
            label = viewState.primaryButtonLabel.resolve(),
            isEnabled = viewState.primaryButtonEnabled,
            isLoading = viewState.isProcessing,
            displayLockIcon = true,
            onButtonClick = {
                viewActionHandler(CustomerSheetViewAction.OnPrimaryButtonPressed)
            },
            modifier = Modifier
                .testTag(CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG)
                .padding(top = 10.dp)
                .padding(horizontal = horizontalPadding),
        )

        if (!viewState.showMandateAbovePrimaryButton) {
            Mandate(
                mandateText = viewState.mandateText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = horizontalPadding),
            )
        }
    }
}

@Composable
private fun EditPaymentMethod(
    viewState: CustomerSheetViewState.EditPaymentMethod,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(modifier) {
        H4Text(
            text = stringResource(PaymentsCoreR.string.stripe_title_update_card),
            modifier = Modifier
                .padding(bottom = 20.dp)
                .padding(horizontal = horizontalPadding)
        )

        EditPaymentMethod(
            interactor = viewState.editPaymentMethodInteractor,
            modifier = modifier,
        )
    }
}

internal const val CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG = "CustomerSheetConfirmButton"
internal const val CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG = "CustomerSheetSaveButton"

private class DefaultCardNumberCompletedEventReporter(
    private val viewActionHandler: (CustomerSheetViewAction) -> Unit
) : CardNumberCompletedEventReporter {
    override fun onCardNumberCompleted() {
        viewActionHandler.invoke(CustomerSheetViewAction.OnCardNumberInputCompleted)
    }
}
