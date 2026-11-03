package com.stripe.android.crypto.onramp

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService
import com.stripe.android.crypto.onramp.exception.OnrampErrorLogger
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampConfigurationResult
import com.stripe.android.crypto.onramp.model.PaymentMethodSelection
import com.stripe.android.crypto.onramp.model.RefreshKycInfo
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayLauncher
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayPresentation
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayResult
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayStatus
import com.stripe.android.crypto.onramp.ui.KycRefreshScreenAction
import com.stripe.android.crypto.onramp.ui.VerifyKycActivityResult
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkController
import com.stripe.android.model.DateOfBirth
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against real Android lifecycle and activity-result registries, with held backend responses. */
@RunWith(TestParameterInjector::class)
internal class OnrampPresenterLifecycleTest {
    @get:Rule
    val networkRule = NetworkRule()

    @Test
    fun latePresentationDoesNotUseDestroyedHost(@TestParameter operation: Presentation) = runBlocking {
        runScenario { scenario ->
            withContext(Dispatchers.Main) { operation.start(scenario.presenter) }
            assertThat(scenario.network.requests.awaitItem()).isEqualTo(operation.requestPath)
            scenario.closeHost()
            scenario.samsung.destroyCalls.awaitItem()
            scenario.network.release.complete(Unit)
            scenario.network.responses.awaitItem()
            // Run after the response continuation on the main thread, including its launch/callback work.
            withContext(Dispatchers.Main) { Unit }
            scenario.errors.expectNoEvents()
            scenario.network.requests.expectNoEvents()
            scenario.samsung.presentCalls.expectNoEvents()
            scenario.oldResults.expectNoEvents()
        }
    }

    @Test
    fun lateResultUsesReplacementCallback(@TestParameter operation: ResultOperation) = runBlocking {
        runScenario { scenario ->
            withContext(Dispatchers.Main) { operation.deliver(scenario.presenter) }
            assertThat(scenario.network.requests.awaitItem()).isEqualTo(operation.requestPath)
            scenario.closeHost()
            scenario.samsung.destroyCalls.awaitItem()
            scenario.openHost(scenario.newResults)
            scenario.network.release.complete(Unit)
            scenario.network.responses.awaitItem()
            withContext(Dispatchers.Main) { Unit }
            scenario.oldResults.expectNoEvents()
            assertThat(scenario.newResults.awaitItem()).isEqualTo(operation.callbackName)
            scenario.errors.expectNoEvents()
            if (operation == ResultOperation.SamsungPay) {
                assertThat(scenario.warnings.awaitItem()).contains("Test credential rejected")
            }
        }
    }

    private suspend fun runScenario(block: suspend (Scenario) -> Unit) {
        val scenario = Scenario()
        try {
            withTimeout(30_000) {
                scenario.configure(networkRule)
                scenario.openHost(scenario.oldResults)
                block(scenario)
                if (scenario.closeHost()) {
                    scenario.samsung.destroyCalls.awaitItem()
                }
                scenario.network.ensureAllEventsConsumed()
                scenario.warnings.ensureAllEventsConsumed()
                scenario.errors.ensureAllEventsConsumed()
                scenario.oldResults.ensureAllEventsConsumed()
                scenario.newResults.ensureAllEventsConsumed()
                scenario.readiness.ensureAllEventsConsumed()
                scenario.availabilityCalls.ensureAllEventsConsumed()
                scenario.samsung.presentCalls.ensureAllEventsConsumed()
                scenario.samsung.destroyCalls.ensureAllEventsConsumed()
            }
        } finally {
            scenario.network.release.complete(Unit)
            try {
                scenario.closeHost()
            } finally {
                scenario.scope.cancel()
                GooglePayRepository.resetFactory()
            }
        }
    }

    enum class Presentation(val requestPath: String) {
        Identity("start_identity_verification"),
        KycVerification("kyc_data_retrieve"),
        Attestation("crs_carf_declaration"),
        KycRequirements("kyc_requirements"),
        GooglePay("platform_settings"),
        SamsungPay("platform_settings");

