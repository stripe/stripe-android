package com.stripe.android.financialconnections.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult.Canceled
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult.Completed
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult.Failed
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenConnectionsSheetExample
import com.stripe.android.financialconnections.example.data.BackendRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FinancialConnectionsExampleViewModel : ViewModel() {

    private val repository = BackendRepository()

    private val _state = MutableStateFlow(FinancialConnectionsExampleState())
    val state: StateFlow<FinancialConnectionsExampleState> = _state

    private val _viewEffect = MutableSharedFlow<FinancialConnectionsExampleViewEffect>()
    val viewEffect: SharedFlow<FinancialConnectionsExampleViewEffect> = _viewEffect

    fun startLinkAccountSession() {
        viewModelScope.launch {
            setState {
                copy(
                    loading = true,
                    status = "Fetching link account session from example backend!"
                )
            }
            kotlin.runCatching { repository.createLinkAccountSession() }
                // Success creating session: open ConnectionsSheet with received secret
                .onSuccess {
                    setState {
                        copy(
                            loading = false,
                            status = "Session created, opening ConnectionsSheet."
                        )
                    }
                    _viewEffect.emit(
                        OpenConnectionsSheetExample(
                            configuration = FinancialConnectionsSheet.Configuration(
                                it.clientSecret,
                                it.publishableKey
                            )
                        )
                    )
                }
                // Error retrieving session: display error.
                .onFailure {
                    setState {
                        copy(
                            loading = false,
                            status = "Error starting linked account session: $it"
                        )
                    }
                }
        }
    }

    fun onFinancialConnectionsSheetResult(result: FinancialConnectionsSheetResult) {
        val statusText = when (result) {
            is Completed -> {
                val linkedAccountList = result.linkAccountSession.linkedAccounts
                linkedAccountList.linkedAccounts.joinToString("\n") {
                    "${it.institutionName} - ${it.displayName} - ${it.last4} - ${it.category}/${it.subcategory}"
                }
            }
            is Failed -> "Failed! ${result.error}"
            is Canceled -> "Cancelled!"
        }
        viewModelScope.launch {
            setState { copy(status = statusText) }
        }
    }

    /**
     * Helper function to mutate state.
     */
    private suspend fun setState(block: FinancialConnectionsExampleState.() -> FinancialConnectionsExampleState) {
        val newState = block(state.value)
        _state.emit(newState)
    }
}
