package com.stripe.android.paymentsheet.repositories

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkController
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.MobileSession
import com.stripe.android.paymentsheet.state.ElementsSessionLoader
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FeatureFlagTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class ElementsSessionMobileSessionTest {
    @get:Rule
    val featureFlag = FeatureFlagTestRule(FeatureFlags.serverPaymentMethodAvailabilityEnabled, isEnabled = true)

    @get:Rule
    val financialConnectionsFlag = FeatureFlagTestRule(
        FeatureFlags.financialConnectionsFullSdkUnavailable,
        isEnabled = false,
    )

    @Test
    fun `PaymentSheet loads send the config header and retain the decoded sidecar`() = runScenario {
        val header = JSONObject(requireNotNull(request.headers[MobileSession.CONFIG_HEADER_NAME]))
        assertThat(header.getJSONObject("payment_element_config").getBoolean("has_return_url")).isTrue()
        assertThat(header.getJSONObject("environment").getBoolean("device_supports_native_link")).isTrue()
        assertThat(request.headers["Authorization"]).isEqualTo("Bearer ${DEFAULT_API_CONFIG.publishableKey}")
        assertThat(request.params).doesNotContainKey("mobile_session")
        assertThat(session.mobileSession?.paymentMethodAvailability?.paymentMethodTypes)
            .containsExactly("klarna", "card").inOrder()
        assertThat(session.mobileSession?.paymentMethodAvailability?.linkEnabled).isTrue()
        assertThat(session.mobileSession?.logMessages).containsExactly("A payment method was unavailable.")
        // The sidecar does not replace client payment method selection in this stage.
        assertThat(session.stripeIntent.paymentMethodTypes.first()).isEqualTo("card")
    }

    @Test
    fun `SetupIntent loads send the config header`() = runScenario(
        initializationMode = PaymentElementLoader.InitializationMode.SetupIntent("seti_123_secret_123"),
        response = ElementsSessionFixtures.EXPANDED_SETUP_INTENT_JSON,
    ) {
        assertThat(request.headers).containsKey(MobileSession.CONFIG_HEADER_NAME)
        assertThat(request.params?.get("type")).isEqualTo("setup_intent")
        assertThat(session.mobileSession?.paymentMethodAvailability).isNotNull()
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
        assertThat(session.mobileSession?.paymentMethodAvailability).isNotNull()
    }

    @Test
    fun `Embedded loads send their configuration in the header`() = runScenario(
        configuration = PaymentElementLoader.Configuration.Embedded(
            isRowSelectionImmediateAction = true,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Merchant")
                .allowsDelayedPaymentMethods(true)
                .paymentMethodOrder(listOf("klarna", "card"))
                .build(),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        val config = JSONObject(requireNotNull(request.headers[MobileSession.CONFIG_HEADER_NAME]))
            .getJSONObject("payment_element_config")
        assertThat(config.getBoolean("allows_delayed_payment_methods")).isTrue()
        assertThat(config.getJSONArray("payment_method_order").getString(0)).isEqualTo("klarna")
    }

    @Test
    fun `flag off omits the header and ignores even malformed sidecars`() = runScenario(
        flagEnabled = false,
        mobileSession = "invalid sidecar",
    ) {
        assertThat(request.headers).doesNotContainKey(MobileSession.CONFIG_HEADER_NAME)
        assertThat(session.mobileSession).isNull()
    }

    @Test
    fun `callers without payment element configuration omit the header`() = runScenario(configuration = null) {
        assertThat(request.headers).doesNotContainKey(MobileSession.CONFIG_HEADER_NAME)
    }

    @Test
    fun `standalone Link omits the header`() = runScenario(
        configuration = PaymentElementLoader.Configuration.StandaloneLink(LINK_CONFIGURATION),
    ) {
        assertThat(request.headers).doesNotContainKey(MobileSession.CONFIG_HEADER_NAME)
    }

    @Test
    fun `crypto onramp omits the header`() = runScenario(
        configuration = PaymentElementLoader.Configuration.CryptoOnramp(LINK_CONFIGURATION),
    ) {
        assertThat(request.headers).doesNotContainKey(MobileSession.CONFIG_HEADER_NAME)
    }

    @Test
    fun `express checkout omits the header`() = runScenario(
        configuration = PaymentElementLoader.Configuration.ExpressCheckoutElement(
            DEFAULT_CONFIGURATION.commonConfiguration,
        ),
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

    @Test
    fun `absent sidecar leaves the session usable without an error`() = runScenario(mobileSession = null) {
        assertThat(session.mobileSession).isNull()
        assertThat(session.stripeIntent.id).isNotEmpty()
    }

    @Test
    fun `null sidecar leaves the session usable without an error`() = runScenario(mobileSession = JSONObject.NULL) {
        assertThat(session.mobileSession).isNull()
    }

    @Test
    fun `empty sidecar decodes with no availability`() = runScenario(mobileSession = JSONObject()) {
        assertThat(session.mobileSession).isNotNull()
        assertThat(session.mobileSession?.paymentMethodAvailability).isNull()
        assertThat(session.mobileSession?.logMessages).isNull()
    }

    @Test
    fun `unknown sidecar fields do not prevent decoding`() = runScenario(
        mobileSession = validSidecar().put("future_field", "future_value"),
    ) {
        assertThat(session.mobileSession?.paymentMethodAvailability?.linkEnabled).isTrue()
    }

    @Test
    fun `malformed availability is reported without failing the session`() = runScenario(
        mobileSession = JSONObject("""{"payment_method_availability":{"payment_method_types":"card"}}"""),
    ) {
        assertDecodeFailure()
    }

    @Test
    fun `string sidecar is reported even when it contains valid JSON`() = runScenario(mobileSession = "{}") {
        assertDecodeFailure()
    }

    @Test
    fun `array sidecar is reported without failing the session`() = runScenario(mobileSession = JSONArray()) {
        assertDecodeFailure()
    }

    private fun runScenario(
        configuration: PaymentElementLoader.Configuration? = DEFAULT_CONFIGURATION,
        initializationMode: PaymentElementLoader.InitializationMode =
            PaymentElementLoader.InitializationMode.PaymentIntent("pi_123_secret_123"),
        response: JSONObject = ElementsSessionFixtures.EXPANDED_PAYMENT_INTENT_JSON,
        mobileSession: Any? = validSidecar(),
        flagEnabled: Boolean = true,
        fullSdkUnavailable: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        featureFlag.setEnabled(flagEnabled)
        financialConnectionsFlag.setEnabled(fullSdkUnavailable)
        val networkClient = FakeNetworkClient(
            JSONObject(response.toString()).put("mobile_session", mobileSession).toString(),
        )
        val errorReporter = FakeErrorReporter()
        val repository = RealElementsSessionRepository(
            application = ApplicationProvider.getApplicationContext(),
            stripeNetworkClient = networkClient,
            stripeRepository = object : AbsFakeStripeRepository() {},
            workContext = UnconfinedTestDispatcher(testScheduler),
            clientParams = ElementsSessionClientParams("com.example") { "session_123" },
            errorReporter = errorReporter,
        )
        val session = if (configuration != null) {
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
        Scenario(session, networkClient.requests.awaitItem() as ApiRequest, errorReporter).block()
        networkClient.requests.ensureAllEventsConsumed()
        errorReporter.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val session: ElementsSession,
        val request: ApiRequest,
        val errorReporter: FakeErrorReporter,
    ) {
        suspend fun assertDecodeFailure() {
            assertThat(session.mobileSession).isNull()
            assertThat(session.stripeIntent.id).isNotEmpty()
            val call = errorReporter.awaitCall()
            assertThat(call.errorEvent.eventName).isEqualTo("mc_mobile_session_decode_failed")
            assertThat(call.errorEvent).isEqualTo(
                ErrorReporter.ExpectedErrorEvent.PAYMENT_SHEET_MOBILE_SESSION_DECODE_FAILED,
            )
            assertThat(call.stripeException).isNotNull()
        }
    }

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
        val DEFAULT_CONFIGURATION = PaymentElementLoader.Configuration.PaymentSheet(
            PaymentSheet.Configuration("Merchant"),
        )
        val LINK_CONFIGURATION = LinkController.Configuration(
            merchantDisplayName = "Merchant",
            publishableKey = DEFAULT_API_CONFIG.publishableKey,
        ).build()

        fun validSidecar() = JSONObject(
            """
            {
                "payment_method_availability": {
                    "payment_method_types": ["klarna", "card"],
                    "link_enabled": true
                },
                "log_messages": ["A payment method was unavailable."]
            }
            """.trimIndent()
        )
    }
}
