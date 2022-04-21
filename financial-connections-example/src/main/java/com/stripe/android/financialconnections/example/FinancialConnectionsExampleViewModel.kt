package com.stripe.android.financialconnections.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.connections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetExample
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetForTokenExample
import com.stripe.android.financialconnections.example.data.BackendRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinancialConnectionsExampleViewModel : ViewModel() {

    private val repository = BackendRepository()

    private val _state = MutableStateFlow(FinancialConnectionsExampleState())
    val state: StateFlow<FinancialConnectionsExampleState> = _state

    private val _viewEffect = MutableSharedFlow<FinancialConnectionsExampleViewEffect>()
    val viewEffect: SharedFlow<FinancialConnectionsExampleViewEffect> = _viewEffect

    fun startLinkAccountSessionForData() {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching { repository.createLinkAccountSession() }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    showLoadingWithMessage("Session created, opening FinancialConnectionsSheet.")
                    _viewEffect.emit(
                        OpenFinancialConnectionsSheetExample(
                            configuration = FinancialConnectionsSheet.Configuration(
                                it.clientSecret,
                                it.publishableKey
                            )
                        )
                    )
                }
                // Error retrieving session: display error.
                .onFailure(::showError)
        }
    }

    fun startLinkAccountSessionForToken() {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching { repository.createLinkAccountSessionForToken() }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    showLoadingWithMessage("Session created, opening FinancialConnectionsSheet.")
                    _viewEffect.emit(
                        OpenFinancialConnectionsSheetForTokenExample(
                            configuration = FinancialConnectionsSheet.Configuration(
                                it.clientSecret,
                                it.publishableKey
                            )
                        )
                    )
                }
                // Error retrieving session: display error.
                .onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        _state.update {
            it.copy(
                loading = false,
                status = "Error starting linked account session: $error"
            )
        }
    }

    private fun showLoadingWithMessage(message: String) {
        _state.update {
            it.copy(
                loading = true,
                status = message
            )
        }
    }

    fun onFinancialConnectionsSheetResult(result: FinancialConnectionsSheetResult) {
        val statusText = when (result) {
            is FinancialConnectionsSheetResult.Completed -> {
                val linkedAccountList = result.linkAccountSession.linkedAccounts
                linkedAccountList.linkedAccounts.joinToString("\n") {
                    "${it.institutionName} - ${it.displayName} - ${it.last4} - ${it.category}/${it.subcategory}"
                }
            }
            is FinancialConnectionsSheetResult.Failed -> "Failed! ${result.error}"
            is FinancialConnectionsSheetResult.Canceled -> "Cancelled!"
        }
        _state.update { it.copy(loading = false, status = statusText) }
    }

    fun onFinancialConnectionsSheetForBankAccountTokenResult(
        result: FinancialConnectionsSheetForTokenResult
    ) {
        val statusText = when (result) {
            is FinancialConnectionsSheetForTokenResult.Completed -> {
                "Token ${result.token.id} generated for bank account ${result.token.bankAccount?.last4}"
            }
            is FinancialConnectionsSheetForTokenResult.Failed -> "Failed! ${result.error}"
            is FinancialConnectionsSheetForTokenResult.Canceled -> "Cancelled!"
        }
        _state.update { it.copy(loading = false, status = statusText) }
    }
}
