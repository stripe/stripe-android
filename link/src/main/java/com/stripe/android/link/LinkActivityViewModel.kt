package com.stripe.android.link

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.DaggerLinkViewModelFactoryComponent
import com.stripe.android.link.injection.LinkViewModelSubcomponent
import com.stripe.android.link.model.Navigator
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * ViewModel that coordinates the user flow through the screens.
 */
internal class LinkActivityViewModel @Inject internal constructor(
    args: LinkActivityContract.Args,
    private val linkAccountManager: LinkAccountManager,
    val navigator: Navigator
) : ViewModel() {

    val startDestination = args.customerEmail?.let {
        LinkScreen.Loading
    } ?: LinkScreen.SignUp

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
            val enableLogging: Boolean,
            val publishableKey: String,
            val stripeAccountId: String?,
            val productUsage: Set<String>
        )

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<LinkViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val args = starterArgsSupplier()
            injectWithFallback(
                args.injectionParams?.injectorKey,
                FallbackInitializeParam(
                    application,
                    args.injectionParams?.enableLogging ?: false,
                    args.injectionParams?.publishableKey
                        ?: PaymentConfiguration.getInstance(application).publishableKey,
                    if (args.injectionParams != null) {
                        args.injectionParams.stripeAccountId
                    } else {
                        PaymentConfiguration.getInstance(application).stripeAccountId
                    },
                    args.injectionParams?.productUsage ?: emptySet()
                )
            )
            return subComponentBuilderProvider.get()
                .args(args)
                .build().linkActivityViewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            DaggerLinkViewModelFactoryComponent.builder()
                .context(arg.application)
                .enableLogging(arg.enableLogging)
                .publishableKeyProvider { arg.publishableKey }
                .stripeAccountIdProvider { arg.stripeAccountId }
                .productUsage(arg.productUsage)
                .build().inject(this)
        }
    }
}
