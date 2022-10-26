package com.stripe.android.paymentsheet.addresselement

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.paymentsheet.injection.DaggerAddressElementViewModelFactoryComponent
import javax.inject.Inject

internal class AddressElementViewModel @Inject internal constructor(
    val navigator: AddressElementNavigator
) : ViewModel() {

    lateinit var injector: NonFallbackInjector

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> AddressElementActivityContract.Args
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam, NonFallbackInjector> {
        internal data class FallbackInitializeParam(
            val application: Application,
            val starterArgs: AddressElementActivityContract.Args
        )

        @Inject
        lateinit var viewModel: AddressElementViewModel

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val starterArgs = starterArgsSupplier()

            val injector = injectWithFallback(
                starterArgs.injectorKey,
                FallbackInitializeParam(applicationSupplier(), starterArgs)
            )
            viewModel.injector = injector
            return viewModel as T
        }

        /**
         * This ViewModel is the first one that is created, and if the process was killed it will
         * recreate the Dagger dependency graph. Because we want to share those dependencies, it is
         * responsible for injecting them not only in itself, but also in the other ViewModel
         * factories of the module.
         */
        override fun fallbackInitialize(arg: FallbackInitializeParam): NonFallbackInjector {
            val viewModelComponent = DaggerAddressElementViewModelFactoryComponent.builder()
                .context(arg.application)
                .starterArgs(arg.starterArgs)
                .build()
            viewModelComponent.inject(this)
            return viewModelComponent
        }
    }
}
