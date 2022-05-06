package com.stripe.android.payments.bankaccount.ui

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.di.DaggerCollectBankAccountComponent
import com.stripe.android.payments.bankaccount.domain.AttachFinancialConnectionsSession
import com.stripe.android.payments.bankaccount.domain.CreateFinancialConnectionsSession
import com.stripe.android.payments.bankaccount.domain.RetrieveStripeIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract.Args.ForPaymentIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract.Args.ForSetupIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponse
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult.Cancelled
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult.Completed
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.OpenConnectionsFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("ConstructorParameterNaming")
internal class CollectBankAccountViewModel @Inject constructor(
    // bound instances
    private val args: CollectBankAccountContract.Args,
    private val _viewEffect: MutableSharedFlow<CollectBankAccountViewEffect>,
    // injected instances
    private val createFinancialConnectionsSession: CreateFinancialConnectionsSession,
    private val attachFinancialConnectionsSession: AttachFinancialConnectionsSession,
    private val retrieveStripeIntent: RetrieveStripeIntent,
    private val savedStateHandle: SavedStateHandle,
    private val logger: Logger
) : ViewModel() {

    private var hasLaunched: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_HAS_LAUNCHED) == true
        set(value) = savedStateHandle.set(KEY_HAS_LAUNCHED, value)

    val viewEffect: SharedFlow<CollectBankAccountViewEffect> = _viewEffect

    init {
        if (hasLaunched.not()) {
            viewModelScope.launch {
                createFinancialConnectionsSession()
            }
        }
    }

    private suspend fun createFinancialConnectionsSession() {
        when (val configuration = args.configuration) {
            is CollectBankAccountConfiguration.USBankAccount -> when (args) {
                is ForPaymentIntent -> createFinancialConnectionsSession.forPaymentIntent(
                    publishableKey = args.publishableKey,
                    clientSecret = args.clientSecret,
                    customerName = configuration.name,
                    customerEmail = configuration.email
                )
                is ForSetupIntent -> createFinancialConnectionsSession.forSetupIntent(
                    publishableKey = args.publishableKey,
                    clientSecret = args.clientSecret,
                    customerName = configuration.name,
                    customerEmail = configuration.email
                )
            }
                .mapCatching { requireNotNull(it.clientSecret) }
                .onSuccess { linkedAccountSessionSecret: String ->
                    logger.debug("Bank account session created! $linkedAccountSessionSecret.")
                    hasLaunched = true
                    _viewEffect.emit(
                        OpenConnectionsFlow(
                            linkedAccountSessionClientSecret = linkedAccountSessionSecret,
                            publishableKey = args.publishableKey
                        )
                    )
                }
                .onFailure { finishWithError(it) }
        }
    }

    fun onConnectionsResult(result: FinancialConnectionsSheetResult) {
        hasLaunched = false
        viewModelScope.launch {
            when (result) {
                is FinancialConnectionsSheetResult.Canceled ->
                    finishWithResult(Cancelled)
                is FinancialConnectionsSheetResult.Failed ->
                    finishWithError(result.error)
                is FinancialConnectionsSheetResult.Completed ->
                    if (args.attachToIntent) {
                        attachFinancialConnectionsSessionToIntent(result.financialConnectionsSession)
                    } else {
                        finishWithFinancialConnectionsSession(result.financialConnectionsSession)
                    }
            }
        }
    }

    private suspend fun finishWithResult(result: CollectBankAccountResult) {
        _viewEffect.emit(CollectBankAccountViewEffect.FinishWithResult(result))
    }

    private fun finishWithFinancialConnectionsSession(financialConnectionsSession: FinancialConnectionsSession) {
        viewModelScope.launch {
            retrieveStripeIntent(
                args.publishableKey,
                args.clientSecret
            ).onSuccess { stripeIntent ->
                finishWithResult(
                    Completed(
                        CollectBankAccountResponse(
                            intent = stripeIntent,
                            financialConnectionsSession = financialConnectionsSession
                        )
                    )
                )
            }.onFailure {
                finishWithError(it)
            }
        }
    }

    private fun attachFinancialConnectionsSessionToIntent(financialConnectionsSession: FinancialConnectionsSession) {
        viewModelScope.launch {
            when (args) {
                is ForPaymentIntent -> attachFinancialConnectionsSession.forPaymentIntent(
                    publishableKey = args.publishableKey,
                    clientSecret = args.clientSecret,
                    linkedAccountSessionId = financialConnectionsSession.id
                )
                is ForSetupIntent -> attachFinancialConnectionsSession.forSetupIntent(
                    publishableKey = args.publishableKey,
                    clientSecret = args.clientSecret,
                    linkedAccountSessionId = financialConnectionsSession.id
                )
            }
                .mapCatching { Completed(CollectBankAccountResponse(it, financialConnectionsSession)) }
                .onSuccess { result: Completed ->
                    logger.debug("Bank account session attached to  intent!!")
                    finishWithResult(result)
                }
                .onFailure { finishWithError(it) }
        }
    }

    private suspend fun finishWithError(throwable: Throwable) {
        logger.error("Error", Exception(throwable))
        finishWithResult(CollectBankAccountResult.Failed(throwable))
    }

    class Factory(
        private val applicationSupplier: () -> Application,
        private val argsSupplier: () -> CollectBankAccountContract.Args,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T {
            return DaggerCollectBankAccountComponent.builder()
                .savedStateHandle(savedStateHandle)
                .application(applicationSupplier())
                .viewEffect(MutableSharedFlow())
                .configuration(argsSupplier()).build()
                .viewModel as T
        }
    }

    companion object {
        private const val KEY_HAS_LAUNCHED = "key_has_launched"
    }
}
