package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.GooglePayConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FeatureFlagTestRule
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class DefaultGooglePayRepositoryFactoryTest {
    @get:Rule
    val allowNoExistingPaymentMethodForGooglePayRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.allowNoExistingPaymentMethodForGooglePay,
        isEnabled = false,
    )

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val googlePayConfig = GooglePayConfig(
        publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        connectedAccountId = ApiKeyFixtures.FAKE_STRIPE_ACCOUNT,
    )
    private val requests = Turbine<IsReadyToPayRequest>()

    @Before
    fun setup() {
        PaymentConfiguration.init(
            context = context,
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            stripeAccountId = null,
        )
        GooglePayRepository.googlePayAvailabilityClientFactory =
            object : GooglePayAvailabilityClient.Factory {
                override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
                    return object : GooglePayAvailabilityClient {
                        override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
                            requests.add(request)
                            return true
                        }
                    }
                }
            }
    }

    @After
    fun tearDown() {
        PaymentConfiguration.clearInstance()
        GooglePayRepository.resetFactory()
    }

    @Test
    fun `when allowNoExistingPaymentMethodForGooglePay is disabled, existingPaymentMethodRequired should be true`() =
        runScenario(allowNoExistingPaymentMethodForGooglePay = false, googlePayConfig = googlePayConfig) {
            val request = readyRequest()
            assertThat(request.getBoolean("existingPaymentMethodRequired")).isTrue()
            assertThat(tokenizationPublishableKey(request))
                .isEqualTo("${ApiKeyFixtures.FAKE_PUBLISHABLE_KEY}/${ApiKeyFixtures.FAKE_STRIPE_ACCOUNT}")
        }

    @Test
    fun `when allowNoExistingPaymentMethodForGooglePay is enabled, existingPaymentMethodRequired should be false`() =
        runScenario(allowNoExistingPaymentMethodForGooglePay = true, googlePayConfig = googlePayConfig) {
            assertThat(readyRequest().getBoolean("existingPaymentMethodRequired")).isFalse()
        }

    @Test
    fun `uses credentials from GooglePayConfig constructed with PaymentConfiguration`() =
        runScenario(allowNoExistingPaymentMethodForGooglePay = false, googlePayConfig = GooglePayConfig(context)) {
            val request = readyRequest()
            assertThat(request.getBoolean("existingPaymentMethodRequired")).isTrue()
            assertThat(tokenizationPublishableKey(request))
                .isEqualTo(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        }

    private fun runScenario(
        allowNoExistingPaymentMethodForGooglePay: Boolean,
        googlePayConfig: GooglePayConfig,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        allowNoExistingPaymentMethodForGooglePayRule.setEnabled(allowNoExistingPaymentMethodForGooglePay)

        val factory = DefaultGooglePayRepositoryFactory(
            appContext = context,
            logger = Logger.noop(),
            errorReporter = FakeErrorReporter(),
        )
        val repository = factory(
            environment = GooglePayEnvironment.Test,
            cardFundingFilter = DefaultCardFundingFilter,
            cardBrandFilter = DefaultCardBrandFilter,
            googlePayConfig = googlePayConfig,
        )

        Scenario(
            repository = repository,
        ).block()

        requests.ensureAllEventsConsumed()
    }

    private inner class Scenario(
        private val repository: GooglePayRepository,
    ) {
        suspend fun readyRequest(): JSONObject {
            repository.isReady().test {
                assertThat(awaitItem()).isTrue()
                awaitComplete()
            }

            return JSONObject(requests.awaitItem().toJson())
        }

        fun tokenizationPublishableKey(request: JSONObject): String {
            return request
                .getJSONArray("allowedPaymentMethods")
                .getJSONObject(0)
                .getJSONObject("tokenizationSpecification")
                .getJSONObject("parameters")
                .getString("stripe:publishableKey")
        }
    }
}
