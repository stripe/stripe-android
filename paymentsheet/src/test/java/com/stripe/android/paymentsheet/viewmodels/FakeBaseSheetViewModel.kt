package com.stripe.android.paymentsheet.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.NewOrExternalPaymentSelection
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

private fun mockLinkHandler(): LinkHandler {
    val linkHandler = mock<LinkHandler>()
    linkHandler.stub {
        whenever(linkHandler.isLinkEnabled).thenReturn(stateFlowOf(false))
    }
    return linkHandler
}

internal class FakeBaseSheetViewModel : BaseSheetViewModel(
    application = mock(),
    config = PaymentSheet.Configuration.Builder("Example, Inc.").build(),
    eventReporter = mock(),
    customerRepository = FakeCustomerRepository(),
    workContext = Dispatchers.IO,
    savedStateHandle = SavedStateHandle(),
    linkHandler = mockLinkHandler(),
    linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
    editInteractorFactory = mock(),
    isCompleteFlow = true,
) {
    override val walletsState: StateFlow<WalletsState?>
        get() = stateFlowOf(null)
    override val walletsProcessingState: StateFlow<WalletsProcessingState?>
        get() = stateFlowOf(null)
    override val primaryButtonUiState: StateFlow<PrimaryButton.UIState?>
        get() = stateFlowOf(PrimaryButton.UIState("Gimme money!".resolvableString, {}, false, true))
    override val error: StateFlow<ResolvableString?>
        get() = stateFlowOf(null)
    override var newPaymentSelection: NewOrExternalPaymentSelection?
        get() = null
        set(value) {}

    override fun onError(error: ResolvableString?) {
    }

    override fun clearErrorMessages() {
    }

    override fun handlePaymentMethodSelected(selection: PaymentSelection?) {
    }

    override fun handleConfirmUSBankAccount(paymentSelection: PaymentSelection.New.USBankAccount) {
    }

    override fun onUserCancel() {
    }

    override fun onPaymentResult(paymentResult: PaymentResult) {
    }
}
