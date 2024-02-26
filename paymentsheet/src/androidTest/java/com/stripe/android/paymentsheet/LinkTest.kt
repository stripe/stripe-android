package com.stripe.android.paymentsheet

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.link.account.LinkStore
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.LinkIntegrationType
import com.stripe.android.paymentsheet.utils.LinkIntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.LinkTestRunnerContext
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runLinkTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(TestParameterInjector::class)
internal class LinkTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val networkRule = NetworkRule()

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @TestParameter(valuesProvider = LinkIntegrationTypeProvider::class)
    lateinit var integrationType: LinkIntegrationType

    @Test
    fun testSuccessfulCardPaymentWithLinkSignUpForPaymentSheet() = activityScenarioRule.runLinkTest(
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            RequestMatchers.method("GET"),
            RequestMatchers.path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        when (testContext) {
            is LinkTestRunnerContext.PaymentSheet -> {
                testContext.context.presentPaymentSheet {
                    presentWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = emptyConfiguration(),
                    )
                }
            }
            is LinkTestRunnerContext.FlowController -> {
                testContext.context.configureFlowController {
                    configureWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = emptyConfiguration(),
                        callback = { success, error ->
                            assertThat(success).isTrue()
                            assertThat(error).isNull()
                            presentPaymentOptions()
                        },
                    )
                }
            }
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()
        page.fillOutLinkPhone()

        Espresso.closeSoftKeyboard()

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/payment_details"),
            /*
             * Ensures we are passing the full expiration year and not the
             * 2-digit shorthand (should send "2034", not "34")
             */
            RequestMatchers.bodyPart("card%5Bexp_year%5D", "2034"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/payment_intents/pi_example/confirm"),
            linkInformation()
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/sessions/log_out"),
        ) { response ->
            response.testBodyFromFile("consumer-session-logout-success.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testSuccessfulCardPaymentWithLinkSignUpFailure() = activityScenarioRule.runLinkTest(
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            RequestMatchers.method("GET"),
            RequestMatchers.path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        when (testContext) {
            is LinkTestRunnerContext.PaymentSheet -> {
                testContext.context.presentPaymentSheet {
                    presentWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = emptyConfiguration(),
                    )
                }
            }
            is LinkTestRunnerContext.FlowController -> {
                testContext.context.configureFlowController {
                    configureWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = emptyConfiguration(),
                        callback = { success, error ->
                            assertThat(success).isTrue()
                            assertThat(error).isNull()
                            presentPaymentOptions()
                        },
                    )
                }
            }
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()
        page.fillOutLinkPhone()

        Espresso.closeSoftKeyboard()

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/payment_details"),
        ) { response ->
            response.setResponseCode(500)
        }

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/payment_intents/pi_example/confirm"),
            RequestMatchers.not(linkInformation())
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testSuccessfulCardPaymentWithExistingLinkEmailUsed() = activityScenarioRule.runLinkTest(
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            RequestMatchers.method("GET"),
            RequestMatchers.path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        when (testContext) {
            is LinkTestRunnerContext.PaymentSheet -> {
                testContext.context.presentPaymentSheet {
                    presentWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = emptyConfiguration(),
                    )
                }
            }
            is LinkTestRunnerContext.FlowController -> {
                testContext.context.configureFlowController {
                    configureWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = emptyConfiguration(),
                        callback = { success, error ->
                            assertThat(success).isTrue()
                            assertThat(error).isNull()
                            presentPaymentOptions()
                        },
                    )
                }
            }
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-exists-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()

        Espresso.closeSoftKeyboard()

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-exists-success.json")
        }

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/payment_intents/pi_example/confirm"),
            RequestMatchers.not(linkInformation())
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testSuccessfulCardPaymentWithLinkPreviouslyUsed() = activityScenarioRule.runLinkTest(
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            RequestMatchers.method("GET"),
            RequestMatchers.path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.scenario.onActivity {
            LinkStore(it).markLinkAsUsed()
        }

        when (testContext) {
            is LinkTestRunnerContext.PaymentSheet -> {
                testContext.context.presentPaymentSheet {
                    presentWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = emptyConfiguration(),
                    )
                }
            }
            is LinkTestRunnerContext.FlowController -> {
                testContext.context.configureFlowController {
                    configureWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = emptyConfiguration(),
                        callback = { success, error ->
                            assertThat(success).isTrue()
                            assertThat(error).isNull()
                            presentPaymentOptions()
                        },
                    )
                }
            }
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/payment_intents/pi_example/confirm"),
            RequestMatchers.not(linkInformation())
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testLogoutAfterLinkTransaction() = activityScenarioRule.runLinkTest(
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            RequestMatchers.method("GET"),
            RequestMatchers.path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        repeat(3) {
            networkRule.enqueue(
                RequestMatchers.method("POST"),
                RequestMatchers.path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }
        }

        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Merchant, Inc.",
            defaultBillingDetails = PaymentSheet.BillingDetails(
                email = "test-${UUID.randomUUID()}@email.com",
                phone = "+15555555555",
            )
        )

        when (testContext) {
            is LinkTestRunnerContext.PaymentSheet -> {
                testContext.context.presentPaymentSheet {
                    presentWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = configuration,
                    )
                }
            }
            is LinkTestRunnerContext.FlowController -> {
                testContext.context.configureFlowController {
                    configureWithPaymentIntent(
                        paymentIntentClientSecret = "pi_example_secret_example",
                        configuration = configuration,
                        callback = { success, error ->
                            assertThat(success).isTrue()
                            assertThat(error).isNull()
                            presentPaymentOptions()
                        },
                    )
                }
            }
        }

        page.fillOutCardDetails()
        page.fillOutLink()

        Espresso.closeSoftKeyboard()

        repeat(2) {
            networkRule.enqueue(
                RequestMatchers.method("POST"),
                RequestMatchers.path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }
        }

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/payment_details"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/payment_intents/pi_example/confirm"),
            linkInformation()
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        networkRule.enqueue(
            RequestMatchers.method("POST"),
            RequestMatchers.path("/v1/consumers/sessions/log_out"),
        ) { response ->
            response.testBodyFromFile("consumer-session-logout-success.json")
        }

        page.clickPrimaryButton()
    }

    private fun linkInformation(): RequestMatcher {
        return RequestMatchers.composite(
            RequestMatchers.bodyPart(
                name = "payment_method_data%5Blink%5D%5Bcard%5D%5Bcvc%5D",
                value = "123"
            ),
            RequestMatchers.bodyPart(
                name = "payment_method_data%5Blink%5D%5Bpayment_details_id%5D",
                value = "QAAAKJ6"
            ),
            RequestMatchers.bodyPart(
                name = "payment_method_data%5Blink%5D%5Bcredentials%5D%5Bconsumer_session_client_secret%5D",
                value = "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJDUzNTFkNjNhLTZkNGMtND"
            ),
        )
    }

    private fun emptyConfiguration(): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = "Merchant, Inc.",
        )
    }
}
