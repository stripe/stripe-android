package com.stripe.android.paymentmethodmessaging.element.analytics

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentmethodmessaging.element.DefaultPaymentMethodMessagingCoordinator
import com.stripe.android.paymentmethodmessaging.element.FakeStripeRepository
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingContent
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElement
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElementPreview
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, PaymentMethodMessagingElementPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class PaymentMethodMessageAnalyticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun `onLoadStarted called with correct params when element is configured successfully`() = runScenario {
        element.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .currency("usd")
                .countryCode("US")
                .amount(1000L)
                .locale("en")
        )
        val loadStartedCall = eventReporter.loadStartedTurbine.awaitItem()
        assertThat(loadStartedCall.amount).isEqualTo(1000L)
        assertThat(loadStartedCall.currency).isEqualTo("usd")
        assertThat(loadStartedCall.countryCode).isEqualTo("US")
        assertThat(loadStartedCall.locale).isEqualTo("en")

        eventReporter.loadSucceededTurbine.awaitItem()
    }

    @Test
    fun `onLoadSucceeded called when element is configured successfully MultiPartner`() = runScenario {
        element.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .currency("usd")
                .countryCode("US")
                .amount(1000L)
                .locale("en")
        )
        eventReporter.loadStartedTurbine.awaitItem()

        val loadSucceededCall = eventReporter.loadSucceededTurbine.awaitItem()
        assertThat(loadSucceededCall.content).isInstanceOf(PaymentMethodMessagingContent.MultiPartner::class.java)
        assertThat(loadSucceededCall.paymentMethods).isNotNull()
    }

    @Test
    fun `onLoadSucceeded called when element is configured successfully SinglePartner`() = runScenario {
        element.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .currency("usd")
                .countryCode("US")
                .amount(1000L)
                .locale("en")
                .paymentMethodTypes(listOf(PaymentMethod.Type.Klarna))
        )
        eventReporter.loadStartedTurbine.awaitItem()

        val loadSucceededCall = eventReporter.loadSucceededTurbine.awaitItem()
        assertThat(loadSucceededCall.content).isInstanceOf(PaymentMethodMessagingContent.SinglePartner::class.java)
        assertThat(loadSucceededCall.paymentMethods).isNotNull()
    }

    @Test
    fun `onLoadSucceeded called when element is configured successfully NoContent`() = runScenario {
        element.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .currency("usd")
                .countryCode("US")
                .amount(0L)
                .locale("en")
        )
        eventReporter.loadStartedTurbine.awaitItem()

        val loadSucceededCall = eventReporter.loadSucceededTurbine.awaitItem()
        assertThat(loadSucceededCall.content).isInstanceOf(PaymentMethodMessagingContent.NoContent::class.java)
        assertThat(loadSucceededCall.paymentMethods).isNotNull()
    }

    @Test
    fun `onLoadFailed called when configuration fails`() = runScenario {
        element.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .currency("usd")
                .countryCode("US")
                .amount(-1L)
                .locale("en")
        )
        eventReporter.loadStartedTurbine.awaitItem()

        val loadSucceededCall = eventReporter.loadFailedTurbine.awaitItem()
        assertThat(loadSucceededCall.message).isEqualTo("Price must be non negative")
    }

    @Test
    fun `onElementDisplayed sent when content called`() = runScenario {
        element.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .currency("usd")
                .countryCode("US")
                .amount(1000L)
        )
        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        composeRule.setContent {
            element.Content()
        }
        assertThat(eventReporter.elementDisplayedTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `onElementTapped sent when content tapped`() = runScenario {
        element.configure(
            configuration = PaymentMethodMessagingElement.Configuration()
                .currency("usd")
                .countryCode("US")
                .amount(1000L)
        )
        assertThat(eventReporter.loadStartedTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.loadSucceededTurbine.awaitItem()).isNotNull()
        composeRule.setContent {
            element.Content()
        }

        eventReporter.elementDisplayedTurbine.awaitItem()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text = "buy stuff", substring = true).performClick()
        assertThat(eventReporter.elementTappedTurbine.awaitItem()).isNotNull()
    }

    private class Scenario(
        val element: PaymentMethodMessagingElement,
        val eventReporter: FakeEventReporter
    )

    private fun runScenario(
        testBlock: suspend Scenario.() -> Unit
    ) = runTest {
        val eventReporter = FakeEventReporter()
        val coordinator = DefaultPaymentMethodMessagingCoordinator(
            stripeRepository = FakeStripeRepository(),
            paymentConfiguration = { PaymentConfiguration(publishableKey = "pk_123_test") },
            eventReporter = eventReporter,
            viewModelScope = CoroutineScope(UnconfinedTestDispatcher())
        )

        val element = PaymentMethodMessagingElement(
            messagingCoordinator = coordinator,
            eventReporter = eventReporter
        )

        assertThat(eventReporter.initTurbine.awaitItem()).isNotNull()

        Scenario(
            element = element,
            eventReporter = eventReporter
        ).testBlock()
        eventReporter.validate()
    }
}
