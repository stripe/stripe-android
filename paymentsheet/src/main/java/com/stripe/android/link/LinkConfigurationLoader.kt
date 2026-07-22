package com.stripe.android.link

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.Logger
import com.stripe.android.link.exceptions.LinkUnavailableException
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkMetadata
import com.stripe.android.networking.RequestSurface
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

internal interface LinkConfigurationLoader {
    suspend fun load(configuration: LinkController.Configuration.State): Result<LinkMetadata>
}

internal class DefaultLinkConfigurationLoader @Inject constructor(
    private val logger: Logger,
    private val paymentElementLoader: PaymentElementLoader,
    private val linkGateFactory: LinkGate.Factory,
    private val savedStateHandle: SavedStateHandle,
    private val requestSurface: RequestSurface,
) : LinkConfigurationLoader {
    private val tag = "LinkConfigurationLoader"

    override suspend fun load(configuration: LinkController.Configuration.State): Result<LinkMetadata> {
        val (initializationMode, integrationConfiguration) = when (requestSurface) {
            RequestSurface.CryptoOnramp -> Pair(
                PaymentElementLoader.InitializationMode.CryptoOnramp(
                    paymentMethodTypes = configuration.paymentMethodTypes,
                ),
                PaymentElementLoader.Configuration.CryptoOnramp(configuration),
            )
            else -> Pair(
                PaymentElementLoader.InitializationMode.StandaloneLink(
                    paymentMethodTypes = configuration.paymentMethodTypes,
                ),
                PaymentElementLoader.Configuration.StandaloneLink(configuration),
            )
        }
        return paymentElementLoader.load(
            initializationMode = initializationMode,
            integrationConfiguration = integrationConfiguration,
            metadata = PaymentElementLoader.Metadata(
                isReloadingAfterProcessDeath = savedStateHandle.contains(
                    LinkControllerInteractor.LINK_CONFIGURED_KEY
                ),
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
                LinkMetadata(
                    linkConfiguration = config,
                    paymentMethodMetadata = state.paymentMethodMetadata,
                )
            } catch (e: Throwable) {
                throw LinkUnavailableException(e)
            }
        }.onFailure { error ->
            logger.error("$tag: Failed to load LinkConfiguration", error)
        }
    }
}
