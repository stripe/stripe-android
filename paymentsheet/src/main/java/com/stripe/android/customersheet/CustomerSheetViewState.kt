package com.stripe.android.customersheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility

internal sealed class CustomerSheetViewState(
    open val savedPaymentMethods: List<PaymentMethod>,
    open val isLiveMode: Boolean,
    open val isProcessing: Boolean,
    open val isEditing: Boolean,
    open val screen: PaymentSheetScreen,
    open val cbcEligibility: CardBrandChoiceEligibility,
    open val allowsRemovalOfLastSavedPaymentMethod: Boolean,
) {

    private fun isModifiable(paymentMethod: PaymentMethod): Boolean {
        val hasMultipleNetworks = paymentMethod.card?.networks?.available?.let { available ->
            available.size > 1
        } ?: false

        return cbcEligibility is CardBrandChoiceEligibility.Eligible && hasMultipleNetworks
    }

    private val canEdit: Boolean
        get() {
            return if (allowsRemovalOfLastSavedPaymentMethod) {
                savedPaymentMethods.isNotEmpty()
            } else {
                if (savedPaymentMethods.size == 1) {
                    isModifiable(savedPaymentMethods.first())
                } else {
                    savedPaymentMethods.size > 1
                }
            }
        }

    val topBarState: PaymentSheetTopBarState
        get() = PaymentSheetTopBarStateFactory.create(
            screen = screen,
            isLiveMode = isLiveMode,
            isProcessing = isProcessing,
            isEditing = isEditing,
            canEdit = canEdit,
        )

    fun shouldDisplayDismissConfirmationModal(
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
    ): Boolean {
        return this is AddPaymentMethod &&
            paymentMethodCode == PaymentMethod.Type.USBankAccount.code &&
            isFinancialConnectionsAvailable() &&
            bankAccountResult is CollectBankAccountResultInternal.Completed &&
            bankAccountResult.response.financialConnectionsSession.paymentAccount is FinancialConnectionsAccount
    }

    data class Loading(
        override val isLiveMode: Boolean,
    ) : CustomerSheetViewState(
        savedPaymentMethods = emptyList(),
        isLiveMode = isLiveMode,
        isProcessing = false,
        isEditing = false,
        screen = PaymentSheetScreen.Loading,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        allowsRemovalOfLastSavedPaymentMethod = true,
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
        val errorMessage: String? = null,
        val unconfirmedPaymentMethod: PaymentMethod? = null,
        val mandateText: String? = null,
        override val cbcEligibility: CardBrandChoiceEligibility,
    ) : CustomerSheetViewState(
        savedPaymentMethods = savedPaymentMethods,
        isLiveMode = isLiveMode,
        isProcessing = isProcessing,
        isEditing = isEditing,
        screen = PaymentSheetScreen.SelectSavedPaymentMethods,
        cbcEligibility = cbcEligibility,
        allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
    ) {
        val primaryButtonEnabled: Boolean
            get() = !isProcessing
    }

    data class AddPaymentMethod(
        val paymentMethodCode: PaymentMethodCode,
        val supportedPaymentMethods: List<SupportedPaymentMethod>,
        val formViewData: FormViewModel.ViewData,
        val formArguments: FormArguments,
        val usBankAccountFormArguments: USBankAccountFormArguments,
        val selectedPaymentMethod: SupportedPaymentMethod,
        val draftPaymentSelection: PaymentSelection?,
        val enabled: Boolean,
        override val isLiveMode: Boolean,
        override val isProcessing: Boolean,
        val errorMessage: String? = null,
        val isFirstPaymentMethod: Boolean,
        val primaryButtonLabel: ResolvableString,
        val primaryButtonEnabled: Boolean,
        val customPrimaryButtonUiState: PrimaryButton.UIState?,
        val mandateText: String? = null,
        val showMandateAbovePrimaryButton: Boolean = false,
        val displayDismissConfirmationModal: Boolean = false,
        val bankAccountResult: CollectBankAccountResultInternal?,
        override val cbcEligibility: CardBrandChoiceEligibility,
    ) : CustomerSheetViewState(
        savedPaymentMethods = emptyList(),
        isLiveMode = isLiveMode,
        isProcessing = isProcessing,
        isEditing = false,
        screen = if (isFirstPaymentMethod) {
            PaymentSheetScreen.AddFirstPaymentMethod
        } else {
            PaymentSheetScreen.AddAnotherPaymentMethod
        },
        cbcEligibility = cbcEligibility,
        allowsRemovalOfLastSavedPaymentMethod = true,
    )

    data class EditPaymentMethod(
        val editPaymentMethodInteractor: ModifiableEditPaymentMethodViewInteractor,
        override val isLiveMode: Boolean,
        override val cbcEligibility: CardBrandChoiceEligibility,
        override val savedPaymentMethods: List<PaymentMethod>,
        override val allowsRemovalOfLastSavedPaymentMethod: Boolean,
    ) : CustomerSheetViewState(
        savedPaymentMethods = savedPaymentMethods,
        isLiveMode = isLiveMode,
        isProcessing = false,
        isEditing = false,
        screen = PaymentSheetScreen.EditPaymentMethod(editPaymentMethodInteractor),
        cbcEligibility = cbcEligibility,
        allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
    )
}
