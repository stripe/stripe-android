package com.stripe.android.payments.bankaccount

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.core.Logger
import com.stripe.android.payments.bankaccount.CollectBankAccountContract.Args.ForPaymentIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountContract.Args.ForSetupIntent
import javax.inject.Inject

internal class CollectBankAccountViewModel @Inject constructor(
    val args: CollectBankAccountContract.Args,
    val logger: Logger
) : ViewModel() {

    init {
        when (args) {
            is ForPaymentIntent -> {
                logger.debug("start payment intent flow")
            }
            is ForSetupIntent -> {
                logger.debug("start setup intent flow")
            }
        }
        // retrieve payment intent
        // open connections flow
        // attach result
    }

    class Factory(
        private val applicationSupplier: () -> Application,
        private val argsSupplier: () -> CollectBankAccountContract.Args,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T {
            return DaggerCollectBankAccountComponent
                .builder()
                .application(applicationSupplier())
                .configuration(argsSupplier())
                .build().viewModel as T
        }
    }
}
