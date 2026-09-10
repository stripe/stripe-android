package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
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
        private val argsSupplier: () -> EmbeddedActivityArgs,
    ) : ViewModelProvider.Factory {
        fun createReadyPresentation(
            activity: EmbeddedSheetActivity,
            args: EmbeddedActivityArgs,
            activityResultCaller: ActivityResultCaller,
        ): EmbeddedSheetPresentation {
            val viewModel = ViewModelProvider(activity, this)[EmbeddedSheetViewModel::class.java]
            return viewModel.embeddedSheetPresentationFactory.create(
                activity = activity,
                args = args,
                activityResultCaller = activityResultCaller,
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argsSupplier()
            check(args.presentationState == EmbeddedActivityArgs.PresentationState.Ready)
            val customViewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val component = DaggerEmbeddedSheetComponent.factory().build(
                paymentMethodMetadata = requireNotNull(args.paymentMethodMetadata),
                statusBarColor = args.statusBarColor,
                configuration = args.configuration,
                productUsage = args.productUsage,
                paymentElementCallbackIdentifier = args.paymentElementCallbackIdentifier,
                application = extras.requireApplication(),
                savedStateHandle = extras.createSavedStateHandle(),
                promotions = args.promotions,
                launchMode = args.launchMode,
                activityConfiguration = args.activityConfiguration,
                viewModelScope = customViewModelScope,
            )

            component.customerStateHolder.setCustomerState(args.customerState)
            component.selectionHolder.setPreviousNewSelections(args.previousNewSelections)
            component.selectionHolder.setSelection(args.selection)

            return component.viewModel as T
        }
    }
}
