package com.stripe.android.payments.bankaccount.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.di.DaggerCollectBankAccountComponent
import com.stripe.android.payments.bankaccount.domain.AttachFinancialConnectionsSession
import com.stripe.android.payments.bankaccount.domain.CreateFinancialConnectionsSession
import com.stripe.android.payments.bankaccount.domain.RetrieveStripeIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal.Cancelled
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal.Completed
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal.Failed
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.OpenConnectionsFlow
import com.stripe.android.utils.requireApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("ConstructorParameterNaming", "LongParameterList")
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
                is CollectBankAccountContract.Args.ForDeferredPaymentIntent ->
                    createFinancialConnectionsSession.forDeferredPayments(
                        publishableKey = args.publishableKey,
                        stripeAccountId = args.stripeAccountId,
                        elementsSessionId = args.elementsSessionId,
                        customerId = args.customerId,
                        onBehalfOf = args.onBehalfOf,
                        amount = args.amount,
                        currency = args.currency
                    )
                is CollectBankAccountContract.Args.ForDeferredSetupIntent ->
                    createFinancialConnectionsSession.forDeferredPayments(
                        publishableKey = args.publishableKey,
                        stripeAccountId = args.stripeAccountId,
                        elementsSessionId = args.elementsSessionId,
                        customerId = args.customerId,
                        onBehalfOf = args.onBehalfOf,
                        amount = null,
                        currency = null
                    )
                is CollectBankAccountContract.Args.ForPaymentIntent ->
                    createFinancialConnectionsSession.forPaymentIntent(
                        publishableKey = args.publishableKey,
                        stripeAccountId = args.stripeAccountId,
                        clientSecret = args.clientSecret,
                        customerName = configuration.name,
                        customerEmail = configuration.email
                    )
                is CollectBankAccountContract.Args.ForSetupIntent ->
                    createFinancialConnectionsSession.forSetupIntent(
                        publishableKey = args.publishableKey,
                        stripeAccountId = args.stripeAccountId,
                        clientSecret = args.clientSecret,
                        customerName = configuration.name,
                        customerEmail = configuration.email
                    )
            }
                .mapCatching { requireNotNull(it.clientSecret) }
                .onSuccess { financialConnectionsSessionSecret: String ->
                    logger.debug("Bank account session created! $financialConnectionsSessionSecret.")
                    hasLaunched = true
                    _viewEffect.emit(
                        OpenConnectionsFlow(
                            financialConnectionsSessionSecret = financialConnectionsSessionSecret,
                            publishableKey = args.publishableKey,
                            stripeAccountId = args.stripeAccountId
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

    private suspend fun finishWithResult(result: CollectBankAccountResultInternal) {
        _viewEffect.emit(CollectBankAccountViewEffect.FinishWithResult(result))
    }

    private fun finishWithFinancialConnectionsSession(
        financialConnectionsSession: FinancialConnectionsSession
    ) {
        viewModelScope.launch {
            val clientSecret = args.clientSecret
            val retrieveIntentResult = if (clientSecret == null) {
                // client secret is null for deferred intents.
                Result.success(null)
            } else {
                retrieveStripeIntent(args.publishableKey, clientSecret)
            }
            retrieveIntentResult
                .onFailure { finishWithError(it) }
                .onSuccess { intent ->
                    finishWithResult(
                        Completed(
                            CollectBankAccountResponseInternal(
                                intent,
                                financialConnectionsSession = financialConnectionsSession
                            )
                        )
                    )
                }
        }
    }

    private fun attachFinancialConnectionsSessionToIntent(financialConnectionsSession: FinancialConnectionsSession) {
        viewModelScope.launch {
            when (args) {
                is CollectBankAccountContract.Args.ForDeferredPaymentIntent ->
                    error("Attach requires client secret")
                is CollectBankAccountContract.Args.ForDeferredSetupIntent ->
                    error("Attach requires client secret")
                is CollectBankAccountContract.Args.ForPaymentIntent ->
                    attachFinancialConnectionsSession.forPaymentIntent(
                        publishableKey = args.publishableKey,
                        stripeAccountId = args.stripeAccountId,
                        clientSecret = args.clientSecret,
                        linkedAccountSessionId = financialConnectionsSession.id
                    )
                is CollectBankAccountContract.Args.ForSetupIntent ->
                    attachFinancialConnectionsSession.forSetupIntent(
                        publishableKey = args.publishableKey,
                        stripeAccountId = args.stripeAccountId,
                        clientSecret = args.clientSecret,
                        linkedAccountSessionId = financialConnectionsSession.id
                    )
            }
                .mapCatching {
                    Completed(
                        CollectBankAccountResponseInternal(
                            it,
                            financialConnectionsSession
                        )
                    )
                }
                .onSuccess { result: Completed ->
                    logger.debug("Bank account session attached to  intent!!")
                    finishWithResult(result)
                }
                .onFailure { finishWithError(it) }
        }
    }

    private suspend fun finishWithError(throwable: Throwable) {
        logger.error("Error", Exception(throwable))
        finishWithResult(Failed(throwable))
    }

    class Factory(
        private val argsSupplier: () -> CollectBankAccountContract.Args,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            return DaggerCollectBankAccountComponent.builder()
                .savedStateHandle(savedStateHandle)
                .application(application)
                .viewEffect(MutableSharedFlow())
                .configuration(argsSupplier()).build()
                .viewModel as T
        }
    }

    companion object {
        private const val KEY_HAS_LAUNCHED = "key_has_launched"
    }
}
