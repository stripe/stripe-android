package com.stripe.android.common.analytics.experiment

import com.stripe.android.common.analytics.experiment.LoggableExperiment.LinkGlobalHoldback
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.LinkDisabledApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.RetrieveCustomerEmail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal interface LogLinkGlobalHoldbackExposure {
    /**
     * Logs the exposure of the Link Global Holdback experiment.
     *
     * @param elementsSession The session containing the experiment data.
     * @param state The current state of the payment element loader.
     */
    operator fun invoke(
        elementsSession: ElementsSession,
        state: PaymentElementLoader.State
    )
}

internal class DefaultLogLinkGlobalHoldbackExposure @Inject constructor(
    private val eventReporter: EventReporter,
    @LinkDisabledApiRepository private val linkDisabledApiRepository: LinkRepository,
    @IOContext private val workContext: CoroutineContext,
    private val retrieveCustomerEmail: RetrieveCustomerEmail,
    private val logger: Logger
) : LogLinkGlobalHoldbackExposure {

    override operator fun invoke(
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
        val experimentGroup: String = elementsSession.experimentsData
            ?.experimentAssignments[LINK_GLOBAL_HOLD_BACK] ?: "control"

        val customerEmail = state.getEmail()

        val isReturningUser: Boolean = customerEmail != null && isReturningUser(customerEmail)

        logger.debug(
            """Link Global Holdback exposure: 
                |arbId=${experimentsData.arbId},
                |isReturningLinkConsumer=$isReturningUser,
                |group=$experimentGroup
            """.trimMargin()
        )
        eventReporter.onExperimentExposure(
            experiment = LinkGlobalHoldback(
                arbId = experimentsData.arbId,
                isReturningLinkConsumer = isReturningUser,
                group = experimentGroup,
            ),
        )
    }

    suspend fun isReturningUser(
        email: String,
    ): Boolean {
        return linkDisabledApiRepository
            .lookupConsumerWithoutBackendLoggingForExposure(email)
            .map { it.exists }
            .getOrElse {
                logger.error("Failed to check if user is returning", it)
                false
            }
    }

    private suspend fun PaymentElementLoader.State.getEmail(): String? =
        paymentMethodMetadata.linkState?.configuration?.customerInfo?.email ?: retrieveCustomerEmail(
            configuration = config,
            customer = customer?.let {
                CustomerRepository.CustomerInfo(
                    id = it.id,
                    ephemeralKeySecret = it.ephemeralKeySecret,
                    customerSessionClientSecret = it.customerSessionClientSecret
                )
            }
        )
}
