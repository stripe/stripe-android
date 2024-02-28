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
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.composite
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.LinkIntegrationType
import com.stripe.android.paymentsheet.utils.LinkIntegrationTypeProvider
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
    fun testSuccessfulCardPaymentWithLinkSignUp() = activityScenarioRule.runLinkTest(
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.launch()

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()
        page.fillOutLinkPhone()

        Espresso.closeSoftKeyboard()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details"),
            /*
             * Ensures we are passing the full expiration year and not the
             * 2-digit shorthand (should send "2034", not "34")
             */
            bodyPart("card%5Bexp_year%5D", "2034"),
            /*
             * Should use the consumer's publishable key when creating payment details
             */
            header("Authorization", "Bearer pk_545454676767898989"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            linkInformation()
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/log_out"),
        ) { response ->
            response.testBodyFromFile("consumer-session-logout-success.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testSuccessfulCardPaymentWithLinkSignUpAndLinkPassthroughMode() = activityScenarioRule.runLinkTest(
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")

            @Suppress("DEPRECATION")
            assertThat(paymentOption?.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_link)
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_pm_with_link_ps_mode.json")
        }

        testContext.launch()

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()
        page.fillOutLinkPhone()

        Espresso.closeSoftKeyboard()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details"),
            /*
             * Ensures we are passing the full expiration year and not the
             * 2-digit shorthand (should send "2034", not "34")
             */
            bodyPart("card%5Bexp_year%5D", "2034"),
            /*
             * In passthrough mode, this needs to be true
             */
            bodyPart("active", "true"),
            /*
             * In passthrough mode, should use the publishable key from base configuration
             */
            header("Authorization", "Bearer pk_test_123"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details/share"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-share-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            bodyPart("payment_method", "pm_1234"),
            not(linkInformation())
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/log_out"),
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
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.launch()

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()
        page.fillOutLinkPhone()

        Espresso.closeSoftKeyboard()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details"),
        ) { response ->
            response.setResponseCode(500)
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            not(linkInformation())
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testSuccessfulCardPaymentWithLinkSignUpFailureInPassthroughMode() = activityScenarioRule.runLinkTest(
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_pm_with_link_ps_mode.json")
        }

        testContext.launch()

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()
        page.fillOutLinkPhone()

        Espresso.closeSoftKeyboard()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details"),
        ) { response ->
            response.setResponseCode(500)
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            not(bodyPart("payment_method", "pm_1234")),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testSuccessfulCardPaymentWithLinkSignUpShareFailureInPassthroughMode() = activityScenarioRule.runLinkTest(
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("4242")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_pm_with_link_ps_mode.json")
        }

        testContext.launch()

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()
        page.fillOutLinkPhone()

        Espresso.closeSoftKeyboard()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details/share"),
        ) { response ->
            response.setResponseCode(500)
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            not(bodyPart("payment_method", "pm_1234")),
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
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.launch()

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-exists-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()

        Espresso.closeSoftKeyboard()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-exists-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            not(linkInformation())
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
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.scenario.onActivity {
            LinkStore(it).markLinkAsUsed()
        }

        testContext.launch()

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            not(linkInformation())
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
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        repeat(3) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
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

        testContext.launch(configuration)

        page.fillOutCardDetails()
        page.fillOutLink()

        Espresso.closeSoftKeyboard()

        repeat(2) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/payment_details"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            linkInformation()
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/log_out"),
        ) { response ->
            response.testBodyFromFile("consumer-session-logout-success.json")
        }

        page.clickPrimaryButton()
    }

    private fun linkInformation(): RequestMatcher {
        return composite(
            bodyPart(
                name = "payment_method_data%5Blink%5D%5Bcard%5D%5Bcvc%5D",
                value = "123"
            ),
            bodyPart(
                name = "payment_method_data%5Blink%5D%5Bpayment_details_id%5D",
                value = "QAAAKJ6"
            ),
            bodyPart(
                name = "payment_method_data%5Blink%5D%5Bcredentials%5D%5Bconsumer_session_client_secret%5D",
                value = "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJDUzNTFkNjNhLTZkNGMtND"
            ),
        )
    }
}
