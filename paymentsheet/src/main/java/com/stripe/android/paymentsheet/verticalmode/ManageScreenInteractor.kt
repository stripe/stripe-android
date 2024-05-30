package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface ManageScreenInteractor {
    val state: StateFlow<State>

    data class State(
        val paymentMethods: List<DisplayableSavedPaymentMethod>,
    )
}

internal class DefaultManageScreenInteractor(viewModel: BaseSheetViewModel) : ManageScreenInteractor {
    override val state = viewModel.paymentMethods.mapAsStateFlow { paymentMethods ->
        val displayablePaymentMethods = paymentMethods?.map {
            DisplayableSavedPaymentMethod(
                displayName = viewModel.providePaymentMethodName(it.type?.code),
                paymentMethod = it,
                isCbcEligible = viewModel.paymentMethodMetadata.value?.cbcEligibility is CardBrandChoiceEligibility.Eligible,
            )
        }
        ManageScreenInteractor.State(displayablePaymentMethods ?: emptyList())
    }
}
