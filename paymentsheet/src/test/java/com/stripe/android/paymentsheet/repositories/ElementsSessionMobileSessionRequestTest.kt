package com.stripe.android.paymentsheet.repositories

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkController
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.model.ElementsSessionFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.MobileSession
import com.stripe.android.paymentsheet.state.ElementsSessionLoader
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FeatureFlagTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class ElementsSessionMobileSessionRequestTest {
    @get:Rule
    val featureFlag = FeatureFlagTestRule(FeatureFlags.serverPaymentMethodAvailabilityEnabled, isEnabled = true)

    @get:Rule
    val financialConnectionsFlag = FeatureFlagTestRule(
        FeatureFlags.financialConnectionsFullSdkUnavailable,
        isEnabled = false,
    )

    @Test
    fun `PaymentSheet loads send the config header`() = runScenario {
        val header = JSONObject(requireNotNull(request.headers[MobileSession.CONFIG_HEADER_NAME]))
        assertThat(header.getJSONObject("payment_element_config").getBoolean("has_return_url")).isTrue()
        assertThat(header.getJSONObject("environment").getBoolean("device_supports_native_link")).isTrue()
        assertThat(request.headers["Authorization"]).isEqualTo("Bearer ${DEFAULT_API_CONFIG.publishableKey}")
        assertThat(request.params).doesNotContainKey("mobile_session")
    }

    @Test
    fun `SetupIntent loads send the config header`() = runScenario(
        initializationMode = PaymentElementLoader.InitializationMode.SetupIntent("seti_123_secret_123"),
        response = ElementsSessionFixtures.EXPANDED_SETUP_INTENT_JSON,
    ) {
        assertThat(request.headers).containsKey(MobileSession.CONFIG_HEADER_NAME)
        assertThat(request.params?.get("type")).isEqualTo("setup_intent")
    }

    @Test
    fun `deferred intent loads send the config header`() = runScenario(
        initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            PaymentSheet.IntentConfiguration(PaymentSheet.IntentConfiguration.Mode.Payment(1000, "usd")),
        ),
        response = ElementsSessionFixtures.DEFERRED_INTENT_JSON,
    ) {
        assertThat(request.headers).containsKey(MobileSession.CONFIG_HEADER_NAME)
        assertThat(request.params?.get("type")).isEqualTo("deferred_intent")
    }

    @Test
    fun `Embedded loads send their configuration in the header`() = runScenario(
        configuration = EmbeddedPaymentElement.Configuration.Builder("Merchant")
            .allowsDelayedPaymentMethods(true)
            .paymentMethodOrder(listOf("klarna", "card"))
            .build()
            .asCommonConfiguration(),
    ) {
        val config = JSONObject(requireNotNull(request.headers[MobileSession.CONFIG_HEADER_NAME]))
            .getJSONObject("payment_element_config")
        assertThat(config.getBoolean("allows_delayed_payment_methods")).isTrue()
        assertThat(config.getJSONArray("payment_method_order").getString(0)).isEqualTo("klarna")
    }

    @Test
    fun `flag off omits the header`() {
        featureFlag.setEnabled(false)

        runScenario {
            assertThat(request.headers).doesNotContainKey(MobileSession.CONFIG_HEADER_NAME)
        }
    }

    @Test
    fun `request uses the decision captured in common configuration`() {
        val configuration = defaultConfiguration()
        featureFlag.setEnabled(false)

        runScenario(configuration = configuration) {
            assertThat(request.headers).containsKey(MobileSession.CONFIG_HEADER_NAME)
        }
    }

    @Test
    fun `callers without payment element configuration omit the header`() = runScenario(configuration = null) {
        assertThat(request.headers).doesNotContainKey(MobileSession.CONFIG_HEADER_NAME)
    }

    @Test
    fun `standalone Link omits the header`() = runScenario(
        configuration = PaymentElementLoader.Configuration.StandaloneLink(LINK_CONFIGURATION).commonConfiguration,
    ) {
        assertThat(request.headers).doesNotContainKey(MobileSession.CONFIG_HEADER_NAME)
    }

    @Test
    fun `crypto onramp omits the header`() = runScenario(
        configuration = PaymentElementLoader.Configuration.CryptoOnramp(LINK_CONFIGURATION).commonConfiguration,
    ) {
        assertThat(request.headers).doesNotContainKey(MobileSession.CONFIG_HEADER_NAME)
    }

    @Test
    fun `common configuration can opt out of MobileSession`() = runScenario(
        configuration = defaultConfiguration().copy(useRemoteMobileSession = false),
    ) {
        assertThat(request.headers).doesNotContainKey(MobileSession.CONFIG_HEADER_NAME)
    }

    @Test
    fun `full Financial Connections unavailability is reflected in the header`() = runScenario(
        fullSdkUnavailable = true,
    ) {
        val config = JSONObject(requireNotNull(request.headers[MobileSession.CONFIG_HEADER_NAME]))
        assertThat(config.getJSONObject("environment").getBoolean("has_financial_connections_sdk")).isFalse()
    }

    private fun runScenario(
        configuration: CommonConfiguration? = defaultConfiguration(),
        initializationMode: PaymentElementLoader.InitializationMode =
            PaymentElementLoader.InitializationMode.PaymentIntent("pi_123_secret_123"),
        response: JSONObject = ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON,
        fullSdkUnavailable: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        financialConnectionsFlag.setEnabled(fullSdkUnavailable)
        val networkClient = FakeNetworkClient(response.toString())
        val repository = RealElementsSessionRepository(
            application = ApplicationProvider.getApplicationContext(),
            stripeNetworkClient = networkClient,
            stripeRepository = object : AbsFakeStripeRepository() {},
            workContext = UnconfinedTestDispatcher(testScheduler),
            clientParams = ElementsSessionClientParams("com.example") { "session_123" },
        )
        if (configuration != null) {
            ElementsSessionLoader(repository)(
                initializationMode = initializationMode,
                configuration = configuration,
                savedPaymentMethodSelection = null,
                apiConfiguration = DEFAULT_API_CONFIG,
            )
        } else {
            repository.get(
                initializationMode = initializationMode,
                customer = null,
                customPaymentMethods = emptyList(),
                externalPaymentMethods = emptyList(),
                savedPaymentMethodSelectionId = null,
                countryOverride = null,
                apiConfiguration = DEFAULT_API_CONFIG,
                mobileSessionConfig = null,
            ).getOrThrow()
        }
        Scenario(networkClient.requests.awaitItem() as ApiRequest).block()
        networkClient.requests.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val request: ApiRequest,
    )

    private class FakeNetworkClient(private val response: String) : StripeNetworkClient {
        val requests = Turbine<StripeRequest>()

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            requests.add(request)
            return StripeResponse(200, response, emptyMap())
        }

        override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> {
            error("Unexpected file request")
        }
    }

    private companion object {
        fun defaultConfiguration() = PaymentSheet.Configuration("Merchant").asCommonConfiguration()
        val LINK_CONFIGURATION = LinkController.Configuration(
            merchantDisplayName = "Merchant",
            publishableKey = DEFAULT_API_CONFIG.publishableKey,
        ).build()
    }
}
