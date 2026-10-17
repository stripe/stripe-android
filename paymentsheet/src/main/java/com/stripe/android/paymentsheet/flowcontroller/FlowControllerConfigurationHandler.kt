package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.core.injection.UIContext
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class FlowControllerConfigurationHandler @Inject constructor(
    private val paymentElementLoader: PaymentElementLoader,
    @UIContext private val uiContext: CoroutineContext,
    private val viewModel: FlowControllerViewModel,
    private val paymentSelectionUpdater: PaymentSelectionUpdater,
    private val confirmationHandler: FlowControllerConfirmationHandler,
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
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: PaymentSheet.Configuration,
        initializedViaCompose: Boolean,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) {
        val oldJob = job.getAndSet(
            scope.launch {
                configureInternal(
                    initializationMode = initializationMode,
                    configuration = configuration,
                    initializedViaCompose = initializedViaCompose,
                    callback = callback,
                )
            }
        )
        oldJob?.cancel()
    }

    private suspend fun configureInternal(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: PaymentSheet.Configuration,
        initializedViaCompose: Boolean,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) {
        suspend fun onConfigured(error: Throwable? = null) {
            withContext(uiContext) {
                didLastConfigurationFail = error != null
                resetJob()
                callback.onConfigured(success = error == null, error = error)
            }
        }

        configuration.appearance.parseAppearance()

        val configureRequest = ConfigureRequest(initializationMode, configuration)
        val canSkip = viewModel.previousConfigureRequest == configureRequest

        if (canSkip) {
            onConfigured()
            return
        }

        viewModel.resetSession()

        paymentElementLoader.load(
            initializationMode = initializationMode,
            integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(configuration),
            metadata = PaymentElementLoader.Metadata(
                isReloadingAfterProcessDeath = false,
                initializedViaCompose = initializedViaCompose,
            )
        ).fold(
            onSuccess = { state ->
                if (state.validationError != null) {
                    onConfigured(state.validationError)
                } else {
                    viewModel.previousConfigureRequest = configureRequest
                    onInitSuccess(PaymentSheetState.Full(state), configuration)
                    onConfigured()
                }
            },
            onFailure = { error ->
                onConfigured(error = error)
            }
        )
    }

    private suspend fun onInitSuccess(
        state: PaymentSheetState.Full,
        configuration: PaymentSheet.Configuration,
    ) {
        viewModel.paymentSelection = paymentSelectionUpdater(
            selection = viewModel.paymentSelection,
            previousConfig = viewModel.state?.config,
            newState = state,
            newConfig = configuration,
            walletButtonsAlreadyShown = viewModel.walletButtonsRendered,
        )

        withContext(uiContext) {
            viewModel.state = DefaultFlowController.State(paymentSheetState = state, config = configuration)
        }
        confirmationHandler.bootstrap(state.paymentMethodMetadata)
    }

    private fun resetJob() {
        job.set(null)
    }

    data class ConfigureRequest(
        val initializationMode: PaymentElementLoader.InitializationMode,
        val configuration: PaymentSheet.Configuration,
    )
}
