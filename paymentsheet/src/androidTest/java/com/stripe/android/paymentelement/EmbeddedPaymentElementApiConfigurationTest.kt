package com.stripe.android.paymentelement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiConfiguration
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.utils.TestRules
import org.junit.Rule
import org.junit.Test

internal class EmbeddedPaymentElementApiConfigurationTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    @Test
    fun testPerInstanceApiConfigurationUsedForLoadingRequests() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession(
            header("Authorization", "Bearer pk_test_per_instance_key"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            apiConfiguration(
                ApiConfiguration("pk_test_per_instance_key")
            )
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()
        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
        testContext.consumePaymentOptionEvent("card", "4242")

        networkRule.enqueue(
            header("Authorization", "Bearer pk_test_per_instance_key"),
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
        networkRule.enqueue(
            header("Authorization", "Bearer pk_test_per_instance_key"),
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }
        networkRule.enqueue(
            header("Authorization", "Bearer pk_test_per_instance_key"),
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        testContext.confirm()
        assertThat(testContext.paymentOptionTurbine.awaitItem()).isNull()
    }

    @Test
    fun testPerInstanceApiConfigurationUsedForFormSheetConfirmation() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession(
            header("Authorization", "Bearer pk_test_per_instance_key"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
            apiConfiguration(
                ApiConfiguration("pk_test_per_instance_key")
            )
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()

        networkRule.enqueue(
            header("Authorization", "Bearer pk_test_per_instance_key"),
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
        networkRule.enqueue(
            header("Authorization", "Bearer pk_test_per_instance_key"),
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }
        networkRule.enqueue(
            header("Authorization", "Bearer pk_test_per_instance_key"),
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
    }
}
