package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.InitializationMode.DeferredIntent
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
        configuration: PaymentSheet.Configuration,
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
        configuration: PaymentSheet.Configuration,
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
            configuration.validate()
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

        AnalyticsRequestFactory.regenerateSessionId()

        paymentSheetLoader.load(initializationMode, configuration).fold(
            onSuccess = { state ->
                viewModel.previousConfigureRequest = configureRequest
                onInitSuccess(state, configureRequest)
                onConfigured()
            },
            onFailure = { error ->
                onConfigured(error = error)
            }
        )
    }

    private suspend fun onInitSuccess(
        state: PaymentSheetState.Full,
        configureRequest: ConfigureRequest,
    ) {
        val isDecoupling = configureRequest.initializationMode is DeferredIntent

        eventReporter.onInit(
            configuration = state.config,
            isDeferred = isDecoupling,
        )

        viewModel.paymentSelection = paymentSelectionUpdater(
            currentSelection = viewModel.paymentSelection,
            previousConfig = viewModel.state?.config,
            newState = state,
        )

        withContext(uiContext) {
            viewModel.state = state
        }
    }

    private fun resetJob() {
        job.set(null)
    }

    data class ConfigureRequest(
        val initializationMode: PaymentSheet.InitializationMode,
        val configuration: PaymentSheet.Configuration,
    )
}
