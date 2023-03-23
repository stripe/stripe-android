package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.core.injection.UIContext
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.repositories.toElementsSessionParams
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.validate
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
    ): Throwable? {
        try {
            initializationMode.validate()
            configuration?.validate()
        } catch (e: InvalidParameterException) {
            return e
        }

        val elementsSessionParams = initializationMode.toElementsSessionParams(configuration)
        val previousElementsSessionParams = viewModel.previousElementsSessionParams
        if (elementsSessionParams == previousElementsSessionParams) {
            return null
        }
        val result = paymentSheetLoader.load(initializationMode, configuration)

        viewModel.initializationMode = initializationMode
        return dispatchResult(result, elementsSessionParams)
    }

    private suspend fun dispatchResult(
        result: PaymentSheetLoader.Result,
        elementsSessionParams: ElementsSessionParams,
    ): Throwable? = withContext(uiContext) {
        when (result) {
            is PaymentSheetLoader.Result.Success -> {
                viewModel.previousElementsSessionParams = elementsSessionParams
                onInitSuccess(result.state)
                null
            }
            is PaymentSheetLoader.Result.Failure -> {
                result.throwable
            }
        }
    }

    private fun onInitSuccess(
        state: PaymentSheetState.Full,
    ) {
        eventReporter.onInit(state.config)

        viewModel.paymentSelection = PaymentSelectionUpdater.process(
            currentSelection = viewModel.paymentSelection,
            newState = state,
        )

        viewModel.state = state
    }
}
