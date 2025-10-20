package com.stripe.android.link

import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.link.exceptions.LinkUnavailableException
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

internal interface LinkConfigurationLoader {
    suspend fun load(configuration: LinkController.Configuration): Result<LinkConfiguration>
}

internal class DefaultLinkConfigurationLoader @Inject constructor(
    private val logger: Logger,
    private val paymentElementLoader: PaymentElementLoader,
    private val linkGateFactory: LinkGate.Factory,
) : LinkConfigurationLoader {
    private val tag = "LinkConfigurationLoader"

    override suspend fun load(configuration: LinkController.Configuration): Result<LinkConfiguration> {
        return paymentElementLoader.load(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                ),
            ),
            configuration = configuration.asCommonConfiguration(),
            metadata = PaymentElementLoader.Metadata(
                isReloadingAfterProcessDeath = false,
                initializedViaCompose = false,
            )
        ).mapCatching { state ->
            @Suppress("TooGenericExceptionCaught")
            try {
                val config = checkNotNull(state.paymentMethodMetadata.linkState?.configuration) {
                    "Link is not available"
                }
                check(linkGateFactory.create(config).useNativeLink) {
                    "Native Link is not available"
                }
                config
            } catch (e: Throwable) {
                throw LinkUnavailableException(e)
            }
        }.onFailure { error ->
            logger.error("$tag: Failed to load LinkConfiguration", error)
        }
    }
}
