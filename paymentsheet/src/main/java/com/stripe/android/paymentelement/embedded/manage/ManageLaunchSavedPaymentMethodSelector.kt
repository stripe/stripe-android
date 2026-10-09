package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityContinueCoordinator
import com.stripe.android.paymentelement.embedded.sheet.SheetTaxRegionUpdater
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manage mode has no Continue step, so selections and saved-card edits sync the Checkout Session
 * billing tax region before returning the sheet result. Other launch modes sync on Continue in
 * [SheetActivityContinueCoordinator].
 */
@Singleton
internal class ManageLaunchSavedPaymentMethodSelector @Inject constructor(
    private val taxRegionUpdater: SheetTaxRegionUpdater,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val selectionHolder: EmbeddedSelectionHolder,
) : ManageScreenSavedPaymentMethodSelector {
    private val _selectionState = MutableStateFlow<SavedPaymentMethodSelectionState>(
        SavedPaymentMethodSelectionState.Idle,
    )
    override val selectionState: StateFlow<SavedPaymentMethodSelectionState> = _selectionState

    override var checkoutSessionResponse: CheckoutSessionResponse? = null
        private set

    override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> {
        val update = taxRegionUpdater.prepareUpdate(paymentMethodMetadata, selection, checkoutSessionResponse)
        if (update != null) {
            _selectionState.value = SavedPaymentMethodSelectionState.Pending(selection.paymentMethod.id)
        }
        return (update?.invoke() ?: Result.success(null)).map { response ->
            response?.let(::updateCheckoutSessionResponse)
            selectionHolder.setSelection(selection)
            _selectionState.value = SavedPaymentMethodSelectionState.Idle
        }.onFailure { error ->
            _selectionState.value = SavedPaymentMethodSelectionState.Failed(error.stripeErrorMessage())
        }
    }

    override suspend fun syncBillingAfterEdit(
        original: PaymentMethod,
        updated: PaymentMethod,
    ): Result<CheckoutSessionResponse?> {
        val selected = selectionHolder.selection.value as? PaymentSelection.Saved
            ?: return Result.success(null)
        if (selected.paymentMethod.id != updated.id ||
            original.billingDetails?.address == updated.billingDetails?.address
        ) {
            return Result.success(null)
        }
        return taxRegionUpdater.prepareUpdate(
            paymentMethodMetadata,
            PaymentSelection.Saved(updated),
            checkoutSessionResponse,
        )?.invoke() ?: Result.success(null)
    }

    override fun updateCheckoutSessionResponse(response: CheckoutSessionResponse) {
        checkoutSessionResponse = response
    }

    override fun clearError() {
        _selectionState.update { state ->
            if (state is SavedPaymentMethodSelectionState.Failed) {
                SavedPaymentMethodSelectionState.Idle
            } else {
                state
            }
        }
    }
}
