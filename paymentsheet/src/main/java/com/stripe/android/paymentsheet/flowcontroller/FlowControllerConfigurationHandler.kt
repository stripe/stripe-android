package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.core.injection.UIContext
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.validate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.InvalidParameterException
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class FlowControllerConfigurationHandler @Inject constructor(
    private val paymentSheetLoader: PaymentSheetLoader,
    @UIContext private val uiContext: CoroutineContext,
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
    private val paymentSelectionUpdater: PaymentSelectionUpdater,
) {

    private val job: AtomicReference<Job?> = AtomicReference(null)

    private var didLastConfigurationFail: Boolean = false

    val isConfigured: Boolean
        get() {
            val isConfiguring = job.get()?.let { !it.isCompleted } ?: false
            return !isConfiguring && !didLastConfigurationFail
        }

    fun configure(
        scope: CoroutineScope,
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) {
        val oldJob = job.getAndSet(
            scope.launch {
                configureInternal(
                    initializationMode = initializationMode,
                    configuration = configuration,
                    callback = callback,
                )
            }
        )
        oldJob?.cancel()
    }

    private suspend fun configureInternal(
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) {
        suspend fun onConfigured(error: Throwable? = null) {
            withContext(uiContext) {
                didLastConfigurationFail = error != null
                resetJob()
                callback.onConfigured(success = error == null, error = error)
            }
        }

        try {
            initializationMode.validate()
            configuration?.validate()
        } catch (e: InvalidParameterException) {
            onConfigured(error = e)
            return
        }

        val configureRequest = ConfigureRequest(initializationMode, configuration)
        val canSkip = viewModel.previousConfigureRequest == configureRequest

        if (canSkip) {
            onConfigured()
            return
        }

        when (val result = paymentSheetLoader.load(initializationMode, configuration)) {
            is PaymentSheetLoader.Result.Success -> {
                viewModel.previousConfigureRequest = configureRequest
                onInitSuccess(result.state)
                onConfigured()
            }
            is PaymentSheetLoader.Result.Failure -> {
                onConfigured(error = result.throwable)
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

    private fun resetJob() {
        job.set(null)
    }

    data class ConfigureRequest(
        val initializationMode: PaymentSheet.InitializationMode,
        val configuration: PaymentSheet.Configuration?,
    )
}
