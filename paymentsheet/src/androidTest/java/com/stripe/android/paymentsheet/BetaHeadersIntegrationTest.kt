package com.stripe.android.paymentsheet

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.runEmbeddedPaymentElementTest
import com.stripe.android.paymentsheet.utils.ApiConfigurationTestType
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runFlowControllerTest
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Rule
import org.junit.Test
import com.stripe.android.paymentelement.assertCompleted as assertEmbeddedCompleted

internal class BetaHeadersIntegrationTest {
    private val apiConfigurationTestType = ApiConfigurationTestType.ApiConfigurationWithBetas
    private val integrationType = IntegrationType.Activity
    private val networkRule = NetworkRule(
        expectedStripeVersions = mapOf(ApiRequest.API_HOST to "2020-03-02;integration_beta=v1"),
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(IntentsRule())
    }

    private val page = PaymentSheetPage(testRules.compose)
    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)
    private val defaultConfiguration = PaymentSheet.Configuration.Builder("Example, Inc.")
        .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
        .build()

    @Test
    fun paymentSheetSendsBetas() = runPaymentSheetTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        composeTestRule = testRules.compose,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = apiConfigurationTestType.applyTo(defaultConfiguration),
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
            clientAttributionMetadataParamsInPaymentMethodData(),
            topLevelClientAttributionMetadataParams(),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButtonWithoutWaitingForDismissal()
    }

    @Test
    fun flowControllerSendsBetas() = runFlowControllerTest(
        apiConfigurationTestType = apiConfigurationTestType,
        networkRule = networkRule,
        integrationType = integrationType,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configureFlowController {
            configureWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = apiConfigurationTestType.applyTo(defaultConfiguration),
                callback = { success, error ->
                    assertThat(success).isTrue()
                    assertThat(error).isNull()
                    presentPaymentOptions()
                }
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        page.clickPrimaryButton()

        testContext.consumePaymentOptionEventForFlowController("card", "4242")
        testContext.consumeNullPaymentOptionEventForFlowController()
    }

    @Test
    fun embeddedElementSendsBetas() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        apiConfigurationTestType = apiConfigurationTestType,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertEmbeddedCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()

        enqueueDeferredIntentConfirmationRequests()

        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
    }

    private fun enqueueDeferredIntentConfirmationRequests() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
        networkRule.enqueue(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }
    }
}