        fun start(presenter: OnrampPresenterCoordinator) {
            when (this) {
                Identity -> presenter.verifyIdentity()
                KycVerification -> presenter.verifyKycInfo()
                Attestation -> presenter.presentUserAttestation()
                KycRequirements -> presenter.fulfillKycRequirements()
                GooglePay -> presenter.collectPaymentMethod(PaymentMethodSelection.GooglePay("usd", 1099))
                SamsungPay -> presenter.collectPaymentMethod(PaymentMethodSelection.SamsungPay("usd", 1099, "order"))
            }
        }
    }

    enum class ResultOperation(val callbackName: String, val requestPath: String) {
        Authorization("authorize", "customers"),
        KycConfirmation("kyc", "refresh_consumer_person"),
        SamsungPay("payment", "tokens");

        fun deliver(presenter: OnrampPresenterCoordinator) {
            when (this) {
                Authorization -> presenter.handleAuthorizeResult(LinkController.AuthorizeResult.Consented)
                KycConfirmation -> presenter.handleVerifyKycResult(
                    VerifyKycActivityResult(
                        KycRefreshScreenAction.Confirm(
                            RefreshKycInfo(
                                firstName = "Test",
                                lastName = "User",
                                idNumberLastFour = "0000",
                                idType = "social_security_number",
                                dateOfBirth = DateOfBirth(1, 1, 1990),
                                address = PaymentSheet.Address(country = "US"),
                            )
                        )
                    )
                )
                SamsungPay -> presenter.handleSamsungPayPaymentSelection(
                    SamsungPayResult.Completed(paymentCredential = "test_credential", kycInfo = null),
                    "pk_test_platform",
                )
            }
        }
    }

