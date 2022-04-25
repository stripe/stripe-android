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
import com.stripe.android.financialconnections.model.LinkAccountSession
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.di.DaggerCollectBankAccountComponent
import com.stripe.android.payments.bankaccount.domain.AttachLinkAccountSession
import com.stripe.android.payments.bankaccount.domain.CreateLinkAccountSession
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
    private val createLinkAccountSession: CreateLinkAccountSession,
    private val attachLinkAccountSession: AttachLinkAccountSession,
    private val retrieveStripeIntent: RetrieveStripeIntent,
    private val logger: Logger
) : ViewModel() {

    val viewEffect: SharedFlow<CollectBankAccountViewEffect> = _viewEffect

    init {
        viewModelScope.launch {
            createLinkAccountSession()
        }
    }

    private suspend fun createLinkAccountSession() {
        when (val configuration = args.configuration) {
            is CollectBankAccountConfiguration.USBankAccount -> when (args) {
                is ForPaymentIntent -> createLinkAccountSession.forPaymentIntent(
                    publishableKey = args.publishableKey,
                    clientSecret = args.clientSecret,
                    customerName = configuration.name,
                    customerEmail = configuration.email
                )
                is ForSetupIntent -> createLinkAccountSession.forSetupIntent(
                    publishableKey = args.publishableKey,
                    clientSecret = args.clientSecret,
                    customerName = configuration.name,
                    customerEmail = configuration.email
                )
            }
                .mapCatching { requireNotNull(it.clientSecret) }
                .onSuccess { linkedAccountSessionSecret: String ->
                    logger.debug("Bank account session created! $linkedAccountSessionSecret.")
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
        viewModelScope.launch {
            when (result) {
                is FinancialConnectionsSheetResult.Canceled ->
                    finishWithResult(Cancelled)
                is FinancialConnectionsSheetResult.Failed ->
                    finishWithError(result.error)
                is FinancialConnectionsSheetResult.Completed ->
                    if (args.attachToIntent) {
                        attachLinkAccountSessionToIntent(result.linkAccountSession)
                    } else {
                        finishWithLinkAccountSession(result.linkAccountSession)
                    }
            }
        }
    }

    private suspend fun finishWithResult(result: CollectBankAccountResult) {
        _viewEffect.emit(CollectBankAccountViewEffect.FinishWithResult(result))
    }

    private fun finishWithLinkAccountSession(linkAccountSession: LinkAccountSession) {
        viewModelScope.launch {
            retrieveStripeIntent(
                args.publishableKey,
                args.clientSecret
            ).onSuccess { stripeIntent ->
                finishWithResult(
                    Completed(
                        CollectBankAccountResponse(
                            intent = stripeIntent,
                            linkAccountSession = linkAccountSession
                        )
                    )
                )
            }.onFailure {
                finishWithError(it)
            }
        }
    }

    private fun attachLinkAccountSessionToIntent(linkAccountSession: LinkAccountSession) {
        viewModelScope.launch {
            when (args) {
                is ForPaymentIntent -> attachLinkAccountSession.forPaymentIntent(
                    publishableKey = args.publishableKey,
                    clientSecret = args.clientSecret,
                    linkedAccountSessionId = linkAccountSession.id
                )
                is ForSetupIntent -> attachLinkAccountSession.forSetupIntent(
                    publishableKey = args.publishableKey,
                    clientSecret = args.clientSecret,
                    linkedAccountSessionId = linkAccountSession.id
                )
            }
                .mapCatching { Completed(CollectBankAccountResponse(it, linkAccountSession)) }
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
                .application(applicationSupplier())
                .viewEffect(MutableSharedFlow())
                .configuration(argsSupplier()).build()
                .viewModel as T
        }
    }
}
