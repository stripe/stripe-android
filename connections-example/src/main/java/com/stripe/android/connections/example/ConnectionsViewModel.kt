package com.stripe.android.connections.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.ConnectionsSheetResult
import com.stripe.android.connections.example.ConnectionsViewEffect.OpenConnectionsSheet
import com.stripe.android.connections.example.data.BackendRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConnectionsViewModel : ViewModel() {

    private val repository = BackendRepository()

    private val _state = MutableStateFlow(ConnectionsState())
    val state: StateFlow<ConnectionsState> = _state

    private val _viewEffect = MutableSharedFlow<ConnectionsViewEffect>()
    val viewEffect: SharedFlow<ConnectionsViewEffect> = _viewEffect

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
                        OpenConnectionsSheet(
                            configuration = ConnectionsSheet.Configuration(
                                it.clientSecret,
                                "pk_live_Uxk6GdfUJzeCePW1FdQmeOFM"
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

    /**
     * Helper function to mutate state.
     */
    private suspend fun setState(block: ConnectionsState.() -> ConnectionsState) {
        val newState = block(state.value)
        _state.emit(newState)
    }
}
