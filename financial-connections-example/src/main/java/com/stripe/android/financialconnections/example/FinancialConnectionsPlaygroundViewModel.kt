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
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext.InitializationMode
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.example.data.BackendRepository
import com.stripe.android.financialconnections.example.data.Settings
import com.stripe.android.financialconnections.example.settings.ConfirmIntentSetting
import com.stripe.android.financialconnections.example.settings.ExperienceSetting
import com.stripe.android.financialconnections.example.settings.FinancialConnectionsPlaygroundUrlHelper
import com.stripe.android.financialconnections.example.settings.FlowSetting
import com.stripe.android.financialconnections.example.settings.IntegrationTypeSetting
import com.stripe.android.financialconnections.example.settings.PlaygroundSettings
import com.stripe.android.financialconnections.example.settings.StripeAccountIdSetting
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountForInstantDebitsResult
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.paymentsheet.PaymentSheetResult
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

    fun connectAccounts() = with(state.value.settings) {
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

        when (state.value.experience) {
            Experience.FinancialConnections -> {
                when (state.value.flow) {
                    Flow.Data -> startForData(this)
                    Flow.Token -> startForToken(this)
                    Flow.PaymentIntent -> startWithPaymentIntent(this, experience = Experience.FinancialConnections)
                }
            }
            Experience.InstantDebits -> {
                startWithPaymentIntent(this, experience = Experience.InstantDebits)
            }
            Experience.LinkCardBrand -> {
                startWithPaymentIntent(this, experience = Experience.LinkCardBrand)
            }
        }
    }

    private fun startWithPaymentIntent(
        settings: PlaygroundSettings,
        experience: Experience,
    ) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching {
                repository.createPaymentIntent(
                    settings.paymentIntentRequest(
                        linkMode = experience.linkMode,
                    )
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
                            publishableKey = it.publishableKey,
                            ephemeralKey = it.ephemeralKey,
                            customerId = it.customerId,
                            elementsSessionContext = ElementsSessionContext(
                                initializationMode = InitializationMode.PaymentIntent(it.intentId),
                                amount = it.amount,
                                currency = it.currency,
                                linkMode = LinkMode.LinkPaymentMethod,
                            ),
                            experience = settings.get<ExperienceSetting>().selectedOption,
                            integrationType = settings.get<IntegrationTypeSetting>().selectedOption,
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
                                financialConnectionsSessionClientSecret = it.clientSecret,
                                publishableKey = it.publishableKey,
                                stripeAccountId = _state.value.stripeAccountId
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
                                financialConnectionsSessionClientSecret = it.clientSecret,
                                publishableKey = it.publishableKey,
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

    fun onCollectBankAccountForInstantDebitsLauncherResult(
        result: CollectBankAccountForInstantDebitsResult,
    ) {
        viewModelScope.launch {
            when (result) {
                is CollectBankAccountForInstantDebitsResult.Completed -> runCatching {
                    _state.update {
                        it.copy(
                            status = it.status + listOf(
                                "Session Completed! ${result.intent.id} " +
                                    "(account: ${result.bankName} •••• ${result.last4})"
                            )
                        )
                    }
                    confirmIntentIfNeeded(result.intent)
                }.onSuccess {
                    _state.update {
                        it.copy(
                            loading = false,
                            status = it.status + "Completed!"
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            status = it.status + "Failed!: $error"
                        )
                    }
                }

                is CollectBankAccountForInstantDebitsResult.Failed -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            status = it.status + "Failed! ${result.error}"
                        )
                    }
                }

                is CollectBankAccountForInstantDebitsResult.Cancelled -> {
                    _state.update { it.copy(loading = false, status = it.status + "Cancelled!") }
                }
            }
        }
    }

    fun onCollectBankAccountLauncherResult(
        result: CollectBankAccountResult
    ) {
        viewModelScope.launch {
            when (result) {
                is CollectBankAccountResult.Completed -> runCatching {
                    _state.update {
                        val session = result.response.financialConnectionsSession
                        val account = session.accounts.data.firstOrNull()
                        it.copy(
                            status = it.status + listOf(
                                "Session Completed! ${session.id} (account: ${account?.id})"
                            )
                        )
                    }
                    confirmIntentIfNeeded(result.response.intent)
                }.onSuccess {
                    _state.update {
                        it.copy(
                            loading = false,
                            status = it.status + "Completed!"
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            status = it.status + "Failed!: $error"
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
    }

    fun onPaymentSheetResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Canceled -> {
                _state.update { it.copy(loading = false, status = it.status + "Cancelled!") }
            }
            is PaymentSheetResult.Completed -> {
                _state.update {
                    it.copy(
                        status = it.status + listOf(
                            "Elements Session Completed"
                        )
                    )
                }
            }
            is PaymentSheetResult.Failed -> {
                _state.update {
                    it.copy(
                        loading = false,
                        status = it.status + "Failed! ${result.error}"
                    )
                }
            }
        }
    }

    private suspend fun confirmIntentIfNeeded(
        intent: StripeIntent,
    ) {
        val shouldConfirmIntent = state.value.settings.get<ConfirmIntentSetting>().selectedOption
        if (shouldConfirmIntent) {
            val params = ConfirmPaymentIntentParams.create(
                clientSecret = intent.clientSecret!!,
                paymentMethodType = PaymentMethod.Type.USBankAccount
            )
            stripe().confirmPaymentIntent(params)
            _state.update {
                it.copy(status = it.status + "Intent Confirmed!")
            }
        } else {
            _state.update {
                it.copy(status = it.status + "Skipping intent confirmation.")
            }
        }
    }

    private fun stripe() = kotlin.runCatching {
        Stripe(
            getApplication(),
            requireNotNull(_state.value.publishableKey),
            null,
            true,
            emptySet()
        )
    }.onFailure {
        _state.update {
            it.copy(status = it.status + "Failed to create Stripe instance: $it")
        }
    }.getOrThrow()

    fun onSettingsChanged(playgroundSettings: PlaygroundSettings) {
        _state.update {
            it.copy(
                settings = playgroundSettings,
            )
        }
    }

    override fun onCleared() {
        FinancialConnections.clearEventListener()
        super.onCleared()
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

enum class Merchant(
    val apiValue: String,
    val canSwitchBetweenTestAndLive: Boolean = true,
) {
    Default("default"),
    PartnerD("partner_d", canSwitchBetweenTestAndLive = false),
    PartnerF("partner_f", canSwitchBetweenTestAndLive = false),
    PartnerM("partner_m", canSwitchBetweenTestAndLive = false),
    PlatformC("strash"),
    Networking("networking"),
    LiveTesting("live_testing", canSwitchBetweenTestAndLive = false),
    TestMode("testmode", canSwitchBetweenTestAndLive = false),
    Custom("other");

    companion object {
        fun fromApiValue(apiValue: String): Merchant {
            return entries.firstOrNull { it.apiValue == apiValue } ?: Default
        }
    }
}

enum class Flow(val apiValue: String) {
    Data("Data"),
    Token("Token"),
    PaymentIntent("PaymentIntent");

    companion object {
        fun fromApiValue(apiValue: String): Flow = entries.first { it.apiValue == apiValue }
    }
}

enum class IntegrationType(
    val displayName: String,
) {
    Standalone("Standalone"),
    PaymentElement("Payment Element"),
}

enum class Experience(
    val displayName: String,
) {
    FinancialConnections("Financial Connections"),
    InstantDebits("Instant Debits"),
    LinkCardBrand("Link Card Brand");

    val linkMode: String?
        get() = when (this) {
            FinancialConnections -> null
            InstantDebits -> "instant_debits"
            LinkCardBrand -> "link_card_brand"
        }
}

enum class NativeOverride(val apiValue: String) {
    None("none"), Native("native"), Web("web");

    companion object {
        fun fromApiValue(apiValue: String): NativeOverride = entries.first { it.apiValue == apiValue }
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
        val ephemeralKey: String?,
        val customerId: String?,
        val publishableKey: String,
        val experience: Experience,
        val integrationType: IntegrationType,
        val elementsSessionContext: ElementsSessionContext,
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

    val experience: Experience = settings.get<ExperienceSetting>().selectedOption
    val flow: Flow = settings.get<FlowSetting>().selectedOption
    val stripeAccountId: String? = settings.getOrNull<StripeAccountIdSetting>()?.selectedOption
        ?.takeIf { it.isNotEmpty() }
}
