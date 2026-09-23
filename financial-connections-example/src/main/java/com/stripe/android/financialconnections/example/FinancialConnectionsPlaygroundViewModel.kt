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
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.FinancialConnectionsPreCollectedConsent
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetForTokenResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.example.data.BackendRepository
import com.stripe.android.financialconnections.example.data.Settings
import com.stripe.android.financialconnections.example.data.model.AccountHolder
import com.stripe.android.financialconnections.example.data.model.IssuedConsent
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.toCreateAccountHolderBody
import com.stripe.android.financialconnections.example.data.model.toCreateConsentBody
import com.stripe.android.financialconnections.example.settings.ConfirmIntentSetting
import com.stripe.android.financialconnections.example.settings.CustomerIdSetting
import com.stripe.android.financialconnections.example.settings.EmailSetting
import com.stripe.android.financialconnections.example.settings.ExperienceSetting
import com.stripe.android.financialconnections.example.settings.FinancialConnectionsPlaygroundUrlHelper
import com.stripe.android.financialconnections.example.settings.FlowSetting
import com.stripe.android.financialconnections.example.settings.ForceOnelinkConsumerSetting
import com.stripe.android.financialconnections.example.settings.ForceOnelinkSetting
import com.stripe.android.financialconnections.example.settings.IntegrationTypeSetting
import com.stripe.android.financialconnections.example.settings.ManualConsentCollectedAtSetting
import com.stripe.android.financialconnections.example.settings.ManualConsentIdSetting
import com.stripe.android.financialconnections.example.settings.MerchantSetting
import com.stripe.android.financialconnections.example.settings.PlaygroundSettings
import com.stripe.android.financialconnections.example.settings.PreCollectedConsentLocaleSetting
import com.stripe.android.financialconnections.example.settings.PreCollectedConsentMode
import com.stripe.android.financialconnections.example.settings.PreCollectedConsentModeSetting
import com.stripe.android.financialconnections.example.settings.StripeAccountIdSetting
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
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
        syncDebugOverrides(_state.value.settings)
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

        if (launchUri == null) {
            // Only load merchants from the backend if we're not in an end-to-end test,
            // which typically open the sample app with a custom URI.
            loadMerchants()
        }
    }

    private fun loadMerchants() {
        viewModelScope.launch {
            runCatching {
                repository.merchants()
            }.onSuccess { response ->
                _state.update {
                    it.updateWithMerchants(response.merchants)
                }
            }.onFailure { error ->
                Log.e("FinancialConnections", "Failed to fetch merchants from backend", error)
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

        val consentMode = get<PreCollectedConsentModeSetting>().selectedOption.takeIf {
            state.value.experience == Experience.FinancialConnections &&
                (state.value.flow != Flow.PaymentIntent ||
                    get<IntegrationTypeSetting>().selectedOption == IntegrationType.Standalone)
        } ?: PreCollectedConsentMode.Off
        when (consentMode) {
            PreCollectedConsentMode.Off -> launchFinancialConnections(
                settings = this,
                accountHolder = null,
                preCollectedConsent = null,
            )
            PreCollectedConsentMode.Manual -> {
                val consent = get<ManualConsentIdSetting>().selectedOption.trim()
                val collectedAt = get<ManualConsentCollectedAtSetting>().selectedOption.toLongOrNull()
                if (consent.isBlank() || collectedAt == null || collectedAt <= 0) {
                    showError(IllegalArgumentException("Manual Consent ID and a positive Unix timestamp are required."))
                } else {
                    launchFinancialConnections(
                        settings = this,
                        accountHolder = manualAccountHolder(),
                        preCollectedConsent = FinancialConnectionsPreCollectedConsent(consent, collectedAt),
                    )
                }
            }
            PreCollectedConsentMode.Guided -> prepareGuidedConsent(this)
        }
    }

    private fun PlaygroundSettings.manualAccountHolder(): AccountHolder? {
        if (get<FlowSetting>().selectedOption == Flow.Token) {
            return null
        }
        return get<CustomerIdSetting>().selectedOption
            .trim()
            .takeIf(String::isNotBlank)
            ?.let { customerId ->
                AccountHolder(type = "customer", customer = customerId)
            }
    }

    private fun prepareGuidedConsent(settings: PlaygroundSettings) {
        viewModelScope.launch {
            showLoadingWithMessage("Creating account holder and Stripe-issued Consent.")
            runCatching {
                val holderType = if (settings.get<FlowSetting>().selectedOption == Flow.Token) "account" else "customer"
                val request = settings.lasRequest()
                val accountHolder = repository.createAccountHolder(
                    request.toCreateAccountHolderBody(holderType)
                ).accountHolder
                val consent = repository.createConsent(
                    request.toCreateConsentBody(
                        accountHolder = accountHolder,
                        locale = settings.get<PreCollectedConsentLocaleSetting>().selectedOption
                            .trim()
                            .takeIf(String::isNotBlank),
                    )
                )
                PendingPreCollectedConsent(settings, accountHolder, consent)
            }.onSuccess { pendingConsent ->
                _state.update {
                    it.copy(
                        loading = false,
                        pendingPreCollectedConsent = pendingConsent,
                        status = it.status + "Review the Stripe-issued Consent before launching Financial Connections.",
                    )
                }
            }.onFailure(::showError)
        }
    }

    fun onPreCollectedConsentAccepted() {
        val pendingConsent = state.value.pendingPreCollectedConsent ?: return
        _state.update { it.copy(pendingPreCollectedConsent = null) }
        launchFinancialConnections(
            settings = pendingConsent.settings,
            accountHolder = pendingConsent.accountHolder,
            preCollectedConsent = FinancialConnectionsPreCollectedConsent(
                consent = pendingConsent.consent.id,
                collectedAt = System.currentTimeMillis() / MILLIS_PER_SECOND,
            ),
        )
    }

    fun onPreCollectedConsentCancelled() {
        _state.update {
            it.copy(
                loading = false,
                pendingPreCollectedConsent = null,
                status = it.status + "Pre-collected Consent acceptance was cancelled.",
            )
        }
    }

    private fun launchFinancialConnections(
        settings: PlaygroundSettings,
        accountHolder: AccountHolder?,
        preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) {
        when (state.value.experience) {
            Experience.FinancialConnections -> {
                when (state.value.flow) {
                    Flow.Data -> startForData(settings, accountHolder, preCollectedConsent)
                    Flow.Token -> startForToken(settings, accountHolder, preCollectedConsent)
                    Flow.PaymentIntent -> startWithPaymentIntent(
                        settings,
                        experience = Experience.FinancialConnections,
                        accountHolder = accountHolder,
                        preCollectedConsent = preCollectedConsent,
                    )
                    Flow.SetupIntent -> startWithSetupIntent(settings, accountHolder, preCollectedConsent)
                }
            }
            Experience.InstantDebits -> {
                startWithPaymentIntent(settings, Experience.InstantDebits, null, null)
            }
            Experience.LinkCardBrand -> {
                startWithPaymentIntent(settings, Experience.LinkCardBrand, null, null)
            }
        }
    }

    private fun startWithPaymentIntent(
        settings: PlaygroundSettings,
        experience: Experience,
        accountHolder: AccountHolder?,
        preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching {
                repository.createPaymentIntent(
                    settings.paymentIntentRequest(
                        linkMode = experience.linkMode,
                    ).copy(accountHolder = accountHolder)
                )
            }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    _state.update { current ->
                        current.copy(
                            publishableKey = it.publishableKey,
                            intentClientSecret = it.intentSecret,
                            loading = true,
                            status = current.status + buildString {
                                append("Payment Intent created: ${it.intentSecret}")
                                appendLine()
                                append("Opening FinancialConnectionsSheet.")
                            }
                        )
                    }

                    val stripeAccount = settings.getOrNull<StripeAccountIdSetting>()?.selectedOption

                    _viewEffect.emit(
                        FinancialConnectionsPlaygroundViewEffect.OpenForPaymentIntent(
                            paymentIntentSecret = it.intentSecret,
                            publishableKey = it.publishableKey,
                            stripeAccountId = stripeAccount?.takeIf(String::isNotBlank),
                            ephemeralKey = it.ephemeralKey,
                            customerId = it.customerId,
                            elementsSessionContext = ElementsSessionContext(
                                amount = it.amount,
                                currency = it.currency,
                                linkMode = LinkMode.LinkPaymentMethod,
                                billingDetails = ElementsSessionContext.BillingDetails(
                                    email = settings.get<EmailSetting>().selectedOption,
                                ),
                                prefillDetails = ElementsSessionContext.PrefillDetails(
                                    email = settings.get<EmailSetting>().selectedOption,
                                    phone = null,
                                    phoneCountryCode = null,
                                ),
                                allowRedisplay = ElementsSessionContext.AllowRedisplay.Unspecified,
                                incentiveEligibilitySession = null,
                            ),
                            experience = settings.get<ExperienceSetting>().selectedOption,
                            integrationType = settings.get<IntegrationTypeSetting>().selectedOption,
                            preCollectedConsent = preCollectedConsent,
                        )
                    )
                }
                // Error retrieving session: display error.
                .onFailure(::showError)
        }
    }

    private fun startForData(
        settings: PlaygroundSettings,
        accountHolder: AccountHolder?,
        preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching {
                repository.createLinkAccountSession(settings.lasRequest().copy(accountHolder = accountHolder))
            }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    showLoadingWithMessage("Session created, opening FinancialConnectionsSheet.")

                    val stripeAccount = settings.getOrNull<StripeAccountIdSetting>()?.selectedOption

                    _state.update { current -> current.copy(publishableKey = it.publishableKey) }
                    _viewEffect.emit(
                        FinancialConnectionsPlaygroundViewEffect.OpenForData(
                            configuration = FinancialConnectionsSheet.Configuration(
                                financialConnectionsSessionClientSecret = it.clientSecret,
                                publishableKey = it.publishableKey,
                                stripeAccountId = stripeAccount?.takeIf(String::isNotBlank),
                            ),
                            preCollectedConsent = preCollectedConsent,
                        )
                    )
                }
                // Error retrieving session: display error.
                .onFailure(::showError)
        }
    }

    private fun startForToken(
        settings: PlaygroundSettings,
        accountHolder: AccountHolder?,
        preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) {
        viewModelScope.launch {
            showLoadingWithMessage("Fetching link account session from example backend!")
            kotlin.runCatching {
                repository.createLinkAccountSessionForToken(settings.lasRequest().copy(accountHolder = accountHolder))
            }
                // Success creating session: open the financial connections sheet with received secret
                .onSuccess {
                    showLoadingWithMessage("Session created, opening FinancialConnectionsSheet.")

                    val stripeAccount = settings.getOrNull<StripeAccountIdSetting>()?.selectedOption
                    _state.update { current -> current.copy(publishableKey = it.publishableKey) }
                    _viewEffect.emit(
                        FinancialConnectionsPlaygroundViewEffect.OpenForToken(
                            configuration = FinancialConnectionsSheet.Configuration(
                                financialConnectionsSessionClientSecret = it.clientSecret,
                                publishableKey = it.publishableKey,
                                stripeAccountId = stripeAccount?.takeIf(String::isNotBlank),
                            ),
                            preCollectedConsent = preCollectedConsent,
                        )
                    )
                }
                // Error retrieving session: display error.
                .onFailure(::showError)
        }
    }

    private fun startWithSetupIntent(
        settings: PlaygroundSettings,
        accountHolder: AccountHolder?,
        preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) {
        viewModelScope.launch {
            showLoadingWithMessage("Creating SetupIntent from example backend.")
            runCatching {
                repository.createSetupIntent(
                    settings.paymentIntentRequest().copy(accountHolder = accountHolder)
                )
            }.onSuccess {
                _state.update { current ->
                    current.copy(
                        publishableKey = it.publishableKey,
                        intentClientSecret = it.intentSecret,
                        loading = true,
                        status = current.status + "SetupIntent created, opening FinancialConnectionsSheet.",
                    )
                }
                val stripeAccount = settings.getOrNull<StripeAccountIdSetting>()?.selectedOption
                _viewEffect.emit(
                    FinancialConnectionsPlaygroundViewEffect.OpenForSetupIntent(
                        setupIntentSecret = it.intentSecret,
                        publishableKey = it.publishableKey,
                        stripeAccountId = stripeAccount?.takeIf(String::isNotBlank),
                        preCollectedConsent = preCollectedConsent,
                    )
                )
            }.onFailure(::showError)
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
                                "Session Completed! ${result.intent?.id} " +
                                    "(account: ${result.bankName} •••• ${result.last4})"
                            )
                        )
                    }
                    confirmIntentIfNeeded()
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
                    confirmIntentIfNeeded()
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

    private suspend fun confirmIntentIfNeeded() {
        val shouldConfirmIntent = state.value.settings.get<ConfirmIntentSetting>().selectedOption &&
            state.value.flow == Flow.PaymentIntent
        val clientSecret = state.value.intentClientSecret

        if (shouldConfirmIntent && clientSecret != null) {
            val params = ConfirmPaymentIntentParams.create(
                clientSecret = clientSecret,
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
        syncDebugOverrides(playgroundSettings)
        _state.update {
            it.copy(
                settings = playgroundSettings,
            )
        }
    }

    private fun syncDebugOverrides(playgroundSettings: PlaygroundSettings) {
        // FC standalone flows read this setting from DebugConfiguration, but PaymentSheet-driven
        // Link surfaces in this app still rely on the global feature flag override.
        FeatureFlags.forceOnelink.setEnabled(
            playgroundSettings.get<ForceOnelinkSetting>().selectedOption
        )
        FeatureFlags.forceOnelinkConsumer.setEnabled(
            playgroundSettings.get<ForceOnelinkConsumerSetting>().selectedOption
        )
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

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}

enum class Flow(val apiValue: String) {
    Data("Data"),
    Token("Token"),
    PaymentIntent("PaymentIntent"),
    SetupIntent("SetupIntent");

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
        val configuration: FinancialConnectionsSheet.Configuration,
        val preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) : FinancialConnectionsPlaygroundViewEffect()

    data class OpenForToken(
        val configuration: FinancialConnectionsSheet.Configuration,
        val preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) : FinancialConnectionsPlaygroundViewEffect()

    data class OpenForPaymentIntent(
        val paymentIntentSecret: String,
        val ephemeralKey: String?,
        val customerId: String?,
        val publishableKey: String,
        val stripeAccountId: String?,
        val experience: Experience,
        val integrationType: IntegrationType,
        val elementsSessionContext: ElementsSessionContext,
        val preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) : FinancialConnectionsPlaygroundViewEffect()

    data class OpenForSetupIntent(
        val setupIntentSecret: String,
        val publishableKey: String,
        val stripeAccountId: String?,
        val preCollectedConsent: FinancialConnectionsPreCollectedConsent?,
    ) : FinancialConnectionsPlaygroundViewEffect()
}

internal data class PendingPreCollectedConsent(
    val settings: PlaygroundSettings,
    val accountHolder: AccountHolder,
    val consent: IssuedConsent,
)

internal data class FinancialConnectionsPlaygroundState(
    val backendUrl: String = "",
    val settings: PlaygroundSettings,
    val loading: Boolean = false,
    val publishableKey: String? = null,
    val intentClientSecret: String? = null,
    val pendingPreCollectedConsent: PendingPreCollectedConsent? = null,
    val status: List<String> = emptyList(),
    val emittedEvents: List<String> = emptyList()
) {

    constructor(application: Application, launchUri: Uri?) : this(
        settings = FinancialConnectionsPlaygroundUrlHelper.settingsFromUri(launchUri)
            ?: PlaygroundSettings.createFromSharedPreferences(application)
    )

    val experience: Experience = settings.get<ExperienceSetting>().selectedOption
    val flow: Flow = settings.get<FlowSetting>().selectedOption

    fun updateWithMerchants(merchants: List<Merchant>): FinancialConnectionsPlaygroundState {
        return copy(
            settings = settings.copy(
                settings = settings.settings.map { setting ->
                    if (setting is MerchantSetting) {
                        setting.updateWithMerchants(merchants)
                    } else {
                        setting
                    }
                }
            )
        )
    }
}
