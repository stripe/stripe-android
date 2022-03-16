package com.stripe.android.payments.bankaccount

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.bankaccount.CollectBankAccountContract.Args.ForPaymentIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountContract.Args.ForSetupIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountViewEffect.FinishWithPaymentIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountViewEffect.FinishWithSetupIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountViewEffect.OpenConnectionsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class CollectBankAccountViewModel @Inject constructor(
    val args: CollectBankAccountContract.Args,
    val stripeRepository: StripeRepository,
    val logger: Logger
) : ViewModel() {

    private val _viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
    val viewEffect: SharedFlow<CollectBankAccountViewEffect> = _viewEffect

    private val apiRequestOptions = ApiRequest.Options(
        publishableKeyProvider = { args.publishableKey },
        stripeAccountIdProvider = { null }, // provide account id?
    )

    init {
        logger.debug(args.params.toString())
        viewModelScope.launch {
            val linkedSessionId = when (args) {
                // TODO stripeRepository.createLinkAccountSessionForPaymentIntent
                is ForPaymentIntent -> "testSessionId"
                // TODO stripeRepository.createLinkAccountSessionForSetupIntent
                is ForSetupIntent -> "testSessionId"
            }
            // simulates API call.
            delay(500)
            _viewEffect.emit(OpenConnectionsFlow(linkedSessionId))
        }
    }

    // TODO receive object from connections flow.
    fun onConnectionsResult(linkedAccountSessionId: String) {
        viewModelScope.launch {
            when (args) {
                is ForPaymentIntent -> {
                    runCatching { attachToPaymentIntent(linkedAccountSessionId, args)!! }
                        .onSuccess { _viewEffect.emit(FinishWithPaymentIntent(it)) }
                        .onFailure { finishWithError(it) }
                }
                is ForSetupIntent -> {
                    runCatching { attachToSetupIntent(linkedAccountSessionId, args)!! }
                        .onSuccess { _viewEffect.emit(FinishWithSetupIntent(it)) }
                        .onFailure { finishWithError(it) }
                }
            }
        }
    }

    private suspend fun finishWithError(throwable: Throwable) {
        _viewEffect.emit(CollectBankAccountViewEffect.FinishWithError(throwable))
    }

    private suspend fun attachToPaymentIntent(
        linkedAccountSessionId: String,
        args: ForPaymentIntent
    ) = stripeRepository.attachLinkAccountSessionToPaymentIntent(
        linkAccountSessionId = linkedAccountSessionId,
        clientSecret = args.clientSecret,
        paymentIntentId = PaymentIntent.ClientSecret(args.clientSecret).paymentIntentId,
        requestOptions = apiRequestOptions
    )

    private suspend fun attachToSetupIntent(
        linkedAccountSessionId: String,
        args: ForSetupIntent,
    ) = stripeRepository.attachLinkAccountSessionToSetupIntent(
        linkAccountSessionId = linkedAccountSessionId,
        clientSecret = args.clientSecret,
        setupIntentId = SetupIntent.ClientSecret(args.clientSecret).setupIntentId,
        requestOptions = apiRequestOptions
    )

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

internal sealed class CollectBankAccountViewEffect {
    data class OpenConnectionsFlow(
        val linkedAccountSessionClientSecret: String
    ) : CollectBankAccountViewEffect()

    data class FinishWithPaymentIntent(
        val paymentIntent: PaymentIntent
    ) : CollectBankAccountViewEffect()

    data class FinishWithSetupIntent(
        val setupIntent: SetupIntent
    ) : CollectBankAccountViewEffect()

    data class FinishWithError(
        val exception: Throwable
    ) : CollectBankAccountViewEffect()
}
