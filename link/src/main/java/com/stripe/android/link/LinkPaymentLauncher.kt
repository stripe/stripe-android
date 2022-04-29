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
import com.stripe.android.link.injection.CUSTOMER_EMAIL
import com.stripe.android.link.injection.DaggerLinkPaymentLauncherComponent
import com.stripe.android.link.injection.LinkPaymentLauncherComponent
import com.stripe.android.link.injection.MERCHANT_NAME
import com.stripe.android.link.injection.NonFallbackInjectable
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.InlineSignupViewModel
import com.stripe.android.link.ui.paymentmethod.FormViewModel
import com.stripe.android.link.ui.paymentmethod.PaymentMethodViewModel
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.link.ui.wallet.WalletViewModel
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * Launcher for an Activity that will confirm a payment using Link.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkPaymentLauncher @AssistedInject internal constructor(
    @Assisted(MERCHANT_NAME) private val merchantName: String,
    @Assisted(CUSTOMER_EMAIL) private val customerEmail: String?,
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
    resourceRepository: ResourceRepository
) : NonFallbackInjectable {
    private var args: LinkActivityContract.Args? = null
    private val launcherComponentBuilder = DaggerLinkPaymentLauncherComponent.builder()
        .merchantName(merchantName)
        .customerEmail(customerEmail)
        .context(context)
        .ioContext(ioContext)
        .uiContext(uiContext)
        .analyticsRequestFactory(paymentAnalyticsRequestFactory)
        .analyticsRequestExecutor(analyticsRequestExecutor)
        .stripeRepository(stripeRepository)
        .resourceRepository(resourceRepository)
        .enableLogging(enableLogging)
        .publishableKeyProvider(publishableKeyProvider)
        .stripeAccountIdProvider(stripeAccountIdProvider)
        .productUsage(productUsage)

    @InjectorKey
    private val injectorKey: String = WeakMapInjectorRegistry.nextKey(
        requireNotNull(LinkPaymentLauncher::class.simpleName)
    )

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

    /**
     * Publicly visible account status, used by PaymentSheet to display the correct UI.
     */
    lateinit var accountStatus: Flow<AccountStatus>

    /**
     * Sets up Link to process the given [StripeIntent].
     *
     * This will fetch the user's account if they're already logged in, or lookup the email passed
     * in during instantiation.
     */
    suspend fun setup(stripeIntent: StripeIntent): AccountStatus {
        val component = setupDependencies(stripeIntent)
        accountStatus = component.linkAccountManager.accountStatus
        linkAccountManager = component.linkAccountManager
        return accountStatus.first()
    }

    fun present(
        activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args>
    ) {
        requireNotNull(args) { "Must call setup before presenting" }
        activityResultLauncher.launch(args)
    }

    private fun setupDependencies(stripeIntent: StripeIntent): LinkPaymentLauncherComponent {
        val args = LinkActivityContract.Args(
            stripeIntent,
            merchantName,
            customerEmail,
            LinkActivityContract.Args.InjectionParams(
                injectorKey,
                productUsage,
                enableLogging,
                publishableKeyProvider(),
                stripeAccountIdProvider()
            )
        )

        val component = launcherComponentBuilder
            .starterArgs(args)
            .build()

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                when (injectable) {
                    is LinkActivityViewModel.Factory -> component.inject(injectable)
                    is SignUpViewModel.Factory -> component.inject(injectable)
                    is VerificationViewModel.Factory -> component.inject(injectable)
                    is WalletViewModel.Factory -> component.inject(injectable)
                    is InlineSignupViewModel.Factory -> component.inject(injectable)
                    is PaymentMethodViewModel.Factory -> component.inject(injectable)
                    is FormViewModel.Factory -> component.inject(injectable)
                    else -> {
                        throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
                    }
                }
            }
        }

        WeakMapInjectorRegistry.register(injector, injectorKey)
        this.args = args
        this.injector = injector
        return component
    }
}
