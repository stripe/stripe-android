@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.rule.IntentsRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class PaymentMethodMessagingContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<LearnMoreActivity>()

    @Test
    fun `onClick launches activity with correct args`() = runScenario {
        coordinator.messagingContent.test {
            assertThat(awaitItem()).isNull()
            coordinator.configure(
                PaymentMethodMessagingElement.Configuration()
                    .paymentMethodTypes(listOf(PaymentMethod.Type.Klarna, PaymentMethod.Type.Affirm))
                    .currency("usd")
                    .countryCode("US")
                    .amount(1000L)
                    .build()
            )
            val content = awaitItem()
            assertThat(content).isNotNull()

            composeRule.setContent {
                content?.Content(PaymentMethodMessagingElement.Appearance().build())
            }

            composeRule.waitForIdle()
            composeRule.onNodeWithText(text = "buy stuff", substring = true).performClick()

            intended(
                allOf(
                    hasExtra(
                        "learn_more_args",
                        LearnMoreActivityArgs(
                            learnMoreUrl = "www.test.com&theme=stripe"
                        )
                    ),
                    hasComponent(LearnMoreActivity::class.java.name),
                )
            )
        }
    }

    private class Scenario(
        val coordinator: DefaultPaymentMethodMessagingCoordinator,
    )

    private fun runScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val coordinator = DefaultPaymentMethodMessagingCoordinator(
            stripeRepository = FakeStripeRepository(),
            paymentConfiguration = PaymentConfiguration("key"),
            learnMoreActivityLauncher = DefaultLearnMoreActivityLauncher()
        )

        Scenario(
            coordinator = coordinator,
        ).block()
    }
}
