package com.stripe.android.customersheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.SheetScreen
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.FormElement

internal sealed class CustomerSheetViewState(
    open val savedPaymentMethods: List<PaymentMethod>,
    open val isLiveMode: Boolean,
    open val isProcessing: Boolean,
    open val isEditing: Boolean,
    open val screen: SheetScreen,
    open val canNavigateBack: Boolean,
    open val cbcEligibility: CardBrandChoiceEligibility,
    open val allowsRemovalOfLastSavedPaymentMethod: Boolean,
    open val canRemovePaymentMethods: Boolean,
) {
    val topBarState: PaymentSheetTopBarState
        get() = PaymentSheetTopBarStateFactory.create(
            screen = screen,
            hasBackStack = canNavigateBack,
            isLiveMode = isLiveMode,
            isProcessing = isProcessing,
            isEditing = isEditing,
            canEdit = canEdit(allowsRemovalOfLastSavedPaymentMethod, savedPaymentMethods, cbcEligibility),
        )

    fun shouldDisplayDismissConfirmationModal(
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
    ): Boolean {
        return this is AddPaymentMethod &&
            paymentMethodCode == PaymentMethod.Type.USBankAccount.code &&
            isFinancialConnectionsAvailable() &&
            bankAccountResult is CollectBankAccountResultInternal.Completed &&
            bankAccountResult.response.usBankAccountData
                ?.financialConnectionsSession
                ?.paymentAccount is FinancialConnectionsAccount
    }

    data class Loading(
        override val isLiveMode: Boolean,
    ) : CustomerSheetViewState(
        savedPaymentMethods = emptyList(),
        isLiveMode = isLiveMode,
        isProcessing = false,
        isEditing = false,
        screen = SheetScreen.LOADING,
        canNavigateBack = false,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        allowsRemovalOfLastSavedPaymentMethod = true,
        canRemovePaymentMethods = false,
    )

    data class SelectPaymentMethod(
        val title: String?,
        override val savedPaymentMethods: List<PaymentMethod>,
        val paymentSelection: PaymentSelection?,
        override val isLiveMode: Boolean,
        override val isProcessing: Boolean,
        override val isEditing: Boolean,
        val isGooglePayEnabled: Boolean,
        val primaryButtonVisible: Boolean,
        val primaryButtonLabel: String?,
        override val allowsRemovalOfLastSavedPaymentMethod: Boolean,
        override val canRemovePaymentMethods: Boolean,
        val errorMessage: String? = null,
        val unconfirmedPaymentMethod: PaymentMethod? = null,
        val mandateText: ResolvableString? = null,
        override val cbcEligibility: CardBrandChoiceEligibility,
    ) : CustomerSheetViewState(
        savedPaymentMethods = savedPaymentMethods,
        isLiveMode = isLiveMode,
        isProcessing = isProcessing,
        isEditing = isEditing,
        screen = SheetScreen.SELECT_SAVED_PAYMENT_METHODS,
        canNavigateBack = false,
        cbcEligibility = cbcEligibility,
        allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
        canRemovePaymentMethods = canRemovePaymentMethods,
    ) {
        val primaryButtonEnabled: Boolean
            get() = !isProcessing
    }

    data class AddPaymentMethod(
        val paymentMethodCode: PaymentMethodCode,
        val supportedPaymentMethods: List<SupportedPaymentMethod>,
        val formFieldValues: FormFieldValues?,
        val formElements: List<FormElement>,
        val formArguments: FormArguments,
        val usBankAccountFormArguments: USBankAccountFormArguments,
        val draftPaymentSelection: PaymentSelection?,
        val enabled: Boolean,
        override val isLiveMode: Boolean,
        override val isProcessing: Boolean,
        val errorMessage: ResolvableString? = null,
        val isFirstPaymentMethod: Boolean,
        val primaryButtonLabel: ResolvableString,
        val primaryButtonEnabled: Boolean,
        val customPrimaryButtonUiState: PrimaryButton.UIState?,
        val mandateText: ResolvableString? = null,
        val showMandateAbovePrimaryButton: Boolean = false,
        val displayDismissConfirmationModal: Boolean = false,
        val bankAccountResult: CollectBankAccountResultInternal?,
        override val cbcEligibility: CardBrandChoiceEligibility,
        val errorReporter: ErrorReporter,
    ) : CustomerSheetViewState(
        savedPaymentMethods = emptyList(),
        isLiveMode = isLiveMode,
        isProcessing = isProcessing,
        isEditing = false,
        screen = if (isFirstPaymentMethod) {
            SheetScreen.ADD_FIRST_PAYMENT_METHOD
        } else {
            SheetScreen.ADD_ANOTHER_PAYMENT_METHOD
        },
        canNavigateBack = !isFirstPaymentMethod,
        cbcEligibility = cbcEligibility,
        allowsRemovalOfLastSavedPaymentMethod = true,
        canRemovePaymentMethods = false,
    )

    data class EditPaymentMethod(
        val editPaymentMethodInteractor: ModifiableEditPaymentMethodViewInteractor,
        override val isLiveMode: Boolean,
        override val cbcEligibility: CardBrandChoiceEligibility,
        override val savedPaymentMethods: List<PaymentMethod>,
        override val allowsRemovalOfLastSavedPaymentMethod: Boolean,
        override val canRemovePaymentMethods: Boolean,
    ) : CustomerSheetViewState(
        savedPaymentMethods = savedPaymentMethods,
        isLiveMode = isLiveMode,
        isProcessing = false,
        isEditing = false,
        screen = SheetScreen.EDIT_PAYMENT_METHOD,
        canNavigateBack = true,
        cbcEligibility = cbcEligibility,
        allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
        canRemovePaymentMethods = canRemovePaymentMethods,
    )
}

internal fun canEdit(
    allowsRemovalOfLastSavedPaymentMethod: Boolean,
    savedPaymentMethods: List<PaymentMethod>,
    cbcEligibility: CardBrandChoiceEligibility,
): Boolean {
    return if (allowsRemovalOfLastSavedPaymentMethod) {
        savedPaymentMethods.isNotEmpty()
    } else {
        if (savedPaymentMethods.size == 1) {
            isModifiable(savedPaymentMethods.first(), cbcEligibility)
        } else {
            savedPaymentMethods.size > 1
        }
    }
}

private fun isModifiable(paymentMethod: PaymentMethod, cbcEligibility: CardBrandChoiceEligibility): Boolean {
    val hasMultipleNetworks = paymentMethod.card?.networks?.available?.let { available ->
        available.size > 1
    } ?: false

    return cbcEligibility is CardBrandChoiceEligibility.Eligible && hasMultipleNetworks
}
