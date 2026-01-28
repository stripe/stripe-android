package com.stripe.android.paymentsheet

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.IntegrationTypeProvider
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(CardFundingFilteringPrivatePreview::class)
@RunWith(TestParameterInjector::class)
internal class CardNumberControllerNetworkTest {
    // The card-metadata request happens async during card number input. We want to make sure it happens,
    // but it's okay if it takes a bit to happen.
    private val networkRule = NetworkRule(validationTimeout = 5.seconds)

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val composeTestRule = testRules.compose

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    @TestParameter(valuesProvider = IntegrationTypeProvider::class)
    lateinit var integrationType: IntegrationType

    @Test
    fun testNoCardMetadataRequestWhenAllFundingTypesAllowed() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        // Configuration with all funding types allowed (default behavior)
        val configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Card Funding Test")
            .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
            .allowedCardFundingTypes(PaymentSheet.CardFundingType.entries)
            .build()

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = configuration,
            )
        }

        // Fill out card details - NO card-metadata request should be made
        // If a card-metadata request is made, the test will fail because we didn't enqueue it
        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm")
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testCardMetadataRequestMadeWhenFundingTypesRestricted() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        // Configuration with only debit cards allowed - this should trigger card-metadata requests
        val configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Card Funding Test")
            .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
            .allowedCardFundingTypes(listOf(PaymentSheet.CardFundingType.Debit))
            .build()

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = configuration,
            )
        }

        // Enqueue card-metadata response - this request SHOULD be made because funding is restricted
        // If this request is NOT made, the test will fail because we enqueued but didn't consume it
        networkRule.enqueue(
            method("GET"),
            path("edge-internal/card-metadata")
        ) { response ->
            // Return a CREDIT card response - this should trigger the warning
            response.testBodyFromFile("card-metadata-visa-credit.json")
        }

        // Fill out card details with a credit card - card-metadata request SHOULD be made
        page.fillOutCardDetails()

        // Assert that the warning message is shown because credit cards are not allowed
        page.waitForText("Only debit cards are accepted")

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm")
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testNoWarningForAllowedFundingWithNetworkRequest() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        // Configuration with only debit cards allowed
        val configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Card Funding Test")
            .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
            .allowedCardFundingTypes(listOf(PaymentSheet.CardFundingType.Debit))
            .build()

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = configuration,
            )
        }

        // Enqueue card-metadata response for a DEBIT card
        networkRule.enqueue(
            method("GET"),
            path("edge-internal/card-metadata")
        ) { response ->
            response.testBodyFromFile("card-metadata-visa-debit.json")
        }

        // Fill out with a debit card number (4000056655665556 is a common test debit card)
        page.fillOutCardDetailsWithCardNumber("4000056655665556")

        // Assert that NO warning message is shown because debit cards are allowed
        page.assertNoText("Only debit cards are accepted")

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm")
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testNoCardMetadataRequestWhenServerFlagDisabled() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        // Use a response where elements_mobile_card_funding_filtering is false
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile(
                "elements-sessions-requires_payment_method.json",
                replacements = listOf(
                    ResponseReplacement(
                        original = "\"elements_mobile_card_funding_filtering\": true",
                        new = "\"elements_mobile_card_funding_filtering\": false",
                    )
                )
            )
        }

        // Configuration with only debit cards allowed - would normally trigger card-metadata requests,
        // but since the server flag is disabled, no request should be made
        val configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Card Funding Test")
            .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
            .allowedCardFundingTypes(listOf(PaymentSheet.CardFundingType.Debit))
            .build()

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = configuration,
            )
        }

        page.fillOutCardDetails()

        // Assert that NO warning message is shown because the server flag is disabled
        page.assertNoText("Only debit cards are accepted")

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm")
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()
    }
}
