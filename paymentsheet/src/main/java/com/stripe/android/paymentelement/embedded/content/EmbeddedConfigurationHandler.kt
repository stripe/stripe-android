@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.coroutines.CoalescingOrchestrator
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

internal interface EmbeddedConfigurationHandler {
    suspend fun configure(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: EmbeddedPaymentElement.Configuration,
    ): Result<PaymentElementLoader.State>
}

internal class DefaultEmbeddedConfigurationHandler @Inject constructor(
    private val paymentElementLoader: PaymentElementLoader,
    private val savedStateHandle: SavedStateHandle,
    private val sheetStateHolder: SheetStateHolder,
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
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: EmbeddedPaymentElement.Configuration,
    ): Result<PaymentElementLoader.State> {
        if (sheetStateHolder.sheetIsOpen) {
            return Result.failure(IllegalStateException("Configuring while a sheet is open is not supported."))
        }

        val targetConfiguration = configuration.asCommonConfiguration()

        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration)
        try {
            initializationMode.validate()
            targetConfiguration.validate()
        } catch (e: IllegalArgumentException) {
            return Result.failure(e)
        }

        val arguments = Arguments(
            intentConfiguration = intentConfiguration,
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

        val supervisorJob = SupervisorJob()
        val coroutineScope = CoroutineScope(supervisorJob)

        val coalescingOrchestrator = CoalescingOrchestrator<Result<PaymentElementLoader.State>>(
            factory = {
                coroutineScope.async {
                    paymentElementLoader.load(
                        initializationMode = initializationMode,
                        configuration = targetConfiguration,
                        isReloadingAfterProcessDeath = false,
                        initializedViaCompose = true,
                    ).onSuccess { state ->
                        cache = ConfigurationCache(
                            arguments = Arguments(
                                intentConfiguration = intentConfiguration,
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
        val intentConfiguration: PaymentSheet.IntentConfiguration,
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
