package com.stripe.android.paymentelement

import com.stripe.android.ApiConfiguration
import com.stripe.android.ApiConfigurationPreview
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.TestRules
import org.junit.Rule
import org.junit.Test

@OptIn(ApiConfigurationPreview::class)
internal class EmbeddedPaymentElementApiConfigurationTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)

    @Test
    fun configureUsesApiConfigurationPublishableKeyForElementsSession() =
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, _ ->
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = { },
        ) { testContext ->
            networkRule.elementsSession(
                header("Authorization", "Bearer pk_test_from_api_configuration"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            testContext.configure(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5000,
                        currency = "USD",
                    )
                ),
            ) {
                apiConfiguration(ApiConfiguration("pk_test_from_api_configuration"))
            }

            embeddedContentPage.assertHasSelectedLpm("card")
            testContext.markTestSucceeded()
        }

    @Test
    fun configureUsesApiConfigurationWithStripeAccountForElementsSession() =
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, _ ->
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = { },
        ) { testContext ->
            networkRule.elementsSession(
                header("Authorization", "Bearer pk_test_from_api_configuration"),
                header("Stripe-Account", "acct_connect_123"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            testContext.configure(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5000,
                        currency = "USD",
                    )
                ),
            ) {
                apiConfiguration(
                    ApiConfiguration("pk_test_from_api_configuration")
                        .stripeAccountId("acct_connect_123")
                )
            }

            embeddedContentPage.assertHasSelectedLpm("card")
            testContext.markTestSucceeded()
        }

    @Test
    fun configureWithoutApiConfigurationUsesPaymentConfigurationKey() =
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, _ ->
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = { },
        ) { testContext ->
            // PaymentConfiguration.init is called with "pk_test_123" in the test runner.
            networkRule.elementsSession(
                header("Authorization", "Bearer pk_test_123"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-requires_payment_method.json")
            }

            testContext.configure(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 5000,
                        currency = "USD",
                    )
                ),
            )

            embeddedContentPage.assertHasSelectedLpm("card")
            testContext.markTestSucceeded()
        }
}
