package com.stripe.android.common.analytics.experiment

import com.stripe.android.common.analytics.experiment.ExperimentGroup.CONTROL
import com.stripe.android.common.analytics.experiment.ExperimentGroup.TREATMENT
import com.stripe.android.common.analytics.experiment.LoggableExperiment.LinkGlobalHoldback
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.LinkDisabledApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class LogLinkGlobalHoldbackExposure @Inject constructor(
    private val eventReporter: EventReporter,
    @LinkDisabledApiRepository private val linkDisabledApiRepository: LinkRepository,
    @IOContext private val workContext: CoroutineContext,
    private val customerRepository: CustomerRepository,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val logger: Logger
) {

    operator fun invoke(
        elementsSession: ElementsSession,
        state: PaymentElementLoader.State
    ) {
        if (FeatureFlags.linkGlobalHoldbackExposureEnabled.isEnabled) {
            CoroutineScope(workContext).launch {
                runCatching {
                    logExposure(elementsSession, state)
                }.onFailure { error ->
                    logger.error("Failed to log Global holdback exposure", error)
                }
            }
        }
    }

    private suspend fun logExposure(
        elementsSession: ElementsSession,
        state: PaymentElementLoader.State
    ) {
        val experimentsData = requireNotNull(
            elementsSession.experimentsData
        ) { "Experiments data required to log exposures" }
        val holdbackOn = elementsSession.linkSettings?.linkGlobalHoldbackOn == true

        val customerEmail = state.getEmail()

        val isReturningUser: Boolean = customerEmail?.let { isReturningUser(state = state, email = it) } == true

        eventReporter.onExperimentExposure(
            experiment = LinkGlobalHoldback(
                arbId = experimentsData.arbId,
                isReturningLinkConsumer = isReturningUser,
                group = if (holdbackOn) TREATMENT else CONTROL,
            ),
        )
    }

    suspend fun isReturningUser(
        state: PaymentElementLoader.State,
        email: String,
    ): Boolean {
        val linkConfiguration = state.paymentMethodMetadata.linkState?.configuration
        return if (linkConfiguration == null) {
            // Link is disabled: Make a transient lookup call uniquely for exposure logging purposes.
            linkDisabledApiRepository
                .lookupConsumerWithoutBackendLoggingForExposure(email)
                .map { it.exists }
        } else {
            // Link is enabled and available: Use existing configuration to make a lookup call and cache it for later use
            linkConfigurationCoordinator.getComponent(linkConfiguration)
                .linkAccountManager
                .lookupConsumer(email = email)
                .map { it != null }
        }.fold(
            onSuccess = { it },
            onFailure = {
                logger.error("Failed to check if user is returning", it)
                false
            }
        )
    }

    private suspend fun PaymentElementLoader.State.getEmail(): String? =
        paymentMethodMetadata.linkState?.configuration?.customerInfo?.email
            ?: config.defaultBillingDetails?.email
            ?: customer?.let {
                customerRepository.retrieveCustomer(
                    CustomerRepository.CustomerInfo(
                        id = it.id,
                        ephemeralKeySecret = it.ephemeralKeySecret,
                        customerSessionClientSecret = it.customerSessionClientSecret
                    )
                )
            }?.email
}
