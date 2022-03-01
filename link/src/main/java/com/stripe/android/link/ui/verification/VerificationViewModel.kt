package com.stripe.android.link.ui.verification

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.DaggerLinkViewModelFactoryComponent
import com.stripe.android.link.injection.LinkViewModelSubcomponent
import com.stripe.android.link.model.Navigator
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * ViewModel that handles user verification confirmation logic.
 */
internal class VerificationViewModel @Inject constructor(
    private val linkAccountManager: LinkAccountManager,
    private val navigator: Navigator,
    private val logger: Logger
) : ViewModel() {

    val linkAccount by lazy {
        // The Link account is already loaded in linkAccountManager when we get to Verification
        requireNotNull(linkAccountManager.linkAccount)
    }

    fun onVerificationCodeEntered(code: String) {
        viewModelScope.launch {
            linkAccountManager.confirmVerification(code).fold(
                onSuccess = {
                    navigator.navigateTo(LinkScreen.Wallet)
                },
                onFailure = ::onError
            )
        }
    }

    fun onResendCodeClicked() {
        viewModelScope.launch {
            linkAccountManager.startVerification().fold(
                onSuccess = {
                    logger.info("Verification code sent")
                },
                onFailure = ::onError
            )
        }
    }

    private fun onError(error: Throwable) {
        logger.error(error.localizedMessage ?: "Internal error.")
        // TODO(brnunes-stripe): Add localized error messages, show them in UI.
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
                .build().verificationViewModel as T
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
