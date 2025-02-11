package com.stripe.android.link

import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal interface LinkConfigurationCoordinator {
    val emailFlow: StateFlow<String?>

    fun getComponent(configuration: LinkConfiguration): LinkComponent

    fun getAccountStatusFlow(configuration: LinkConfiguration): Flow<AccountStatus>

    fun linkGate(configuration: LinkConfiguration): LinkGate

    suspend fun signInWithUserInput(
        configuration: LinkConfiguration,
        userInput: UserInput
    ): Result<Boolean>

    suspend fun attachNewCardToAccount(
        configuration: LinkConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails>

    suspend fun logOut(
        configuration: LinkConfiguration,
    ): Result<ConsumerSession>
}

@Singleton
internal class RealLinkConfigurationCoordinator @Inject internal constructor(
    private val linkComponentBuilder: LinkComponent.Builder,
) : LinkConfigurationCoordinator {
    private val componentFlow = MutableStateFlow<LinkComponent?>(null)

    override val emailFlow: StateFlow<String?> = componentFlow
        .flatMapLatestAsStateFlow { it?.linkAccountManager?.linkAccount ?: stateFlowOf(null) }
        .mapAsStateFlow { it?.email }

    /**
     * Fetch the dependency injector Component for all injectable classes in Link while in an embedded
     * environment.
     */
    override fun getComponent(configuration: LinkConfiguration): LinkComponent {
        return getLinkPaymentLauncherComponent(configuration)
    }

    /**
     * Fetch the customer's account status, initializing the dependencies if they haven't been
     * initialized yet.
     */
    override fun getAccountStatusFlow(configuration: LinkConfiguration): Flow<AccountStatus> =
        getLinkPaymentLauncherComponent(configuration).linkAccountManager.accountStatus

    override fun linkGate(configuration: LinkConfiguration): LinkGate {
        return getLinkPaymentLauncherComponent(configuration).linkGate
    }

    /**
     * Trigger Link sign in with the input collected from the user inline in PaymentSheet, whether
     * it's a new or existing account.
     */
    override suspend fun signInWithUserInput(
        configuration: LinkConfiguration,
        userInput: UserInput
    ): Result<Boolean> = getLinkPaymentLauncherComponent(configuration)
        .linkAccountManager
        .signInWithUserInput(userInput)
        .map { true }

    /**
     * Attach a new Card to the currently signed in Link account.
     *
     * @return The parameters needed to confirm the current Stripe Intent using the newly created
     *          PaymentDetails.
     */
    override suspend fun attachNewCardToAccount(
        configuration: LinkConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails> =
        getLinkPaymentLauncherComponent(configuration)
            .linkAccountManager
            .createCardPaymentDetails(paymentMethodCreateParams)

    override suspend fun logOut(
        configuration: LinkConfiguration,
    ): Result<ConsumerSession> {
        return getLinkPaymentLauncherComponent(configuration)
            .linkAccountManager
            .logOut()
    }

    /**
     * Create or get the existing [LinkComponent], responsible for injecting all
     * injectable classes in Link while in an embedded environment.
     */
    private fun getLinkPaymentLauncherComponent(configuration: LinkConfiguration) =
        componentFlow.value?.takeIf { it.configuration == configuration }
            ?: linkComponentBuilder
                .configuration(configuration)
                .build()
                .also {
                    componentFlow.value = it
                }
}
