package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface PaymentMethodVerticalLayoutInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val supportedPaymentMethods: List<SupportedPaymentMethod>,
        val isProcessing: Boolean,
    )

    sealed interface ViewAction {
        data object TransitionToManageSavedPaymentMethods : ViewAction
        data class TransitionToForm(val selectedPaymentMethodCode: String) : ViewAction
    }
}

internal class DefaultPaymentMethodVerticalLayoutInteractor(
    private val viewModel: BaseSheetViewModel,
) : PaymentMethodVerticalLayoutInteractor {
    override val state: StateFlow<PaymentMethodVerticalLayoutInteractor.State> = combineAsStateFlow(
        viewModel.processing,
        viewModel.paymentMethodMetadata,
    ) { isProcessing, paymentMethodMetadata ->
        PaymentMethodVerticalLayoutInteractor.State(
            supportedPaymentMethods = paymentMethodMetadata?.sortedSupportedPaymentMethods() ?: emptyList(),
            isProcessing = isProcessing,
        )
    }

    override fun handleViewAction(viewAction: PaymentMethodVerticalLayoutInteractor.ViewAction) {
        when (viewAction) {
            is PaymentMethodVerticalLayoutInteractor.ViewAction.TransitionToForm -> {
                viewModel.transitionTo(
                    PaymentSheetScreen.Form(
                        DefaultVerticalModeFormInteractor(
                            viewAction.selectedPaymentMethodCode,
                            viewModel
                        )
                    )
                )
            }
            PaymentMethodVerticalLayoutInteractor.ViewAction.TransitionToManageSavedPaymentMethods -> {
                viewModel.transitionTo(
                    PaymentSheetScreen.ManageSavedPaymentMethods(
                        interactor = DefaultManageScreenInteractor(
                            viewModel
                        )
                    )
                )
            }
        }
    }
}
