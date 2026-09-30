package com.stripe.android.paymentelement.embedded.manage

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityContinueCoordinator
import com.stripe.android.paymentelement.embedded.sheet.SheetTaxRegionUpdater
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    override suspend fun select(selection: PaymentSelection.Saved) {
        val paymentMethodId = selection.paymentMethod.id
        val update = taxRegionUpdater.prepareUpdate(paymentMethodMetadata, selection)
        val response = update?.let {
            _selectionState.value = SavedPaymentMethodSelectionState.Pending(paymentMethodId)
            it().getOrNull()
        }

        checkoutSessionResponse = response
        selectionHolder.setSelection(selection)
        _selectionState.value = SavedPaymentMethodSelectionState.Idle
    }
}
