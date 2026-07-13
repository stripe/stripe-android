package com.stripe.android.paymentelement.embedded.content

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.coroutines.CoalescingOrchestrator
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Provider

internal interface EmbeddedConfigurationHandler {
    suspend fun configure(
        configuration: EmbeddedPaymentElement.Configuration,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): Result<PaymentElementLoader.State>
}

internal class DefaultEmbeddedConfigurationHandler @Inject constructor(
    private val paymentElementLoader: PaymentElementLoader,
    private val savedStateHandle: SavedStateHandle,
    private val sheetStateHolder: SheetStateHolder,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
    private val selectionResolver: EmbeddedSelectionChooserResolver,
    private val selectionHolder: EmbeddedSelectionHolder,
) : EmbeddedConfigurationHandler {

    private var cache: ConfigurationCache?
        get() {
            return savedStateHandle[ConfigurationCache.KEY]
        }
        set(value) {
            savedStateHandle[ConfigurationCache.KEY] = value
        }

    @Volatile
    private var inFlightRequest: InFlightRequest? = null

    override suspend fun configure(
        configuration: EmbeddedPaymentElement.Configuration,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): Result<PaymentElementLoader.State> {
        // The cached state is selection-independent (the loader returns its raw computed
        // selection); selection resolution runs here on every configure — cache hit or fresh load
        // — against the current selection, so a reconfigure that only changes the selection still
        // resolves correctly.
        return loadRawState(configuration, initializationMode).map { rawState ->
            rawState.copy(
                paymentSelection = selectionResolver.resolve(
                    state = rawState,
                    integrationConfiguration = PaymentElementLoader.Configuration.Embedded(
                        isRowSelectionImmediateAction = internalRowSelectionCallback.get() != null,
                        configuration = configuration,
                    ),
                    reconfigureContext = PaymentElementLoader.ReconfigureContext(
                        previousSelection = selectionHolder.selection.value,
                    ),
                ),
            )
        }
    }

    private suspend fun loadRawState(
        configuration: EmbeddedPaymentElement.Configuration,
        initializationMode: PaymentElementLoader.InitializationMode,
    ): Result<PaymentElementLoader.State> {
        val targetConfiguration = configuration.asCommonConfiguration()

        val arguments = Arguments(
            initializationMode = initializationMode,
            configuration = targetConfiguration,
        )

        cache?.let { cache ->
            if (cache.arguments == arguments) {
                return Result.success(cache.resultState)
            }
        }
        cache = null

        inFlightRequest?.let { inFlightRequest ->
            if (inFlightRequest.arguments == arguments) {
                return inFlightRequest.result()
            } else {
                // Cancel the previous request since they have different arguments.
                inFlightRequest.cancellationHandle()
            }
        }
        inFlightRequest = null

        if (sheetStateHolder.sheetIsOpen) {
            return Result.failure(IllegalStateException("Configuring while a sheet is open is not supported."))
        }

        val supervisorJob = SupervisorJob()
        val coroutineScope = CoroutineScope(supervisorJob)

        val coalescingOrchestrator = CoalescingOrchestrator<Result<PaymentElementLoader.State>>(
            factory = {
                coroutineScope.async {
                    paymentElementLoader.load(
                        initializationMode = initializationMode,
                        integrationConfiguration = PaymentElementLoader.Configuration.Embedded(
                            isRowSelectionImmediateAction = internalRowSelectionCallback.get() != null,
                            configuration = configuration,
                        ),
                        metadata = PaymentElementLoader.Metadata(
                            isReloadingAfterProcessDeath = false,
                            initializedViaCompose = true,
                        ),
                    ).onSuccess { state ->
                        cache = ConfigurationCache(
                            arguments = Arguments(
                                initializationMode = initializationMode,
                                configuration = targetConfiguration,
                            ),
                            resultState = state,
                        )
                    }.also {
                        inFlightRequest = null
                    }
                }.await()
            },
        )

        inFlightRequest = InFlightRequest(
            arguments = arguments,
            result = coalescingOrchestrator::get,
            cancellationHandle = { supervisorJob.cancel() }
        )

        return coalescingOrchestrator.get()
    }

    @Parcelize
    data class Arguments(
        val initializationMode: PaymentElementLoader.InitializationMode,
        val configuration: CommonConfiguration,
    ) : Parcelable

    @Parcelize
    data class ConfigurationCache(
        val arguments: Arguments,
        val resultState: PaymentElementLoader.State,
    ) : Parcelable {
        companion object {
            const val KEY = "ConfigurationCache"
        }
    }

    private data class InFlightRequest(
        val arguments: Arguments,
        val result: suspend () -> Result<PaymentElementLoader.State>,
        val cancellationHandle: () -> Unit,
    )
}
