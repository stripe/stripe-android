package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.DaggerLinkViewModelFactoryComponent
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.link.ui.verification.VerificationViewModel
import com.stripe.android.link.ui.wallet.WalletViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that coordinates the user flow through the screens.
 */
internal class LinkActivityViewModel @Inject internal constructor(
    args: LinkActivityContract.Args,
    private val linkAccountManager: LinkAccountManager,
    val navigator: Navigator
) : ViewModel() {
    /**
     * This ViewModel exists during the whole user flow, and needs to share the Dagger dependencies
     * with the other, screen-specific ViewModels. So it holds a reference to the injector which is
     * passed as a parameter to the other ViewModel factories.
     */
    lateinit var injector: NonFallbackInjector

    val startDestination = args.customerEmail?.let {
        LinkScreen.Loading
    } ?: LinkScreen.SignUp

    val linkAccount = linkAccountManager.linkAccount

    init {
        if (startDestination == LinkScreen.Loading) {
            // Loading screen is shown only when customer email is not null
            val consumerEmail = requireNotNull(args.customerEmail)
            viewModelScope.launch {
                navigator.navigateTo(
                    linkAccountManager.lookupConsumer(consumerEmail).fold(
                        onSuccess = {
                            it?.let { linkAccount ->
                                if (linkAccount.isVerified) {
                                    LinkScreen.Wallet
                                } else {
                                    LinkScreen.Verification
                                }
                            } ?: LinkScreen.SignUp
                        },
                        onFailure = {
                            LinkScreen.SignUp
                        }
                    )
                )
            }
        }
    }

    internal class Factory(
        private val application: Application,
        private val starterArgsSupplier: () -> LinkActivityContract.Args
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
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

        private lateinit var injector: NonFallbackInjector

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val logger = Logger.getInstance(BuildConfig.DEBUG)
            val starterArgs = starterArgsSupplier()

            starterArgs.injectionParams?.injectorKey?.let {
                WeakMapInjectorRegistry.retrieve(it)
            }?.let { it as? NonFallbackInjector }?.let {
                logger.info(
                    "Injector available, " +
                        "injecting dependencies into ${this::class.java.canonicalName}"
                )
                injector = it
                it.inject(this)
            } ?: run {
                logger.info(
                    "Injector unavailable, " +
                        "initializing dependencies of ${this::class.java.canonicalName}"
                )
                fallbackInitialize(
                    FallbackInitializeParam(
                        application,
                        starterArgs,
                        starterArgs.injectionParams?.enableLogging ?: false,
                        starterArgs.injectionParams?.publishableKey
                            ?: PaymentConfiguration.getInstance(application).publishableKey,
                        if (starterArgs.injectionParams != null) {
                            starterArgs.injectionParams.stripeAccountId
                        } else {
                            PaymentConfiguration.getInstance(application).stripeAccountId
                        },
                        starterArgs.injectionParams?.productUsage ?: emptySet()
                    )
                )
            }

            viewModel.injector = injector
            return viewModel as T
        }

        /**
         * This ViewModel is the first one that is created, and if the process was killed it will
         * recreate the Dagger dependency graph. Because we want to share those dependencies, it is
         * responsible for injecting them not only in itself, but also in the other ViewModel
         * factories of the module.
         */
        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            val viewModelComponent = DaggerLinkViewModelFactoryComponent.builder()
                .context(arg.application)
                .enableLogging(arg.enableLogging)
                .publishableKeyProvider { arg.publishableKey }
                .stripeAccountIdProvider { arg.stripeAccountId }
                .productUsage(arg.productUsage)
                .starterArgs(arg.starterArgs)
                .build()

            injector = object : NonFallbackInjector {
                override fun inject(injectable: Injectable<*>) {
                    when (injectable) {
                        is Factory -> viewModelComponent.inject(injectable)
                        is SignUpViewModel.Factory -> viewModelComponent.inject(injectable)
                        is VerificationViewModel.Factory -> viewModelComponent.inject(injectable)
                        is WalletViewModel.Factory -> viewModelComponent.inject(injectable)
                        else -> {
                            throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
                        }
                    }
                }
            }

            viewModelComponent.inject(this)
        }
    }
}
