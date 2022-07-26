package com.stripe.android.link

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.CUSTOMER_EMAIL
import com.stripe.android.link.injection.CUSTOMER_PHONE
import com.stripe.android.link.injection.DaggerLinkPaymentLauncherComponent
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.injection.LinkPaymentLauncherComponent
import com.stripe.android.link.injection.MERCHANT_NAME
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.cardedit.CardEditViewModel
import com.stripe.android.link.ui.inline.InlineSignupViewModel
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.link.ui.paymentmethod.PaymentMethodViewModel
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.link.ui.wallet.WalletViewModel
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.ui.core.injection.NonFallbackInjector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * Launcher for an Activity that will confirm a payment using Link.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkPaymentLauncher @AssistedInject internal constructor(
    @Assisted(MERCHANT_NAME) private val merchantName: String,
    @Assisted(CUSTOMER_EMAIL) private val customerEmail: String?,
    @Assisted(CUSTOMER_PHONE) private val customerPhone: String?,
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
    lpmResourceRepository: ResourceRepository<LpmRepository>,
    addressResourceRepository: ResourceRepository<AddressRepository>
) : NonFallbackInjectable {
    private var stripeIntent: StripeIntent? = null
    private val launcherComponentBuilder = DaggerLinkPaymentLauncherComponent.builder()
        .merchantName(merchantName)
        .customerEmail(customerEmail)
        .customerPhone(customerPhone)
        .context(context)
        .ioContext(ioContext)
        .uiContext(uiContext)
        .analyticsRequestFactory(paymentAnalyticsRequestFactory)
        .analyticsRequestExecutor(analyticsRequestExecutor)
        .stripeRepository(stripeRepository)
        .lpmResourceRepository(lpmResourceRepository)
        .addressResourceRepository(addressResourceRepository)
        .enableLogging(enableLogging)
        .publishableKeyProvider(publishableKeyProvider)
        .stripeAccountIdProvider(stripeAccountIdProvider)
        .productUsage(productUsage)

    @InjectorKey
    private val injectorKey: String = WeakMapInjectorRegistry.nextKey(
        requireNotNull(LinkPaymentLauncher::class.simpleName)
    )

    private lateinit var linkComponentBuilder: LinkComponent.Builder

    /**
     * The dependency injector for all injectable classes in Link.
     * This is safe to hold here because [LinkPaymentLauncher] lives only for as long as
     * PaymentSheet's ViewModel is alive.
     */
    internal var injector: NonFallbackInjector? = null

    /**
     * The [LinkAccountManager], exposed here so that classes that are not injected (like LinkButton
     * or LinkInlineSignup) can access it and share the account status with all other components.
     */
    internal lateinit var linkAccountManager: LinkAccountManager

    internal lateinit var linkEventsReporter: LinkEventsReporter

    /**
     * Publicly visible account status, used by PaymentSheet to display the correct UI.
     */
    lateinit var accountStatus: StateFlow<AccountStatus>

    /**
     * Sets up Link to process the given [StripeIntent].
     *
     * This will fetch the user's account if they're already logged in, or lookup the email passed
     * in during instantiation.
     *
     * @param stripeIntent the PaymentIntent or SetupIntent.
     * @param coroutineScope the coroutine scope used to collect the account status flow.
     */
    suspend fun setup(
        stripeIntent: StripeIntent,
        coroutineScope: CoroutineScope
    ): AccountStatus {
        val component = buildLinkPaymentLauncherComponent(stripeIntent)
        accountStatus = component.linkAccountManager.accountStatus.stateIn(coroutineScope)
        linkAccountManager = component.linkAccountManager
        linkEventsReporter = component.linkEventsReporter
        linkComponentBuilder = component.linkComponentBuilder
        this.stripeIntent = stripeIntent
        return accountStatus.value
    }

    /**
     * Launch the Link UI to process the Stripe Intent sent in [setup].
     *
     * @param prefilledNewCardParams The card information prefilled by the user. If non null, Link
     *  will launch into adding a new card, with the card information pre-filled.
     */
    fun present(
        activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args>,
        prefilledNewCardParams: PaymentMethodCreateParams? = null
    ) {
        val stripeIntent = requireNotNull(stripeIntent) { "Must call setup before presenting" }

        val args = LinkActivityContract.Args(
            stripeIntent,
            merchantName,
            customerEmail,
            customerPhone,
            null,
            LinkActivityContract.Args.InjectionParams(
                injectorKey,
                productUsage,
                enableLogging,
                publishableKeyProvider(),
                stripeAccountIdProvider()
            )
        )
        buildLinkComponent(args)
        activityResultLauncher.launch(args)
    }

    /**
     * Trigger Link sign in with the input collected from the user, whether it's a new or existing
     * account.
     */
    suspend fun signInWithUserInput(userInput: UserInput) =
        linkAccountManager.signInWithUserInput(userInput).map { true }

    /**
     * Attach a new Card to the currently signed in Link account.
     *
     * @return The parameters needed to confirm the current Stripe Intent using the newly created
     *          PaymentDetails.
     */
    suspend fun attachNewCardToAccount(
        paymentMethodCreateParams: PaymentMethodCreateParams
    ): Result<LinkPaymentDetails.New> =
        linkAccountManager.createPaymentDetails(
            SupportedPaymentMethod.Card,
            paymentMethodCreateParams
        )

    /**
     * Set up [LinkPaymentLauncherComponent], responsible for injecting into classes used by inline
     * sign up.
     */
    private fun buildLinkPaymentLauncherComponent(
        stripeIntent: StripeIntent
    ): LinkPaymentLauncherComponent {
        val component = launcherComponentBuilder
            .stripeIntent(stripeIntent)
            .build()

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                when (injectable) {
                    is VerificationViewModel.Factory -> component.inject(injectable)
                    is InlineSignupViewModel.Factory -> component.inject(injectable)
                    else -> {
                        throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
                    }
                }
            }
        }

        this.injector = injector
        return component
    }

    /**
     * Set up [LinkComponent], responsible for injecting all dependencies into the Link app.
     */
    private fun buildLinkComponent(args: LinkActivityContract.Args) {
        val linkComponent = linkComponentBuilder.starterArgs(args).build()
        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                when (injectable) {
                    is LinkActivityViewModel.Factory -> linkComponent.inject(injectable)
                    is SignUpViewModel.Factory -> linkComponent.inject(injectable)
                    is VerificationViewModel.Factory -> linkComponent.inject(injectable)
                    is WalletViewModel.Factory -> linkComponent.inject(injectable)
                    is PaymentMethodViewModel.Factory -> linkComponent.inject(injectable)
                    is CardEditViewModel.Factory -> linkComponent.inject(injectable)
                    else -> {
                        throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
                    }
                }
            }
        }
        WeakMapInjectorRegistry.register(injector, injectorKey)
    }

    companion object {
        const val LINK_ENABLED = false
    }
}
