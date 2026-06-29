package com.stripe.android.paymentelement

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeClient
import com.stripe.android.checkout.CheckoutInstancesTestRule
import com.stripe.android.checkout.CheckoutStateFactory
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfigurationCoordinator
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationHelper
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedStateHelper
import com.stripe.android.paymentelement.embedded.content.PaymentOptionDisplayDataHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class EmbeddedPaymentElementTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))
        .around(CheckoutInstancesTestRule())

    @Test
    fun `configure with checkout throws when checkout mutation is in flight`() = runTest {
        val checkout = CheckoutStateFactory.createCheckout(applicationContext)
        networkRule.checkoutUpdate { response ->
            response.setBodyDelay(5, TimeUnit.SECONDS)
            response.testBodyFromFile("checkout-session-apply-discount.json")
        }
        val deferred = async { checkout.applyPromotionCode("10OFF") }
        testScheduler.advanceUntilIdle()

        val embeddedPaymentElement = createEmbeddedPaymentElement()
        val error = runCatching {
            embeddedPaymentElement.configure(
                checkout,
                EmbeddedPaymentElement.Configuration.Builder("Test").build(),
            )
        }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error).hasMessageThat()
            .isEqualTo("Cannot launch while a checkout session mutation is in flight.")

        deferred.cancel()
    }

    @Test
    fun `configure uses stripeClient publishable key when set`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val customKey = "pk_test_custom_123"

            networkRule.elementsSession(
                RequestMatchers.header("Authorization", "Bearer $customKey"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            lateinit var embeddedPaymentElement: EmbeddedPaymentElement
            composeTestRule.setContent {
                embeddedPaymentElement = rememberEmbeddedPaymentElement(
                    EmbeddedPaymentElement.Builder(
                        createIntentCallback = { _, _ -> throw NotImplementedError() },
                        resultCallback = { _ -> },
                    ).stripeClient(StripeClient(customKey))
                )
            }
            composeTestRule.waitForIdle()

            runBlocking {
                embeddedPaymentElement.configure(
                    intentConfiguration = PaymentSheet.IntentConfiguration(
                        mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                            amount = 5000,
                            currency = "usd",
                        ),
                    ),
                    configuration = EmbeddedPaymentElement.Configuration.Builder("Test").build(),
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createEmbeddedPaymentElement(): EmbeddedPaymentElement {
        return EmbeddedPaymentElement(
            confirmationHelper = object : EmbeddedConfirmationHelper {
                override fun confirm() = error("Not expected")
            },
            contentHelper = FakeEmbeddedContentHelper(),
            selectionHolder = EmbeddedSelectionHolder(SavedStateHandle()),
            paymentOptionDisplayDataHolder = object : PaymentOptionDisplayDataHolder {
                override val paymentOption = MutableStateFlow<EmbeddedPaymentElement.PaymentOptionDisplayData?>(null)
            },
            configurationCoordinator = object : EmbeddedConfigurationCoordinator {
                override suspend fun configure(
                    configuration: EmbeddedPaymentElement.Configuration,
                    initializationMode: PaymentElementLoader.InitializationMode,
                ): EmbeddedPaymentElement.ConfigureResult = error("Not expected")
            },
            stateHelper = FakeEmbeddedStateHelper(),
        )
    }
}
