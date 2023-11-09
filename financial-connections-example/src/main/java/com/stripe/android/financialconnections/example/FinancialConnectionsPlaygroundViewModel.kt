package com.stripe.android.financialconnections.example

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.Stripe
import com.stripe.android.confirmPaymentIntent
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.example.data.BackendRepository
import com.stripe.android.financialconnections.example.data.Settings
import com.stripe.android.financialconnections.example.settings.FinancialConnectionsPlaygroundUrlHelper
import com.stripe.android.financialconnections.example.settings.FlowSetting
import com.stripe.android.financialconnections.example.settings.PlaygroundSettings
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal class FinancialConnectionsPlaygroundViewModel(
    application: Application,
    launchUri: Uri?,
) : AndroidViewModel(application) {

    private val settings = Settings(application)
    private val repository = BackendRepository(settings)

    private val _state = MutableStateFlow(FinancialConnectionsPlaygroundState(application, launchUri))
    val state: StateFlow<FinancialConnectionsPlaygroundState> = _state

    private val _viewEffect = MutableSharedFlow<FinancialConnectionsPlaygroundViewEffect?>()
    val viewEffect: SharedFlow<FinancialConnectionsPlaygroundViewEffect?> = _viewEffect

    init {
        _state.update { it.copy(backendUrl = settings.backendUrl) }
        FinancialConnections.setEventListener { event: FinancialConnectionsEvent ->
            _state.update { state ->
                state.copy(
                    emittedEvents = state.emittedEvents + buildString {
                        append(event.name)
                        append(", ")
                        append(event.metadata.toMap().filterValues { it != null })
                    }
                )
            }
        }
    }

    fun startFinancialConnectionsSession() = with(state.value.settings) {
        _state.update {
            it.copy(
                status = emptyList(),
                emittedEvents = emptyList()
            )
        }
        Log.d(
            "FinancialConnections",
            "Starting session with settings: ${asJsonString()}"
        )
        saveToSharedPreferences(getApplication())
        when (state.value.flow) {
            Flow.Data -> startForData(this)
            Flow.Token -> startForToken(this)
            Flow.PaymentIntent -> startWithPaymentIntent(this)
        }
    }

    private fun startWithPaymentIntent(settings: PlaygroundSettings) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching {
                repository.createPaymentIntent(
                    settings.paymentIntentRequest()
                )
            }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    _state.update { current ->
                        current.copy(
                            publishableKey = it.publishableKey,
                            loading = true,
                            status = current.status + buildString {
                                append("Payment Intent created: ${it.intentSecret}")
                                appendLine()
                                append("Opening FinancialConnectionsSheet.")
                            }
                        )
                    }
                    _viewEffect.emit(
                        FinancialConnectionsPlaygroundViewEffect.OpenForPaymentIntent(
                            paymentIntentSecret = it.intentSecret,
                            publishableKey = it.publishableKey
                        )
                    )
                }
                // Error retrieving session: display error.
                .onFailure(::showError)
        }
    }

    private fun startForData(settings: PlaygroundSettings) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching {
                repository.createLinkAccountSession(settings.lasRequest())
            }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    showLoadingWithMessage("Session created, opening FinancialConnectionsSheet.")
                    _state.update { current -> current.copy(publishableKey = it.publishableKey) }
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

    private fun startForToken(settings: PlaygroundSettings) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching {
                repository.createLinkAccountSessionForToken(settings.lasRequest())
            }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    showLoadingWithMessage("Session created, opening FinancialConnectionsSheet.")
                    _state.update { current -> current.copy(publishableKey = it.publishableKey) }
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
        val errorText = when (error) {
            is HttpException -> error.response()?.errorBody()?.string() ?: error.message()
            else -> error.message
        }
        _state.update {
            it.copy(
                loading = false,
                status = it.status + "Error starting linked account session: $errorText"
            )
        }
    }

    private fun showLoadingWithMessage(message: String) {
        _state.update {
            it.copy(
                loading = true,
                status = it.status + message
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
        _state.update { it.copy(loading = false, status = it.status + statusText) }
    }

    fun onFinancialConnectionsSheetResult(result: FinancialConnectionsSheetResult) {
        val statusText = when (result) {
            is FinancialConnectionsSheetResult.Completed -> {
                "Completed!" + result.financialConnectionsSession.toString()
            }

            is FinancialConnectionsSheetResult.Failed -> "Failed! ${result.error}"
            is FinancialConnectionsSheetResult.Canceled -> "Cancelled!"
        }
        _state.update { it.copy(loading = false, status = it.status + statusText) }
    }

    fun onCollectBankAccountLauncherResult(
        result: CollectBankAccountResult
    ) = viewModelScope.launch {
        when (result) {
            is CollectBankAccountResult.Completed -> runCatching {
                _state.update {
                    val session = result.response.financialConnectionsSession
                    val account = session.accounts.data.firstOrNull()
                    it.copy(
                        status = it.status + listOf(
                            "Session Completed! ${session.id} (account: ${account?.id})",
                            "Confirming Intent: ${result.response.intent.id}",
                        )
                    )
                }
                val params = ConfirmPaymentIntentParams.create(
                    clientSecret = requireNotNull(result.response.intent.clientSecret),
                    paymentMethodType = PaymentMethod.Type.USBankAccount
                )
                stripe(_state.value.publishableKey!!).confirmPaymentIntent(params)
            }.onSuccess {
                _state.update {
                    it.copy(
                        loading = false,
                        status = it.status + "Intent Confirmed!"
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        status = it.status + "Confirming intent Failed!: $error"
                    )
                }
            }

            is CollectBankAccountResult.Failed -> {
                _state.update {
                    it.copy(
                        loading = false,
                        status = it.status + "Failed! ${result.error}"
                    )
                }
            }

            is CollectBankAccountResult.Cancelled -> {
                _state.update { it.copy(loading = false, status = it.status + "Cancelled!") }
            }
        }
    }

    private fun stripe(publishableKey: String) = Stripe(
        getApplication(),
        publishableKey,
        null,
        true,
        emptySet()
    )

    fun onSettingsChanged(playgroundSettings: PlaygroundSettings) {
        _state.update {
            it.copy(
                settings = playgroundSettings,
            )
        }
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val uriSupplier: () -> Uri?,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return FinancialConnectionsPlaygroundViewModel(applicationSupplier(), uriSupplier()) as T
        }
    }
}

