package com.stripe.android.link

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.Logger
import com.stripe.android.link.exceptions.LinkUnavailableException
import com.stripe.android.link.exceptions.MissingAppAttestationException
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkMetadata
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.networking.RequestSurface
import com.stripe.android.paymentsheet.state.LinkDisabledState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.LinkStateResult
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
            val linkStateResult = state.paymentMethodMetadata.linkStateResult
            // Check this first so that we don't misreport Link being unavailable for the merchant (e.g. because
            // it's turned off in their payment method settings) as an attestation issue.
            if (linkStateResult.isNotSupportedInElementsSession()) {
                throw LinkUnavailableException()
            }
            // Any other reason for Link being disabled stems from the device not being able to use native Link.
            val config = (linkStateResult as? LinkState)?.configuration
            if (config == null || !linkGateFactory.create(config).useNativeLink) {
                throw MissingAppAttestationException()
            }
            LinkMetadata(
                linkConfiguration = config,
                paymentMethodMetadata = state.paymentMethodMetadata,
            )
        }.onFailure { error ->
            logger.error("$tag: Failed to load LinkConfiguration", error)
        }
    }

    private fun LinkStateResult?.isNotSupportedInElementsSession(): Boolean {
        return when (this) {
            is LinkDisabledState -> LinkDisabledReason.NotSupportedInElementsSession in linkDisabledReasons
            is LinkState -> false
            null -> true
        }
    }
}
