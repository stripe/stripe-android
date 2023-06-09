package com.stripe.android.link

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.NonFallbackInjectable
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.link.injection.DaggerLinkPaymentLauncherComponent
import com.stripe.android.link.injection.LinkPaymentLauncherComponent
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.uicore.address.AddressRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkConfigurationCoordinator @Inject internal constructor(
    context: Context,
    @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @IOContext ioContext: CoroutineContext,
    @UIContext uiContext: CoroutineContext,
    paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    analyticsRequestExecutor: AnalyticsRequestExecutor,
    stripeRepository: StripeRepository,
    addressRepository: AddressRepository,
) : NonFallbackInjectable {
    private val launcherComponentBuilder = DaggerLinkPaymentLauncherComponent.builder()
        .context(context)
        .ioContext(ioContext)
        .uiContext(uiContext)
        .analyticsRequestFactory(paymentAnalyticsRequestFactory)
        .analyticsRequestExecutor(analyticsRequestExecutor)
        .stripeRepository(stripeRepository)
        .addressRepository(addressRepository)
        .enableLogging(enableLogging)
        .publishableKeyProvider(publishableKeyProvider)
        .stripeAccountIdProvider(stripeAccountIdProvider)
        .productUsage(productUsage)

    private val componentFlow = MutableStateFlow<LinkPaymentLauncherComponent?>(null)

    @OptIn(FlowPreview::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val emailFlow: Flow<String?> = componentFlow
        .filterNotNull()
        .flatMapMerge { it.linkAccountManager.linkAccount }
        .map { it?.email }

    /**
     * The dependency injector Component for all injectable classes in Link while in an embedded
     * environment.
     */
    internal val component: LinkPaymentLauncherComponent?
        get() = componentFlow.value

    /**
     * Fetch the customer's account status, initializing the dependencies if they haven't been
     * initialized yet.
     */
    fun getAccountStatusFlow(configuration: LinkConfiguration) =
        getLinkPaymentLauncherComponent(configuration).linkAccountManager.accountStatus

    /**
     * Trigger Link sign in with the input collected from the user inline in PaymentSheet, whether
     * it's a new or existing account.
     */
    suspend fun signInWithUserInput(
        configuration: LinkConfiguration,
        userInput: UserInput
    ) = getLinkPaymentLauncherComponent(configuration)
        .linkAccountManager
        .signInWithUserInput(userInput)
        .map { true }

    /**
     * Attach a new Card to the currently signed in Link account.
     *
     * @return The parameters needed to confirm the current Stripe Intent using the newly created
     *          PaymentDetails.
     */
    suspend fun attachNewCardToAccount(
        configuration: LinkConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails.New> =
        getLinkPaymentLauncherComponent(configuration)
            .linkAccountManager
            .createCardPaymentDetails(paymentMethodCreateParams)

    /**
     * Create or get the existing [LinkPaymentLauncherComponent], responsible for injecting all
     * injectable classes in Link while in an embedded environment.
     */
    private fun getLinkPaymentLauncherComponent(configuration: LinkConfiguration) =
        component?.takeIf { it.configuration == configuration }
            ?: launcherComponentBuilder
                .configuration(configuration)
                .build()
                .also {
                    componentFlow.value = it
                }
}
