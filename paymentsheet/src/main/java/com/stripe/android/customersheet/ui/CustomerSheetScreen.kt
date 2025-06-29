package com.stripe.android.customersheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.common.ui.BottomSheetScaffold
import com.stripe.android.common.ui.PrimaryButton
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.customersheet.CustomerSheetViewAction
import com.stripe.android.customersheet.CustomerSheetViewModel
import com.stripe.android.customersheet.CustomerSheetViewState
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.PaymentElement
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodTabLayoutUI
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodUI
import com.stripe.android.paymentsheet.utils.PaymentSheetContentPadding
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.ui.core.elements.Mandate
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.ui.core.elements.events.AnalyticsEventReporter
import com.stripe.android.ui.core.elements.events.CardBrandDisallowedReporter
import com.stripe.android.ui.core.elements.events.CardNumberCompletedEventReporter
import com.stripe.android.ui.core.elements.events.LocalAnalyticsEventReporter
import com.stripe.android.ui.core.elements.events.LocalCardBrandDisallowedReporter
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.R as PaymentsCoreR

@Composable
internal fun CustomerSheetScreen(
    viewModel: CustomerSheetViewModel,
) {
    val viewState by viewModel.viewState.collectAsState()

    CustomerSheetScreen(
        viewState = viewState,
        viewActionHandler = viewModel::handleViewAction,
        paymentMethodNameProvider = viewModel::providePaymentMethodName,
    )
}

