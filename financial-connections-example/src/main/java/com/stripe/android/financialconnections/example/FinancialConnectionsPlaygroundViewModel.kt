package com.stripe.android.financialconnections.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetExample
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

    private val _viewEffect = MutableSharedFlow<FinancialConnectionsExampleViewEffect>()
    val viewEffect: SharedFlow<FinancialConnectionsExampleViewEffect> = _viewEffect

    fun startFinancialConnectionsSessionForData(
        mode: Mode
    ) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching { repository.createLinkAccountSession(mode.flow) }
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
                "Completed!" + result.financialConnectionsSession.toString()
            }

            is FinancialConnectionsSheetResult.Failed -> "Failed! ${result.error}"
            is FinancialConnectionsSheetResult.Canceled -> "Cancelled!"
        }
        _state.update { it.copy(loading = false, status = statusText) }
    }
}

enum class Mode(val flow: String) {
    Test("testmode"), Live("mx")
}
