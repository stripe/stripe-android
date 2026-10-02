package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
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
 * In Manage mode the selection is the sheet result, so this syncs the Checkout Session billing
 * tax region, when required, before committing. Other launch modes sync on Continue in
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
        val paymentMethodId = selection.paymentMethod.id
        val update = taxRegionUpdater.prepareUpdate(paymentMethodMetadata, selection)
        val response = update?.let {
            _selectionState.value = SavedPaymentMethodSelectionState.Pending(paymentMethodId)
            it().getOrElse { error ->
                _selectionState.value = SavedPaymentMethodSelectionState.Failed(error.stripeErrorMessage())
                return Result.failure(error)
            }
        }

        checkoutSessionResponse = response
        selectionHolder.setSelection(selection)
        _selectionState.value = SavedPaymentMethodSelectionState.Idle
        return Result.success(Unit)
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
