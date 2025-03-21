package com.stripe.android.paymentsheet.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.NewOrExternalPaymentSelection
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.mockito.kotlin.mock

private fun linkHandler(): LinkHandler {
    return LinkHandler(
        linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
    )
}

internal class FakeBaseSheetViewModel private constructor(
    savedStateHandle: SavedStateHandle,
    linkHandler: LinkHandler,
    paymentMethodMetadata: PaymentMethodMetadata,
) : BaseSheetViewModel(
    config = PaymentSheet.Configuration.Builder("Example, Inc.").build(),
    eventReporter = FakeEventReporter(),
    customerRepository = FakeCustomerRepository(),
    workContext = Dispatchers.IO,
    savedStateHandle = savedStateHandle,
    linkHandler = linkHandler,
    cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
    isCompleteFlow = true,
) {
    companion object {
        fun create(
            paymentMethodMetadata: PaymentMethodMetadata,
            initialScreen: PaymentSheetScreen,
            canGoBack: Boolean,
        ): FakeBaseSheetViewModel {
            val savedStateHandle = SavedStateHandle()
            val linkHandler = linkHandler()
            return FakeBaseSheetViewModel(savedStateHandle, linkHandler, paymentMethodMetadata).apply {
                if (canGoBack) {
                    navigationHandler.resetTo(
                        listOf(mock(), initialScreen)
                    )
                } else {
                    navigationHandler.transitionTo(initialScreen)
                }
            }.also {
                if (initialScreen.buyButtonState.value.visible) {
                    it.primaryButtonUiStateSource.update {
                        PrimaryButton.UIState(
                            label = "Gimme money!".resolvableString,
                            onClick = {},
                            enabled = false,
                            lockVisible = true,
                        )
                    }
                }
            }
        }
    }

    init {
        setPaymentMethodMetadata(paymentMethodMetadata)
    }

    val walletsStateSource = MutableStateFlow<WalletsState?>(null)
    override val walletsState: StateFlow<WalletsState?> = walletsStateSource.asStateFlow()

    val walletsProcessingStateSource = MutableStateFlow<WalletsProcessingState?>(null)
    override val walletsProcessingState: StateFlow<WalletsProcessingState?> = walletsProcessingStateSource.asStateFlow()

    val primaryButtonUiStateSource = MutableStateFlow<PrimaryButton.UIState?>(null)
    override val primaryButtonUiState: StateFlow<PrimaryButton.UIState?> = primaryButtonUiStateSource.asStateFlow()

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

    override fun onUserCancel() {
        throw AssertionError("Not expected.")
    }
}
