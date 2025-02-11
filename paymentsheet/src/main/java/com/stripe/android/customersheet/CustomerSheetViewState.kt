package com.stripe.android.customersheet

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.FormElement

internal sealed class CustomerSheetViewState(
    open val isLiveMode: Boolean,
    open val isProcessing: Boolean,
    open val canNavigateBack: Boolean,
) {
    abstract fun topBarState(onEditIconPressed: () -> Unit): PaymentSheetTopBarState

    fun shouldDisplayDismissConfirmationModal(): Boolean {
        return when (this) {
            is Loading,
            is UpdatePaymentMethod,
            is SelectPaymentMethod -> {
                false
            }
            is AddPaymentMethod -> {
                paymentMethodCode == PaymentMethod.Type.USBankAccount.code && bankAccountSelection != null
            }
        }
    }

    data class Loading(
        override val isLiveMode: Boolean,
    ) : CustomerSheetViewState(
        isLiveMode = isLiveMode,
        isProcessing = false,
        canNavigateBack = false,
    ) {
        override fun topBarState(onEditIconPressed: () -> Unit): PaymentSheetTopBarState {
            return PaymentSheetTopBarStateFactory.create(
                isLiveMode = isLiveMode,
                editable = PaymentSheetTopBarState.Editable.Never,
            )
        }
    }

    data class SelectPaymentMethod(
        val title: String?,
        val savedPaymentMethods: List<PaymentMethod>,
        val paymentSelection: PaymentSelection?,
        override val isLiveMode: Boolean,
        override val isProcessing: Boolean,
        val isEditing: Boolean,
        val isGooglePayEnabled: Boolean,
        val primaryButtonVisible: Boolean,
        val canEdit: Boolean,
        val canRemovePaymentMethods: Boolean,
        val errorMessage: String? = null,
        val mandateText: ResolvableString? = null,
        val isCbcEligible: Boolean,
    ) : CustomerSheetViewState(
        isLiveMode = isLiveMode,
        isProcessing = isProcessing,
        canNavigateBack = false,
    ) {
        val primaryButtonLabel: ResolvableString = R.string.stripe_paymentsheet_confirm.resolvableString

        val primaryButtonEnabled: Boolean
            get() = !isProcessing

        override fun topBarState(onEditIconPressed: () -> Unit): PaymentSheetTopBarState {
            return PaymentSheetTopBarStateFactory.create(
                isLiveMode = isLiveMode,
                editable = PaymentSheetTopBarState.Editable.Maybe(
                    isEditing = isEditing,
                    canEdit = canEdit,
                    onEditIconPressed = onEditIconPressed,
                ),
            )
        }
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
        val bankAccountSelection: PaymentSelection.New.USBankAccount?,
        val errorReporter: ErrorReporter,
    ) : CustomerSheetViewState(
        isLiveMode = isLiveMode,
        isProcessing = isProcessing,
        canNavigateBack = !isFirstPaymentMethod,
    ) {
        override fun topBarState(onEditIconPressed: () -> Unit): PaymentSheetTopBarState {
            return PaymentSheetTopBarStateFactory.create(
                isLiveMode = isLiveMode,
                editable = PaymentSheetTopBarState.Editable.Never,
            )
        }
    }

    data class UpdatePaymentMethod(
        val updatePaymentMethodInteractor: UpdatePaymentMethodInteractor,
        override val isLiveMode: Boolean,
    ) : CustomerSheetViewState(
        isLiveMode = isLiveMode,
        isProcessing = false,
        canNavigateBack = true,
    ) {
        override fun topBarState(onEditIconPressed: () -> Unit): PaymentSheetTopBarState {
            return PaymentSheetTopBarStateFactory.create(
                isLiveMode = isLiveMode,
                editable = PaymentSheetTopBarState.Editable.Never,
            )
        }
    }
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

internal fun isModifiable(paymentMethod: PaymentMethod, cbcEligibility: CardBrandChoiceEligibility): Boolean {
    val hasMultipleNetworks = paymentMethod.card?.networks?.available?.let { available ->
        available.size > 1
    } ?: false

    return cbcEligibility is CardBrandChoiceEligibility.Eligible && hasMultipleNetworks
}
