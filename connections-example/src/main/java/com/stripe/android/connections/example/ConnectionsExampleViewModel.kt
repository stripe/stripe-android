package com.stripe.android.connections.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.ConnectionsSheetForTokenResult
import com.stripe.android.connections.ConnectionsSheetResult
import com.stripe.android.connections.example.ConnectionsExampleViewEffect.OpenConnectionsSheetExample
import com.stripe.android.connections.example.ConnectionsExampleViewEffect.OpenConnectionsSheetForTokenExample
import com.stripe.android.connections.example.data.BackendRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConnectionsExampleViewModel : ViewModel() {

    private val repository = BackendRepository()

    private val _state = MutableStateFlow(ConnectionsExampleState())
    val state: StateFlow<ConnectionsExampleState> = _state

    private val _viewEffect = MutableSharedFlow<ConnectionsExampleViewEffect>()
    val viewEffect: SharedFlow<ConnectionsExampleViewEffect> = _viewEffect

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
                            configuration = ConnectionsSheet.Configuration(
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

    fun startLinkAccountSessionForToken() {
        viewModelScope.launch {
            setState {
                copy(
                    loading = true,
                    status = "Fetching link account session for token from example backend!"
                )
            }
            kotlin.runCatching { repository.createLinkAccountSessionForToken() }
                // Success creating session: open ConnectionsSheet with received secret
                .onSuccess {
                    setState {
                        copy(
                            loading = false,
                            status = "Session created, opening ConnectionsSheet for Token."
                        )
                    }
                    _viewEffect.emit(
                        OpenConnectionsSheetForTokenExample(
                            configuration = ConnectionsSheet.Configuration(
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

    fun onConnectionsSheetResult(connectionsSheetResult: ConnectionsSheetResult) {
        val statusText = when (connectionsSheetResult) {
            is ConnectionsSheetResult.Completed -> {
                connectionsSheetResult.linkAccountSession.linkedAccounts.linkedAccounts.joinToString(
                    "\n"
                ) {
                    "${it.institutionName} - ${it.displayName} - ${it.last4} - ${it.category}/${it.subcategory}"
                }
            }
            is ConnectionsSheetResult.Failed -> "Failed! ${connectionsSheetResult.error}"
            is ConnectionsSheetResult.Canceled -> "Cancelled!"
        }
        viewModelScope.launch {
            setState { copy(status = statusText) }
        }
    }

    fun onConnectionsSheetForTokenResult(result: ConnectionsSheetForTokenResult) {
        val statusText = when (result) {
            is ConnectionsSheetForTokenResult.Completed -> {
                "Token ${result.token.id} generated for bank account ${result.token.bankAccount?.last4}"
            }
            is ConnectionsSheetForTokenResult.Failed -> "Failed! ${result.error}"
            is ConnectionsSheetForTokenResult.Canceled -> "Cancelled!"
        }
        viewModelScope.launch {
            setState { copy(status = statusText) }
        }
    }

    /**
     * Helper function to mutate state.
     */
    private suspend fun setState(block: ConnectionsExampleState.() -> ConnectionsExampleState) {
        val newState = block(state.value)
        _state.emit(newState)
    }
}
