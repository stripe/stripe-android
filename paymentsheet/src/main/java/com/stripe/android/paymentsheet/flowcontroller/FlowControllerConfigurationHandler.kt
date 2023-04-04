package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.core.injection.UIContext
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.validate
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.security.InvalidParameterException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class FlowControllerConfigurationHandler @Inject constructor(
    private val paymentSheetLoader: PaymentSheetLoader,
    @UIContext private val uiContext: CoroutineContext,
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
) {

    suspend fun configure(
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) {
        try {
            initializationMode.validate()
            configuration?.validate()
        } catch (e: InvalidParameterException) {
            callback.onConfigured(false, e)
            return
        }

        val canSkip = viewModel.canSkipLoad(initializationMode, configuration)

        if (canSkip) {
            callback.onConfigured(true, null)
            return
        }

        val result = paymentSheetLoader.load(initializationMode, configuration)

        if (currentCoroutineContext().isActive) {
            dispatchResult(result, initializationMode, callback)
        } else {
            callback.onConfigured(false, null)
        }
    }

    private suspend fun dispatchResult(
        result: PaymentSheetLoader.Result,
        initializationMode: PaymentSheet.InitializationMode,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) = withContext(uiContext) {
        when (result) {
            is PaymentSheetLoader.Result.Success -> {
                onInitSuccess(result.state, initializationMode, callback)
            }
            is PaymentSheetLoader.Result.Failure -> {
                callback.onConfigured(false, result.throwable)
            }
        }
    }

    private fun onInitSuccess(
        state: PaymentSheetState.Full,
        initializationMode: PaymentSheet.InitializationMode,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) {
        eventReporter.onInit(state.config)
        viewModel.storeLastInput(initializationMode, state.config)

        viewModel.paymentSelection = PaymentSelectionUpdater.process(
            currentSelection = viewModel.paymentSelection,
            newState = state,
        )

        viewModel.state = state

        callback.onConfigured(true, null)
    }
}
