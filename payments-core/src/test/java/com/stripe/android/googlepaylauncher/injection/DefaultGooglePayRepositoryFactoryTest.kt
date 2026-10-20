package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
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
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class DefaultGooglePayRepositoryFactoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val googlePayConfig = GooglePayConfig(
        publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        connectedAccountId = ApiKeyFixtures.FAKE_STRIPE_ACCOUNT,
    )

    @get:Rule
    val paymentConfigTestRule = PaymentConfigurationTestRule(
        context,
        "pk_payment_configuration",
        "acct_payment_configuration"
    )

    @Test
    fun `test environment does not require an existing payment method`() =
        runScenario(
            environment = GooglePayEnvironment.Test,
            googlePayConfigProvider = { googlePayConfig },
        ) {
            assertThat(readyRequest().getBoolean("existingPaymentMethodRequired")).isFalse()
        }

    @Test
    fun `production environment requires an existing payment method`() =
        runScenario(
            environment = GooglePayEnvironment.Production,
            googlePayConfigProvider = { googlePayConfig },
        ) {
            assertThat(readyRequest().getBoolean("existingPaymentMethodRequired")).isTrue()
        }

    @Test
    fun `uses credentials from GooglePayConfig constructed with PaymentConfiguration`() =
        runScenario(
            environment = GooglePayEnvironment.Test,
            googlePayConfigProvider = { GooglePayConfig(context) },
        ) {
            val request = readyRequest()
            assertThat(tokenizationPublishableKey(request))
                .isEqualTo("pk_payment_configuration/acct_payment_configuration")
        }

    @Test
    fun `uses credentials from GooglePayConfig constructed with explicit credentials`() =
        runScenario(
            environment = GooglePayEnvironment.Test,
            googlePayConfigProvider = { googlePayConfig },
        ) {
            val request = readyRequest()
            assertThat(tokenizationPublishableKey(request))
                .isEqualTo("${ApiKeyFixtures.FAKE_PUBLISHABLE_KEY}/${ApiKeyFixtures.FAKE_STRIPE_ACCOUNT}")
        }

    private fun runScenario(
        environment: GooglePayEnvironment,
        googlePayConfigProvider: () -> GooglePayConfig,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val requests = Turbine<IsReadyToPayRequest>()
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

        val factory = DefaultGooglePayRepositoryFactory(
            appContext = context,
            logger = Logger.noop(),
            errorReporter = FakeErrorReporter(),
        )
        val repository = factory(
            environment = environment,
            cardFundingFilter = DefaultCardFundingFilter,
            cardBrandFilter = DefaultCardBrandFilter,
            googlePayConfig = googlePayConfigProvider(),
        )

        Scenario(
            repository = repository,
            requests = requests,
        ).block()

        requests.ensureAllEventsConsumed()
        PaymentConfiguration.clearInstance()
        GooglePayRepository.resetFactory()
    }

    private class Scenario(
        private val repository: GooglePayRepository,
        private val requests: ReceiveTurbine<IsReadyToPayRequest>,
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
