package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface ManageScreenInteractor {
    val state: StateFlow<State>

    data class State(
        val paymentMethods: List<DisplayableSavedPaymentMethod>?,
    )
}

internal class DefaultManageScreenInteractor(viewModel: BaseSheetViewModel) : ManageScreenInteractor {
    override val state = viewModel.displayableSavedPaymentMethods.mapAsStateFlow { paymentMethods ->
        ManageScreenInteractor.State(paymentMethods)
    }
}
