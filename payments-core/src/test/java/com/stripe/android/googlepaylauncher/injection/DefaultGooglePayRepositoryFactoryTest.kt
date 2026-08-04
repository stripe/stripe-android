package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
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
    private var capturedRequest: IsReadyToPayRequest? = null

    @Before
    fun setup() {
        PaymentConfiguration.init(context, "pk_123")
        GooglePayRepository.googlePayAvailabilityClientFactory =
            object : GooglePayAvailabilityClient.Factory {
                override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
                    return object : GooglePayAvailabilityClient {
                        override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
                            capturedRequest = request
                            return true
                        }
                    }
                }
            }
    }

    @After
    fun tearDown() {
        GooglePayRepository.resetFactory()
        capturedRequest = null
    }

    @Test
    fun `when allowNoExistingPaymentMethodForGooglePay is disabled, existingPaymentMethodRequired should be true`() =
        runScenario(allowNoExistingPaymentMethodForGooglePay = false) {
            assertThat(existingPaymentMethodRequired()).isTrue()
        }

    @Test
    fun `when allowNoExistingPaymentMethodForGooglePay is enabled, existingPaymentMethodRequired should be false`() =
        runScenario(allowNoExistingPaymentMethodForGooglePay = true) {
            assertThat(existingPaymentMethodRequired()).isFalse()
        }

    private fun runScenario(
        allowNoExistingPaymentMethodForGooglePay: Boolean,
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
        )

        Scenario(
            repository = repository,
        ).block()
    }

    private inner class Scenario(
        private val repository: GooglePayRepository,
    ) {
        suspend fun existingPaymentMethodRequired(): Boolean {
            repository.isReady().test {
                assertThat(awaitItem()).isTrue()
                awaitComplete()
            }

            assertThat(capturedRequest).isNotNull()
            return JSONObject(capturedRequest!!.toJson())
                .getBoolean("existingPaymentMethodRequired")
        }
    }
}