@Suppress("Unused")
enum class Merchant(val apiValue: String) {
    Test("testmode"),
    PartnerM("partner_m"),
    PartnerF("partner_f"),
    PartnerD("partner_d"),
    PlatformC("strash"),
    App2App("app2app"),
    Networking("networking"),
    NetworkingTestMode("networking_testmode"),
    Livetesting("live_testing"),
    Other("other");

    companion object {
        fun fromApiValue(apiValue: String): Merchant =
            Merchant.values().first { it.apiValue == apiValue }
    }
}

enum class Flow(val apiValue: String) {
    Data("Data"),
    Token("Token"),
    PaymentIntent("PaymentIntent");

    companion object {
        fun fromApiValue(apiValue: String): Flow =
            Flow.values().first { it.apiValue == apiValue }
    }
}

enum class NativeOverride(val apiValue: String) {
    None("none"), Native("native"), Web("web");

    companion object {
        fun fromApiValue(apiValue: String): NativeOverride =
            NativeOverride.values().first { it.apiValue == apiValue }
    }
}

sealed class FinancialConnectionsPlaygroundViewEffect {
    data class OpenForData(
        val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsPlaygroundViewEffect()

    data class OpenForToken(
        val configuration: FinancialConnectionsSheet.Configuration
    ) : FinancialConnectionsPlaygroundViewEffect()

    data class OpenForPaymentIntent(
        val paymentIntentSecret: String,
        val publishableKey: String
    ) : FinancialConnectionsPlaygroundViewEffect()
}

internal data class FinancialConnectionsPlaygroundState(
    val backendUrl: String = "",
    val settings: PlaygroundSettings,
    val loading: Boolean = false,
    val publishableKey: String? = null,
    val status: List<String> = emptyList(),
    val emittedEvents: List<String> = emptyList()
) {

    constructor(application: Application, launchUri: Uri?) : this(
        settings = FinancialConnectionsPlaygroundUrlHelper.settingsFromUri(launchUri)
            ?: PlaygroundSettings.createFromSharedPreferences(application)
    )

    val flow: Flow = settings.get<FlowSetting>().selectedOption
}
