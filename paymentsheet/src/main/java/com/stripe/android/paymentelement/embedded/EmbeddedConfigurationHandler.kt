package com.stripe.android.paymentelement.embedded

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.coroutines.CoalescingOrchestrator
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@ExperimentalEmbeddedPaymentElementApi
internal interface EmbeddedConfigurationHandler {
    suspend fun configure(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: EmbeddedPaymentElement.Configuration,
    ): Result<PaymentElementLoader.State>
}

@ExperimentalEmbeddedPaymentElementApi
internal class DefaultEmbeddedConfigurationHandler @Inject constructor(
    private val paymentElementLoader: PaymentElementLoader,
    private val savedStateHandle: SavedStateHandle,
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
        val targetConfiguration = configuration.asCommonConfiguration()
        val arguments = Arguments(
            intentConfiguration = intentConfiguration,
            configuration = targetConfiguration,
        )

        cache?.let { cache ->
            if (cache.arguments == arguments) {
                return Result.success(cache.resultState)
            }
        }

        inFlightRequest?.let { inFlightRequest ->
            if (inFlightRequest.arguments == arguments) {
                return inFlightRequest.result()
            }
        }

        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration)
        try {
            initializationMode.validate()
            targetConfiguration.validate()
        } catch (e: IllegalArgumentException) {
            return Result.failure(e)
        }

        val coalescingOrchestrator = CoalescingOrchestrator<Result<PaymentElementLoader.State>>(
            factory = {
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
                }
            },
        )

        inFlightRequest = InFlightRequest(
            arguments = arguments,
            result = coalescingOrchestrator::get,
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
    )
}
