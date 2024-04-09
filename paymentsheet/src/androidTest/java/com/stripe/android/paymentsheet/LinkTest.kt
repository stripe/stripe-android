package com.stripe.android.paymentsheet

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.utils.urlEncode
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
import kotlin.time.Duration.Companion.seconds

@RunWith(TestParameterInjector::class)
internal class LinkTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val activityScenarioRule = composeTestRule.activityRule

    // The /v1/consumers/sessions/log_out request is launched async from a GlobalScope. We want to make sure it happens,
    // but it's okay if it takes a bit to happen.
    @get:Rule
    val networkRule = NetworkRule(validationTimeout = 5.seconds)

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

        closeSoftKeyboard()

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
             * Make sure card number is included
             */
            bodyPart(urlEncode("card[number]"), "4242424242424242"),
            /*
             * Make sure card expiration month is included
             */
            bodyPart(urlEncode("card[exp_month]"), "12"),
            /*
             * Ensures we are passing the full expiration year and not the
             * 2-digit shorthand (should send "2034", not "34")
             */
            bodyPart(urlEncode("card[exp_year]"), "2034"),
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
    fun testSuccessfulCardPaymentWithLinkSignUpAndSaveForFutureUsage() =
        activityScenarioRule.runLinkTest(
            integrationType = integrationType,
            paymentOptionCallback = { paymentOption ->
                assertThat(paymentOption?.label).endsWith("4242")

                @Suppress("DEPRECATION")
                assertThat(paymentOption?.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_link_2024)
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

            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/customers/cus_1"),
            ) { response ->
                response.testBodyFromFile("customer-get-success.json")
            }

            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/payment_methods"),
            ) { response ->
                response.testBodyFromFile("payment-methods-get-success-empty.json")
            }

            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }

            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }

            testContext.launch(
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "123"
                    )
                )
            )

            page.fillOutCardDetails()
            page.clickOnSaveForFutureUsage("Merchant, Inc.")

            closeSoftKeyboard()

            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }

            page.fillOutLinkPhone()

            closeSoftKeyboard()

            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
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
                bodyPart(urlEncode("payment_method_options[card][setup_future_usage]"), "off_session"),
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
    fun testSuccessfulCardPaymentWithLinkSignUpAndCardBrandChoice() = activityScenarioRule.runLinkTest(
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
            response.testBodyFromFile("elements-sessions-requires_payment_method_with_cbc.json")
        }

        testContext.launch()

        page.fillOutCardDetailsWithCardBrandChoice()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()
        page.fillOutLinkPhone()
        page.fillOutLinkName()

        closeSoftKeyboard()

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
             * Ensures card number is included
             */
            bodyPart(urlEncode("card[number]"), "4000002500001001"),
            /*
             * Ensures card expiration month is included
             */
            bodyPart(urlEncode("card[exp_month]"), "12"),
            /*
             * Ensures we are passing the full expiration year and not the
             * 2-digit shorthand (should send "2034", not "34")
             */
            bodyPart(urlEncode("card[exp_year]"), "2034"),
            /*
             * Ensures card brand choice is passed properly.
             */
            bodyPart(urlEncode("card[preferred_network]"), "cartes_bancaires"),
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
            assertThat(paymentOption?.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_link_2024)
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

        closeSoftKeyboard()

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
             * Make sure card number is included
             */
            bodyPart(urlEncode("card[number]"), "4242424242424242"),
            /*
             * Make sure card expiration month is included
             */
            bodyPart(urlEncode("card[exp_month]"), "12"),
            /*
             * Ensures we are passing the full expiration year and not the
             * 2-digit shorthand (should send "2034", not "34")
             */
            bodyPart(urlEncode("card[exp_year]"), "2034"),
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
    fun testSuccessfulCardPaymentWithLinkSignUpAndLinkPassthroughModeAndSaveForFutureUsage() =
        activityScenarioRule.runLinkTest(
            integrationType = integrationType,
            paymentOptionCallback = { paymentOption ->
                assertThat(paymentOption?.label).endsWith("4242")

                @Suppress("DEPRECATION")
                assertThat(paymentOption?.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_link_2024)
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

            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/customers/cus_1"),
            ) { response ->
                response.testBodyFromFile("customer-get-success.json")
            }

            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/payment_methods"),
            ) { response ->
                response.testBodyFromFile("payment-methods-get-success-empty.json")
            }

            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }

            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }

            testContext.launch(
                configuration = PaymentSheet.Configuration(
                    merchantDisplayName = "Merchant, Inc.",
                    customer = PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "123"
                    )
                )
            )

            page.fillOutCardDetails()
            page.clickOnSaveForFutureUsage("Merchant, Inc.")

            closeSoftKeyboard()

            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
            }

            page.fillOutLinkPhone()

            closeSoftKeyboard()

            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { response ->
                response.testBodyFromFile("consumer-session-lookup-success.json")
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
                path("/v1/consumers/payment_details/share"),
            ) { response ->
                response.testBodyFromFile("consumer-payment-details-share-success.json")
            }

            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_intents/pi_example/confirm"),
                bodyPart(urlEncode("payment_method_options[card][setup_future_usage]"), "off_session"),
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
    fun testSuccessfulCardPaymentWithLinkSignUpPassthroughModeAndCardBrandChoice() = activityScenarioRule.runLinkTest(
        integrationType = integrationType,
        paymentOptionCallback = { paymentOption ->
            assertThat(paymentOption?.label).endsWith("1001")

            @Suppress("DEPRECATION")
            assertThat(paymentOption?.drawableResourceId).isEqualTo(R.drawable.stripe_ic_paymentsheet_link_2024)
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_pm_with_link_ps_mode_and_cbc.json")
        }

        testContext.launch()

        page.fillOutCardDetailsWithCardBrandChoice()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        page.clickOnLinkCheckbox()
        page.fillOutLinkEmail()
        page.fillOutLinkPhone()
        page.fillOutLinkName()

        closeSoftKeyboard()

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
             * Make sure card number is included
             */
            bodyPart(urlEncode("card[number]"), "4000002500001001"),
            /*
             * Make sure card expiration month is included
             */
            bodyPart(urlEncode("card[exp_month]"), "12"),
            /*
             * Ensures we are passing the full expiration year and not the
             * 2-digit shorthand (should send "2034", not "34")
             */
            bodyPart(urlEncode("card[exp_year]"), "2034"),
            /*
             * Ensures card brand choice is passed properly.
             */
            bodyPart(urlEncode("card[preferred_network]"), "cartes_bancaires"),
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

        closeSoftKeyboard()

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

        closeSoftKeyboard()

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

        closeSoftKeyboard()

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

        closeSoftKeyboard()

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

        closeSoftKeyboard()

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

    @Test
    fun testSuccessfulCardPaymentWithLinkSignUpWithAlbaniaPhoneNumber() = activityScenarioRule.runLinkTest(
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
        page.selectPhoneNumberCountry("Albania")
        page.fillOutLinkPhone("888888888")

        closeSoftKeyboard()

        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
            bodyPart(
                name = "email_address",
                value = "email%40email.com"
            ),
            bodyPart(
                name = "phone_number",
                value = "%2B355888888888"
            ),
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
            linkInformation(),
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
                name = urlEncode("payment_method_data[link][card][cvc]"),
                value = "123"
            ),
            bodyPart(
                name = urlEncode("payment_method_data[link][payment_details_id]"),
                value = "QAAAKJ6"
            ),
            bodyPart(
                name = urlEncode("payment_method_data[link][credentials][consumer_session_client_secret]"),
                value = "12oBEhVjc21yKkFYNnhMVTlXbXdBQUFJRmEaJDUzNTFkNjNhLTZkNGMtND"
            ),
        )
    }
}
