package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentelement.embedded.EmbeddedActivityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

internal class EmbeddedSheetViewModel @Inject constructor(
    private val embeddedSheetPresentationFactory: ReadyEmbeddedSheetPresentation.Factory,
    @ViewModelScope private val customViewModelScope: CoroutineScope,
) : ViewModel() {
    override fun onCleared() {
        customViewModelScope.cancel()
    }

    class Factory(
        private val stateSupplier: () -> EmbeddedActivityState.Ready,
    ) : ViewModelProvider.Factory {
        fun createReadyPresentation(
            activity: EmbeddedSheetActivity,
            state: EmbeddedActivityState.Ready,
            activityResultCaller: ActivityResultCaller,
        ): EmbeddedSheetPresentation {
            val viewModel = ViewModelProvider(activity, this)[EmbeddedSheetViewModel::class.java]
            return viewModel.embeddedSheetPresentationFactory.create(
                activity = activity,
                state = state,
                activityResultCaller = activityResultCaller,
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val state = stateSupplier()
            val context = state.context
            val customViewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val component = DaggerEmbeddedSheetComponent.factory().build(
                paymentMethodMetadata = context.paymentMethodMetadata,
                statusBarColor = context.statusBarColor,
                configuration = context.configuration,
                productUsage = context.productUsage,
                paymentElementCallbackIdentifier = context.paymentElementCallbackIdentifier,
                application = extras.requireApplication(),
                savedStateHandle = extras.createSavedStateHandle(),
                promotions = state.promotions,
                launchMode = state.launchMode,
                viewModelScope = customViewModelScope,
            )

            component.customerStateHolder.setCustomerState(state.customerState)
            component.selectionHolder.setPreviousNewSelections(state.previousNewSelections)
            component.selectionHolder.setSelection(state.initialSelection)

            return component.viewModel as T
        }
    }
}