@Composable
internal fun CustomerSheetScreen(
    viewState: CustomerSheetViewState,
    displayAddForm: Boolean = true,
    modifier: Modifier = Modifier,
    viewActionHandler: (CustomerSheetViewAction) -> Unit = {},
    paymentMethodNameProvider: (PaymentMethodCode?) -> ResolvableString,
) {
    BottomSheetScaffold(
        topBar = {
            PaymentSheetTopBar(
                state = viewState.topBarState {
                    viewActionHandler(CustomerSheetViewAction.OnEditPressed)
                },
                canNavigateBack = viewState.canNavigateBack,
                isEnabled = !viewState.isProcessing,
                handleBackPressed = {
                    viewActionHandler(
                        CustomerSheetViewAction.OnBackPressed
                    )
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
                    is CustomerSheetViewState.UpdatePaymentMethod -> {
                        UpdatePaymentMethod(
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
    paymentMethodNameProvider: (PaymentMethodCode?) -> ResolvableString,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = StripeTheme.getOuterFormInsets()

    Column(
        modifier = modifier
    ) {
        H4Text(
            text = viewState.title ?: stringResource(
                id = R.string.stripe_paymentsheet_manage_your_payment_methods
            ),
            modifier = Modifier
                .padding(bottom = 20.dp)
                .padding(horizontalPadding)
        )

        val paymentOptionsState = PaymentOptionsStateFactory.create(
            paymentMethods = viewState.savedPaymentMethods,
            showGooglePay = viewState.showGooglePay,
            showLink = false,
            currentSelection = viewState.paymentSelection,
            nameProvider = paymentMethodNameProvider,
            isCbcEligible = viewState.isCbcEligible,
            defaultPaymentMethodId = null
        )

        SavedPaymentMethodTabLayoutUI(
            paymentOptionsItems = paymentOptionsState.items,
            selectedPaymentOptionsItem = paymentOptionsState.selectedItem,
            isEditing = viewState.isEditing,
            isProcessing = viewState.isProcessing,
            onAddCardPressed = { viewActionHandler(CustomerSheetViewAction.OnAddCardPressed) },
            onItemSelected = { viewActionHandler(CustomerSheetViewAction.OnItemSelected(it)) },
            onModifyItem = { viewActionHandler(CustomerSheetViewAction.OnModifyItem(it)) },
            modifier = Modifier.padding(bottom = 2.dp),
        )

        viewState.errorMessage?.let { error ->
            ErrorMessage(
                error = error,
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .padding(horizontalPadding),
            )
        }

        if (viewState.primaryButtonVisible) {
            PrimaryButton(
                label = viewState.primaryButtonLabel.resolve(),
                isEnabled = viewState.primaryButtonEnabled,
                isLoading = viewState.isProcessing,
                onButtonClick = {
                    viewActionHandler(CustomerSheetViewAction.OnPrimaryButtonPressed)
                },
                modifier = Modifier
                    .testTag(CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG)
                    .padding(top = 20.dp)
                    .padding(horizontalPadding),
            )
        }

        Mandate(
            mandateText = viewState.mandateText?.resolve(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontalPadding),
        )
    }
}

@Composable
@Suppress("LongMethod")
internal fun AddPaymentMethod(
    viewState: CustomerSheetViewState.AddPaymentMethod,
    viewActionHandler: (CustomerSheetViewAction) -> Unit,
    displayForm: Boolean,
) {
    val horizontalPadding = StripeTheme.getOuterFormInsets()

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
    H4Text(
        text = stringResource(id = R.string.stripe_paymentsheet_save_a_new_payment_method),
        modifier = Modifier
            .padding(bottom = 4.dp)
            .padding(horizontalPadding)
    )

    val eventReporter = remember(viewActionHandler) {
        DefaultCardNumberCompletedEventReporter(viewActionHandler)
    }

    val disallowedReporter = remember(viewActionHandler) {
        DefaultCardBrandDisallowedReporter(viewActionHandler)
    }

    val analyticsEventReporter = remember(viewActionHandler) {
        DefaultAnalyticsEventReporter(viewActionHandler)
    }

    if (displayForm) {
        CompositionLocalProvider(
            LocalCardNumberCompletedEventReporter provides eventReporter,
            LocalCardBrandDisallowedReporter provides disallowedReporter,
            LocalAnalyticsEventReporter provides analyticsEventReporter,
        ) {
            PaymentElement(
                enabled = viewState.enabled,
                supportedPaymentMethods = viewState.supportedPaymentMethods,
                selectedItemCode = viewState.paymentMethodCode,
                formElements = viewState.formElements,
                onItemSelectedListener = {
                    viewActionHandler(CustomerSheetViewAction.OnAddPaymentMethodItemChanged(it))
                },
                formArguments = viewState.formArguments,
                usBankAccountFormArguments = viewState.usBankAccountFormArguments,
                incentive = null,
                onFormFieldValuesChanged = {
                    // This only gets emitted if form field values are complete
                    viewActionHandler(CustomerSheetViewAction.OnFormFieldValuesCompleted(it))
                }
            )
        }
    }

    Spacer(modifier = Modifier.padding(top = 24.dp))

    viewState.errorMessage?.let { error ->
        ErrorMessage(
            error = error.resolve(),
            modifier = Modifier.padding(horizontalPadding)
        )
    }

    if (viewState.showMandateAbovePrimaryButton) {
        Mandate(
            mandateText = viewState.mandateText?.resolve(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = viewState.errorMessage?.let {
                        8.dp
                    } ?: 0.dp
                )
                .padding(horizontalPadding),
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
            .padding(horizontalPadding),
    )

    if (!viewState.showMandateAbovePrimaryButton) {
        Mandate(
            mandateText = viewState.mandateText?.resolve(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontalPadding)
        )
    }
}

@Composable
private fun UpdatePaymentMethod(
    viewState: CustomerSheetViewState.UpdatePaymentMethod,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = StripeTheme.getOuterFormInsets()

    Column(modifier) {
        viewState.updatePaymentMethodInteractor.screenTitle?.let {
            H4Text(
                text = it.resolve(),
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .padding(horizontalPadding)
            )
        }

        UpdatePaymentMethodUI(
            interactor = viewState.updatePaymentMethodInteractor,
            modifier = modifier,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG = "CustomerSheetConfirmButton"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG = "CustomerSheetSaveButton"

private class DefaultAnalyticsEventReporter(
    private val viewActionHandler: (event: CustomerSheetViewAction) -> Unit
) : AnalyticsEventReporter {
    override fun onAnalyticsEvent(event: AnalyticsEvent) {
        viewActionHandler.invoke(CustomerSheetViewAction.OnAnalyticsEvent(event))
    }
}

private class DefaultCardNumberCompletedEventReporter(
    private val viewActionHandler: (CustomerSheetViewAction) -> Unit
) : CardNumberCompletedEventReporter {
    override fun onCardNumberCompleted() {
        viewActionHandler.invoke(CustomerSheetViewAction.OnCardNumberInputCompleted)
    }
}

private class DefaultCardBrandDisallowedReporter(
    private val viewActionHandler: (CustomerSheetViewAction) -> Unit
) : CardBrandDisallowedReporter {
    override fun onDisallowedCardBrandEntered(brand: CardBrand) {
        viewActionHandler.invoke(CustomerSheetViewAction.OnDisallowedCardBrandEntered(brand))
    }
}
