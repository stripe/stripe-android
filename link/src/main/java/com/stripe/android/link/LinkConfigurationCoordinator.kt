package com.stripe.android.link

import androidx.annotation.RestrictTo
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface LinkConfigurationCoordinator {

    val component: LinkComponent?
    val emailFlow: Flow<String?>

    fun getAccountStatusFlow(configuration: LinkConfiguration): Flow<AccountStatus>
    suspend fun signInWithUserInput(
        configuration: LinkConfiguration,
        userInput: UserInput
    ): Result<Boolean>
    suspend fun attachNewCardToAccount(
        configuration: LinkConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails>
}

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class RealLinkConfigurationCoordinator @Inject internal constructor(
    private val linkComponentBuilder: LinkComponent.Builder,
) : LinkConfigurationCoordinator {
    private val componentFlow = MutableStateFlow<LinkComponent?>(null)

    override val emailFlow: Flow<String?> = componentFlow
        .filterNotNull()
        .flatMapMerge { it.linkAccountManager.linkAccount }
        .map { it?.email }

    /**
     * The dependency injector Component for all injectable classes in Link while in an embedded
     * environment.
     */
    override val component: LinkComponent?
        get() = componentFlow.value

    /**
     * Fetch the customer's account status, initializing the dependencies if they haven't been
     * initialized yet.
     */
    override fun getAccountStatusFlow(configuration: LinkConfiguration): Flow<AccountStatus> =
        getLinkPaymentLauncherComponent(configuration).linkAccountManager.accountStatus

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

    /**
     * Create or get the existing [LinkComponent], responsible for injecting all
     * injectable classes in Link while in an embedded environment.
     */
    private fun getLinkPaymentLauncherComponent(configuration: LinkConfiguration) =
        component?.takeIf { it.configuration == configuration }
            ?: linkComponentBuilder
                .configuration(configuration)
                .build()
                .also {
                    componentFlow.value = it
                }
}
