package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.validate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.security.InvalidParameterException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FlowControllerConfigurationHandler @Inject constructor(
    private val paymentSheetLoader: PaymentSheetLoader,
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
    private val paymentSelectionUpdater: PaymentSelectionUpdater,
) {

    private var job: CompletableJob? = null

    fun configure(
        scope: CoroutineScope,
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) {
        job?.cancel()

        job = Job().apply {
            invokeOnCompletion { error ->
                if (error !is CancellationException) {
                    callback.onConfigured(
                        success = error == null,
                        error = error,
                    )
                    job = null
                }
            }
        }

        scope.launch(job!!) {
            configureInternal(
                initializationMode = initializationMode,
                configuration = configuration,
            )
        }
    }

    private suspend fun configureInternal(
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
    ) {
        try {
            initializationMode.validate()
            configuration?.validate()
        } catch (e: InvalidParameterException) {
            job?.completeExceptionally(e)
            return
        }

        val configureRequest = ConfigureRequest(initializationMode, configuration)
        val canSkip = viewModel.previousConfigureRequest == configureRequest

        if (canSkip) {
            job?.complete()
            return
        }

        when (val result = paymentSheetLoader.load(initializationMode, configuration)) {
            is PaymentSheetLoader.Result.Success -> {
                viewModel.previousConfigureRequest = configureRequest
                onInitSuccess(result.state)
                job?.complete()
            }
            is PaymentSheetLoader.Result.Failure -> {
                job?.completeExceptionally(result.throwable)
            }
        }
    }

    private fun onInitSuccess(state: PaymentSheetState.Full) {
        eventReporter.onInit(state.config)

        viewModel.paymentSelection = paymentSelectionUpdater(
            currentSelection = viewModel.paymentSelection,
            newState = state,
        )

        viewModel.state = state
    }

    data class ConfigureRequest(
        val initializationMode: PaymentSheet.InitializationMode,
        val configuration: PaymentSheet.Configuration?,
    )
}
