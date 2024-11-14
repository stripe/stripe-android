package com.stripe.android.paymentelement.embedded

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
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

    override suspend fun configure(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: EmbeddedPaymentElement.Configuration,
    ): Result<PaymentElementLoader.State> {
        val targetConfiguration = configuration.asCommonConfiguration()

        cache?.let { cache ->
            if (intentConfiguration == cache.intentConfiguration && targetConfiguration == cache.configuration) {
                return Result.success(cache.resultState)
            }
        }

        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(intentConfiguration)
        try {
            initializationMode.validate()
            targetConfiguration.validate()
        } catch (e: IllegalArgumentException) {
            return Result.failure(e)
        }

        return paymentElementLoader.load(
            initializationMode = initializationMode,
            configuration = targetConfiguration,
            isReloadingAfterProcessDeath = false,
            initializedViaCompose = true,
        ).onSuccess { state ->
            cache = ConfigurationCache(
                intentConfiguration = intentConfiguration,
                configuration = targetConfiguration,
                resultState = state,
            )
        }
    }

    @Parcelize
    data class ConfigurationCache(
        val intentConfiguration: PaymentSheet.IntentConfiguration,
        val configuration: CommonConfiguration,
        val resultState: PaymentElementLoader.State,
    ) : Parcelable {
        companion object {
            const val KEY = "ConfigurationCache"
        }
    }
}
