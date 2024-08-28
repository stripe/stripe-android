package com.stripe.android.paymentsheet.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.NewOrExternalPaymentSelection
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.NoOpEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.ThrowingEditPaymentMethodViewInteractorFactory
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

internal class FakeBaseSheetViewModel private constructor(): BaseSheetViewModel(
    config = PaymentSheet.Configuration.Builder("Example, Inc.").build(),
    eventReporter = NoOpEventReporter(),
    customerRepository = FakeCustomerRepository(),
    workContext = Dispatchers.IO,
    savedStateHandle = SavedStateHandle(),
    linkHandler = mockLinkHandler(),
    linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
    cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
    editInteractorFactory = ThrowingEditPaymentMethodViewInteractorFactory,
    isCompleteFlow = true,
) {
    companion object {
        fun create(): FakeBaseSheetViewModel {
            return FakeBaseSheetViewModel()
        }
    }

    override val walletsState: StateFlow<WalletsState?>
        get() = stateFlowOf(null)
    override val walletsProcessingState: StateFlow<WalletsProcessingState?>
        get() = stateFlowOf(null)
    override val primaryButtonUiState: StateFlow<PrimaryButton.UIState?>
        get() = stateFlowOf(
            PrimaryButton.UIState(
                label = "Gimme money!".resolvableString,
                onClick = {},
                enabled = false,
                lockVisible = true,
            )
        )

    private val errorSource = MutableStateFlow<ResolvableString?>(null)
    override val error: StateFlow<ResolvableString?> = errorSource.asStateFlow()

    override var newPaymentSelection: NewOrExternalPaymentSelection? = null

    override fun onError(error: ResolvableString?) {
        errorSource.value = error
    }

    override fun clearErrorMessages() {
        errorSource.value = null
    }

    override fun handlePaymentMethodSelected(selection: PaymentSelection?) {
        // Not yet implemented.
    }

    override fun handleConfirmUSBankAccount(paymentSelection: PaymentSelection.New.USBankAccount) {
        throw AssertionError("Not expected.")
    }

    override fun onUserCancel() {
        throw AssertionError("Not expected.")
    }

    override fun onPaymentResult(paymentResult: PaymentResult) {
        throw AssertionError("Not expected.")
    }
}
