package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface ManageScreenInteractor {
    val state: StateFlow<State>

    data class State(
        val paymentMethods: List<DisplayableSavedPaymentMethod>,
    )
}

internal class DefaultManageScreenInteractor(viewModel: BaseSheetViewModel) : ManageScreenInteractor {
    override val state = combineAsStateFlow(
        viewModel.paymentMethods,
        viewModel.paymentMethodMetadata
    ) { paymentMethods, paymentMethodMetadata ->
        val displayablePaymentMethods = paymentMethods?.map {
            DisplayableSavedPaymentMethod(
                displayName = viewModel.providePaymentMethodName(it.type?.code),
                paymentMethod = it,
                isCbcEligible = paymentMethodMetadata?.cbcEligibility is CardBrandChoiceEligibility.Eligible,
            )
        }
        ManageScreenInteractor.State(displayablePaymentMethods ?: emptyList())
    }
}
