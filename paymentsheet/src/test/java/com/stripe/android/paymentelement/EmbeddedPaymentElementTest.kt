package com.stripe.android.paymentelement

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutInstancesTestRule
import com.stripe.android.checkout.CheckoutStateFactory
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfigurationCoordinator
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationHelper
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.FakeEmbeddedStateHelper
import com.stripe.android.paymentelement.embedded.content.PaymentOptionDisplayDataHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentConfigurationTestRule
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
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
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
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
    fun `configure with checkout returns Failed when BDCC address is Never`() = runTest {
        val checkout = CheckoutStateFactory.createCheckout(applicationContext)
        val embeddedPaymentElement = createEmbeddedPaymentElement()

        val result = embeddedPaymentElement.configure(
            checkout,
            EmbeddedPaymentElement.Configuration.Builder("Test")
                .billingDetailsCollectionConfiguration(
                    PaymentSheet.BillingDetailsCollectionConfiguration(
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
                    ),
                )
                .build(),
        )

        assertThat(result).isInstanceOf(EmbeddedPaymentElement.ConfigureResult.Failed::class.java)
        val error = (result as EmbeddedPaymentElement.ConfigureResult.Failed).error
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(error).hasMessageThat()
            .contains("BillingDetailsCollectionConfiguration.address must not be CollectionMode.Never")
    }

    private fun createEmbeddedPaymentElement(): EmbeddedPaymentElement {
        return EmbeddedPaymentElement(
            confirmationHelper = object : EmbeddedConfirmationHelper {
                override fun confirm() = error("Not expected")
            },
            contentHelper = FakeEmbeddedContentHelper(),
            selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()),
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
