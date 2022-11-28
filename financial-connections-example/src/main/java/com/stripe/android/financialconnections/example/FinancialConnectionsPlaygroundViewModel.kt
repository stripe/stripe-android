package com.stripe.android.financialconnections.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.example.data.BackendRepository
import com.stripe.android.financialconnections.example.data.Settings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinancialConnectionsPlaygroundViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = BackendRepository(Settings(application))

    private val _state = MutableStateFlow(FinancialConnectionsExampleState())
    val state: StateFlow<FinancialConnectionsExampleState> = _state

    private val _viewEffect = MutableSharedFlow<FinancialConnectionsPlaygroundViewEffect?>()
    val viewEffect: SharedFlow<FinancialConnectionsPlaygroundViewEffect?> = _viewEffect

    fun startFinancialConnectionsSession(
        mode: Mode,
        flow: Flow
    ) {
        when (flow) {
            Flow.Data -> startForData(mode)
            Flow.Token -> startForToken(mode)
        }

    }

    private fun startForData(mode: Mode) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching { repository.createLinkAccountSession(mode.flow) }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    showLoadingWithMessage("Session created, opening FinancialConnectionsSheet.")
                    _viewEffect.emit(
                        FinancialConnectionsPlaygroundViewEffect.OpenForData(
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

    private fun startForToken(mode: Mode) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching { repository.createLinkAccountSessionForToken(mode.flow) }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    showLoadingWithMessage("Session created, opening FinancialConnectionsSheet.")
                    _viewEffect.emit(
                        FinancialConnectionsPlaygroundViewEffect.OpenForToken(
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

    fun onFinancialConnectionsSheetForTokenResult(result: FinancialConnectionsSheetForTokenResult) {
        val statusText = when (result) {
            is FinancialConnectionsSheetForTokenResult.Completed -> {
                "Completed!\n" +
                    "Session: ${result.financialConnectionsSession}\n" +
                    "Token: ${result.token}\n"
            }

            is FinancialConnectionsSheetForTokenResult.Failed -> "Failed! ${result.error}"
            is FinancialConnectionsSheetForTokenResult.Canceled -> "Cancelled!"
        }
        _state.update { it.copy(loading = false, status = statusText) }
    }

    fun onFinancialConnectionsSheetResult(result: FinancialConnectionsSheetResult) {
        val statusText = when (result) {
            is FinancialConnectionsSheetResult.Completed -> {
                "Completed!" + result.financialConnectionsSession.toPrettyString()
            }

            is FinancialConnectionsSheetResult.Failed -> "Failed! ${result.error}"
            is FinancialConnectionsSheetResult.Canceled -> "Cancelled!"
        }
        _state.update { it.copy(loading = false, status = statusText) }
    }

    private fun Any.toPrettyString(indentSize: Int = 4) = " ".repeat(indentSize).let { indent ->
        toString()
            .replace(", ", ",\n$indent")
            .replace("(", "(\n$indent")
            .dropLast(1) + "\n)"
    }
}

enum class Mode(val flow: String) {
    Test("testmode"), Live("mx")
}

enum class Flow {
    Data, Token
}

enum class NativeOverride {
    None, Native, Web
}

sealed class FinancialConnectionsPlaygroundViewEffect {
    data class OpenForData(
        val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsPlaygroundViewEffect()

    data class OpenForToken(
        val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsPlaygroundViewEffect()
}