    private class Scenario {
        val network = FakeLifecycleNetworkClient()
        val warnings = Turbine<String>()
        val errors = Turbine<Throwable>()
        val oldResults = Turbine<String>()
        val newResults = Turbine<String>()
        val readiness = Turbine<Boolean>()
        val availabilityCalls = Turbine<String>()
        val samsung = FakeLifecycleSamsungPayLauncher()
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, error -> errors.add(error) }
        )
        private val store = OnrampCallbackStore()
        private val application = ApplicationProvider.getApplicationContext<Application>()
        private val linkController = LinkController.create(application, SavedStateHandle())
        val interactor = OnrampInteractor(
            application = application,
            linkController = linkController,
            cryptoApiRepository = CryptoApiRepository(
                stripeNetworkClient = network,
                stripeRepository = FakeLifecycleStripeRepository(network),
                linkController = linkController,
                apiConfigProvider = { ApiConfiguration.State("pk_test_lifecycle", null) },
                apiVersion = CryptoApiRepository.CRYPTO_ONRAMP_API_VERSION,
                appInfo = null,
            ),
            analyticsServiceFactory = object : OnrampAnalyticsService.Factory {
                override fun create(elementsSessionId: String): OnrampAnalyticsService =
                    error("No analytics session configured by this test")
            },
            errorLogger = OnrampErrorLogger(object : UserFacingLogger {
                override fun logWarningWithoutPii(message: String) = warnings.add(message)
            }),
            checkoutHandler = { "cos_test_secret" },
            savedStateHandle = SavedStateHandle(),
        )
        private var host: ActivityScenario<ComponentActivity>? = null
        lateinit var presenter: OnrampPresenterCoordinator

        init {
            PaymentConfiguration.init(application, "pk_test_lifecycle")
            GooglePayRepository.googlePayAvailabilityClientFactory = object : GooglePayAvailabilityClient.Factory {
                override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
                    availabilityCalls.add("create")
                    return object : GooglePayAvailabilityClient {
                        override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
                            availabilityCalls.add("ready")
                            return true
                        }
                    }
                }
            }
        }

        suspend fun configure(networkRule: NetworkRule) {
            // Only the onramp configuration is needed here; don't initialize a real Link session.
            networkRule.enqueue(
                path("/v1/elements/sessions"),
                header("Authorization", "Bearer pk_test_lifecycle"),
                applyDefaultAuthorization = false,
            ) { response ->
                response.setResponseCode(400).setBody(
                    """{"error":{
                        "type":"invalid_request_error",
                        "message":"Link is not configured for this lifecycle test"
                    }}"""
                )
            }
            val result = interactor.configure(
                OnrampConfiguration()
                    .merchantDisplayName("Lifecycle test")
                    .publishableKey("pk_test_lifecycle")
                    .googlePayConfig(
                        GooglePayPaymentMethodLauncher.Config(
                            GooglePayEnvironment.Test, "US", "Lifecycle test"
                        )
                    )
                    .build()
            )
            assertThat(result).isInstanceOf(OnrampConfigurationResult.Failed::class.java)
            assertThat(warnings.awaitItem()).contains("configure")
            assertThat(availabilityCalls.awaitItem()).isEqualTo("create")
            assertThat(availabilityCalls.awaitItem()).isEqualTo("ready")
        }

        suspend fun openHost(results: Turbine<String>) {
            store.update(OnrampCallbacks()
                .checkoutCallback { results.add("checkout") }
                .verifyIdentityCallback { results.add("identity") }
                .collectPaymentCallback { results.add("payment") }
                .authorizeCallback { results.add("authorize") }
                .verifyKycCallback { results.add("kyc") }
                .kycCallback { results.add("requirements") }
                .userAttestationCallback { results.add("attestation") }
                .googlePayIsReadyCallback { readiness.add(it) }
                .onrampSessionClientSecretProvider { "cos_test_secret" }
                .build())
            host = ActivityScenario.launch(ComponentActivity::class.java).also { scenario ->
                scenario.onActivity { activity ->
                    presenter = OnrampPresenterCoordinator(
                        interactor = interactor,
                        linkController = linkController,
                        lifecycleOwner = activity,
                        activity = activity,
                        coroutineScope = scope,
                        onrampCallbackIdentifier = "lifecycle_test",
                        callbackStore = store,
                        samsungPayLauncherFactory = object : SamsungPayLauncher.Factory {
                            override fun create(
                                context: Context,
                                configuration: OnrampConfiguration.SamsungPayConfig,
                                merchantDisplayName: String,
                            ): SamsungPayLauncher = error("Samsung configuration is supplied directly by this test")
                        },
                    )
                    presenter.samsungPayLauncher = samsung
                }
            }
            assertThat(availabilityCalls.awaitItem()).isEqualTo("create")
            assertThat(availabilityCalls.awaitItem()).isEqualTo("ready")
            assertThat(readiness.awaitItem()).isTrue()
            withContext(Dispatchers.Main) {
                interactor.onLinkControllerState(
                    LinkController.State(
                        internalLinkAccount = LinkController.LinkAccount(
                            email = "test@example.com",
                            redactedPhoneNumber = "***-***-1234",
                            sessionState = LinkController.SessionState.LoggedIn,
                            consumerSessionClientSecret = "secret_test",
                            linkSessionKey = "lsk_test",
                        )
                    )
                )
            }
        }

        fun closeHost(): Boolean {
            val current = host ?: return false
            host = null
            current.close()
            return true
        }
    }
}

internal class FakeLifecycleSamsungPayLauncher : SamsungPayLauncher {
    val presentCalls = Turbine<SamsungPayPresentation>()
    val destroyCalls = Turbine<Unit>()

    override fun getStatus(callback: (SamsungPayStatus) -> Unit) = error("Unexpected status request")

    override fun present(presentation: SamsungPayPresentation, callback: (SamsungPayResult) -> Unit) {
        presentCalls.add(presentation)
    }

    override fun destroy() {
        destroyCalls.add(Unit)
    }
}
