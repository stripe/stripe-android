package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.injection.DaggerAddressElementViewModelFactoryComponent
import com.stripe.android.ui.core.injection.NonFallbackInjector
import javax.inject.Inject

internal class AddressElementViewModel @Inject internal constructor(
    val navigator: AddressElementNavigator
) : ViewModel() {

    lateinit var injector: NonFallbackInjector

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> AddressElementActivityContract.Args
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application,
            val starterArgs: AddressElementActivityContract.Args,
            val enableLogging: Boolean,
            val productUsage: Set<String>
        )

        @Inject
        lateinit var viewModel: AddressElementViewModel

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
                        applicationSupplier(),
                        starterArgs,
                        starterArgs.injectionParams?.enableLogging ?: false,
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
            val viewModelComponent = DaggerAddressElementViewModelFactoryComponent.builder()
                .context(arg.application)
                .enableLogging(arg.enableLogging)
                .productUsage(arg.productUsage)
                .starterArgs(arg.starterArgs)
                .build()

            injector = object : NonFallbackInjector {
                override fun inject(injectable: Injectable<*>) {
                    when (injectable) {
                        is Factory -> viewModelComponent.inject(injectable)
                        is InputAddressViewModel.Factory -> viewModelComponent.inject(injectable)
                        is AutoCompleteViewModel.Factory -> viewModelComponent.inject(injectable)
                        else -> {
                            throw IllegalArgumentException(
                                "invalid Injectable $injectable requested in $this"
                            )
                        }
                    }
                }
            }

            viewModelComponent.inject(this)
        }
    }
}
