package com.stripe.android.link

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.link.LinkActivityResult.Canceled.Reason.LoggedOut
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.injection.DaggerLinkViewModelFactoryComponent
import com.stripe.android.link.model.Navigator
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import javax.inject.Inject

/**
 * ViewModel that coordinates the user flow through the screens.
 */
internal class LinkActivityViewModel @Inject internal constructor(
    args: LinkActivityContract.Args,
    val linkAccountManager: LinkAccountManager,
    val navigator: Navigator,
    private val confirmationManager: ConfirmationManager
) : ViewModel() {
    /**
     * This ViewModel exists during the whole user flow, and needs to share the Dagger dependencies
     * with the other, screen-specific ViewModels. So it holds a reference to the injector which is
     * passed as a parameter to the other ViewModel factories.
     */
    lateinit var injector: NonFallbackInjector

    val linkAccount = linkAccountManager.linkAccount

    init {
        assertStripeIntentIsValid(args.stripeIntent)
    }

    fun setupPaymentLauncher(activityResultCaller: ActivityResultCaller) {
        confirmationManager.setupPaymentLauncher(activityResultCaller)
    }

    fun onBackPressed() {
        navigator.onBack(userInitiated = true)
    }

    fun logout() {
        navigator.cancel(reason = LoggedOut)
        linkAccountManager.logout()
    }

    fun unregisterFromActivity() {
        confirmationManager.invalidatePaymentLauncher()
    }

    /**
     * Assert that the [StripeIntent] has all the fields required for confirmation.
     */
    private fun assertStripeIntentIsValid(stripeIntent: StripeIntent) {
        runCatching {
            requireNotNull(stripeIntent.id)
            requireNotNull(stripeIntent.clientSecret)
            (stripeIntent as? PaymentIntent)?.let {
                requireNotNull(stripeIntent.amount)
                requireNotNull(stripeIntent.currency)
            }
        }.onFailure {
            navigator.dismiss(LinkActivityResult.Failed(it))
        }
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> LinkActivityContract.Args
    ) : ViewModelProvider.Factory,
        Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application,
            val starterArgs: LinkActivityContract.Args,
            val enableLogging: Boolean,
            val publishableKey: String,
            val stripeAccountId: String?,
            val productUsage: Set<String>
        )

        @Inject
        lateinit var viewModel: LinkActivityViewModel

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val starterArgs = starterArgsSupplier()

            val injector = injectWithFallback(
                starterArgs.injectionParams?.injectorKey,
                FallbackInitializeParam(
                    applicationSupplier(),
                    starterArgs,
                    starterArgs.injectionParams?.enableLogging ?: false,
                    starterArgs.injectionParams?.publishableKey
                        ?: PaymentConfiguration.getInstance(applicationSupplier()).publishableKey,
                    if (starterArgs.injectionParams != null) {
                        starterArgs.injectionParams.stripeAccountId
                    } else {
                        PaymentConfiguration.getInstance(applicationSupplier()).stripeAccountId
                    },
                    starterArgs.injectionParams?.productUsage ?: emptySet()
                )
            )
            viewModel.injector = requireNotNull(injector as NonFallbackInjector)
            return viewModel as T
        }

        /**
         * This ViewModel is the first one that is created, and if the process was killed it will
         * recreate the Dagger dependency graph. Because we want to share those dependencies, it is
         * responsible for injecting them not only in itself, but also in the other ViewModel
         * factories of the module.
         */
        override fun fallbackInitialize(arg: FallbackInitializeParam): Injector {
            val viewModelComponent = DaggerLinkViewModelFactoryComponent.builder()
                .context(arg.application)
                .enableLogging(arg.enableLogging)
                .publishableKeyProvider { arg.publishableKey }
                .stripeAccountIdProvider { arg.stripeAccountId }
                .productUsage(arg.productUsage)
                .starterArgs(arg.starterArgs)
                .build()
            viewModelComponent.inject(this)
            return viewModelComponent
        }
    }
}
