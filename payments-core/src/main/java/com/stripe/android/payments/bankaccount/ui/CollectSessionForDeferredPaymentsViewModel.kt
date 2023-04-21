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
import com.stripe.android.payments.bankaccount.di.DaggerCollectSessionForDeferredPaymentsComponent
import com.stripe.android.payments.bankaccount.domain.CreateFinancialConnectionsSession
import com.stripe.android.payments.bankaccount.navigation.CollectSessionForDeferredPaymentsContract
import com.stripe.android.payments.bankaccount.navigation.CollectSessionForDeferredPaymentsResult
import com.stripe.android.utils.requireApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("ConstructorParameterNaming", "LongParameterList")
internal class CollectSessionForDeferredPaymentsViewModel @Inject constructor(
    // bound instances
    private val args: CollectSessionForDeferredPaymentsContract.Args,
    private val _viewEffect: MutableSharedFlow<CollectSessionForDeferredPaymentsViewEffect>,
    // injected instances
    private val createFinancialConnectionsSession: CreateFinancialConnectionsSession,
    private val savedStateHandle: SavedStateHandle,
    private val logger: Logger
) : ViewModel() {

    private var hasLaunched: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_HAS_LAUNCHED) == true
        set(value) = savedStateHandle.set(KEY_HAS_LAUNCHED, value)

    val viewEffect: SharedFlow<CollectSessionForDeferredPaymentsViewEffect> = _viewEffect

    init {
        if (hasLaunched.not()) {
            viewModelScope.launch {
                createFinancialConnectionsSession()
            }
        }
    }

    private suspend fun createFinancialConnectionsSession() {
        createFinancialConnectionsSession.forDeferredPayments(
            publishableKey = args.publishableKey,
            stripeAccountId = args.stripeAccountId,
            elementsSessionId = args.elementsSessionId,
            customerId = args.customer,
            amount = args.amount,
            currency = args.currency
        ).fold(
            onSuccess = { session ->
                session.clientSecret?.let { clientSecret ->
                    logger.debug("Bank account session created! $clientSecret.")
                    hasLaunched = true
                    _viewEffect.emit(
                        CollectSessionForDeferredPaymentsViewEffect.OpenConnectionsFlow(
                            financialConnectionsSessionSecret = clientSecret,
                            publishableKey = args.publishableKey,
                            stripeAccountId = args.stripeAccountId
                        )
                    )
                }
            },
            onFailure = {
                finishWithError(it)
            }
        )
    }

    fun onConnectionsResult(result: FinancialConnectionsSheetResult) {
        hasLaunched = false
        viewModelScope.launch {
            when (result) {
                is FinancialConnectionsSheetResult.Canceled ->
                    finishWithResult(CollectSessionForDeferredPaymentsResult.Cancelled)
                is FinancialConnectionsSheetResult.Failed ->
                    finishWithError(result.error)
                is FinancialConnectionsSheetResult.Completed ->
                    finishWithFinancialConnectionsSession(result.financialConnectionsSession)
            }
        }
    }

    private suspend fun finishWithResult(result: CollectSessionForDeferredPaymentsResult) {
        _viewEffect.emit(CollectSessionForDeferredPaymentsViewEffect.FinishWithResult(result))
    }

    private fun finishWithFinancialConnectionsSession(financialConnectionsSession: FinancialConnectionsSession) {
        viewModelScope.launch {
            finishWithResult(
                CollectSessionForDeferredPaymentsResult.Completed(
                    financialConnectionsSession = financialConnectionsSession
                )
            )
        }
    }

    private suspend fun finishWithError(throwable: Throwable) {
        logger.error("Error", Exception(throwable))
        finishWithResult(CollectSessionForDeferredPaymentsResult.Failed(throwable))
    }

    class Factory(
        private val argsSupplier: () -> CollectSessionForDeferredPaymentsContract.Args,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            return DaggerCollectSessionForDeferredPaymentsComponent.builder()
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
