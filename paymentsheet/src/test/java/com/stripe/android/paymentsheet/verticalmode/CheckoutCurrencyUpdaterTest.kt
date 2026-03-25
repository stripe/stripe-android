package com.stripe.android.paymentsheet.verticalmode

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkout.CheckoutInstancesTestRule
import com.stripe.android.checkout.CheckoutStateFactory
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.utils.FakePaymentElementLoader
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class CheckoutCurrencyUpdaterTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(CheckoutInstancesTestRule())

    private val checkoutSessionRepository = CheckoutSessionRepository(
        stripeNetworkClient = DefaultStripeNetworkClient(),
        publishableKeyProvider = { "pk_test_123" },
        stripeAccountIdProvider = { null },
    )

    @Test
    fun `updateCurrency success - calls repo, updates checkout instances, calls loader`() = runTest {
        val instancesKey = CheckoutStateFactory.DEFAULT_KEY
        val initialResponse = CheckoutSessionResponseFactory.create(id = DEFAULT_CHECKOUT_SESSION_ID)
        val checkout = Checkout.createWithState(
            applicationContext,
            CheckoutStateFactory.create(key = instancesKey, checkoutSessionResponse = initialResponse),
        )

        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        var capturedInitMode: PaymentElementLoader.InitializationMode? = null
        val fakeLoader = FakePaymentElementLoader()
        val trackingLoader = object : PaymentElementLoader {
            override suspend fun load(
                initializationMode: PaymentElementLoader.InitializationMode,
                integrationConfiguration: PaymentElementLoader.Configuration,
                metadata: PaymentElementLoader.Metadata,
            ): Result<PaymentElementLoader.State> {
                capturedInitMode = initializationMode
                return fakeLoader.load(initializationMode, integrationConfiguration, metadata)
            }
        }

        val config = PaymentSheet.Configuration("Test Merchant")
        val updater = DefaultCheckoutCurrencyUpdater(checkoutSessionRepository, trackingLoader)

        val result = updater.updateCurrency(
            instancesKey = instancesKey,
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            currencyCode = "eur",
            config = config,
            initializedViaCompose = false,
        )

        assertThat(result.isSuccess).isTrue()
        val updateResult = result.getOrThrow()

        // Verify CheckoutInstances was updated with the response from the network
        assertThat(checkout.internalState.checkoutSessionResponse).isEqualTo(updateResult.checkoutSessionResponse)

        // Verify loader was called with the updated init mode
        val capturedMode = capturedInitMode as? PaymentElementLoader.InitializationMode.CheckoutSession
        assertThat(capturedMode?.instancesKey).isEqualTo(instancesKey)
        assertThat(capturedMode?.checkoutSessionResponse).isEqualTo(updateResult.checkoutSessionResponse)
    }

    @Test
    fun `updateCurrency failure on API call - returns failure and loader not called`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error": {"message": "Invalid currency"}}""")
        }

        val loaderNotExpectedToBeCalledLoader = object : PaymentElementLoader {
            override suspend fun load(
                initializationMode: PaymentElementLoader.InitializationMode,
                integrationConfiguration: PaymentElementLoader.Configuration,
                metadata: PaymentElementLoader.Metadata,
            ): Result<PaymentElementLoader.State> {
                throw AssertionError("Loader should not be called when API fails")
            }
        }

        val updater = DefaultCheckoutCurrencyUpdater(checkoutSessionRepository, loaderNotExpectedToBeCalledLoader)

        val result = updater.updateCurrency(
            instancesKey = CheckoutStateFactory.DEFAULT_KEY,
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            currencyCode = "invalid",
            config = PaymentSheet.Configuration("Test Merchant"),
            initializedViaCompose = false,
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `updateCurrency failure on loader - returns failure`() = runTest {
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val failingLoader = FakePaymentElementLoader(shouldFail = true)
        val updater = DefaultCheckoutCurrencyUpdater(checkoutSessionRepository, failingLoader)

        val result = updater.updateCurrency(
            instancesKey = CheckoutStateFactory.DEFAULT_KEY,
            sessionId = DEFAULT_CHECKOUT_SESSION_ID,
            currencyCode = "eur",
            config = PaymentSheet.Configuration("Test Merchant"),
            initializedViaCompose = false,
        )

        assertThat(result.isFailure).isTrue()
    }
}
