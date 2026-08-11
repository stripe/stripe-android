package com.stripe.android.paymentelement

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.utils.GooglePayRepositoryTestRule
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.UsBankAccountFormTestUtils
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class EmbeddedApiConfigurationTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds, // Analytics requests happen async.
    )
    private val analyticEventRule = AnalyticEventRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(analyticEventRule)
            .around(AdvancedFraudSignalsTestRule())
            .around(GooglePayRepositoryTestRule())
            .around(IntentsRule())
    }

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    @Test
    fun testSuccessfulCardPayment_withFormSheetActionConfirm() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession(
            header("Authorization", "Bearer pk_test_123")
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()

        networkRule.enqueueWithPublishableKeyValidation(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
        networkRule.enqueueWithPublishableKeyValidation(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }
        networkRule.enqueueWithPublishableKeyValidation(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
    }

    @Test
    fun testSuccessfulCardPayment_withFormSheetActionContinue() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession(
            header("Authorization", "Bearer pk_test_123")
        ) { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()
        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
        testContext.consumePaymentOptionEvent("card", "4242")

        embeddedContentPage.assertHasSelectedLpm("card")
        embeddedContentPage.clickOnLpm("card")
        formPage.clickPrimaryButton() // Ensures the form has the previous values.
        formPage.waitUntilMissing()

        networkRule.enqueueWithPublishableKeyValidation(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
        networkRule.enqueueWithPublishableKeyValidation(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }
        networkRule.enqueueWithPublishableKeyValidation(
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
    fun testSuccessfulCashAppPayment() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("cashapp")

        networkRule.enqueueWithPublishableKeyValidation(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
        networkRule.enqueueWithPublishableKeyValidation(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }
        networkRule.enqueueWithPublishableKeyValidation(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        testContext.consumePaymentOptionEvent("cashapp", "Cash App Pay")
        testContext.confirm()
        assertThat(testContext.paymentOptionTurbine.awaitItem()).isNull()
    }

    @Test
    fun testSuccessfulUsBankAccountPayment() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success("pi_example_secret_example")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        UsBankAccountFormTestUtils.setupSuccessfulCompletionOfUsBankAccountForm()

        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            allowsDelayedPaymentMethods(true)
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("us_bank_account")

        formPage.waitUntilVisible()
        testRules.compose.onNode(hasText("Full name"))
            .performTextReplacement("Jane Doe")
        testRules.compose.onNode(hasText("Email"))
            .performTextReplacement("janedoe@example.com")

        formPage.clickPrimaryButton(false)

        networkRule.enqueueWithPublishableKeyValidation(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create-us_bank_account.json")
        }
        networkRule.enqueueWithPublishableKeyValidation(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method-us_bank_account.json")
        }
        networkRule.enqueueWithPublishableKeyValidation(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm-us_bank_account.json")
        }

        formPage.clickPrimaryButton()
        //testRules.compose.waitForIdle()
        formPage.waitUntilMissing()
    }

    @Test
    fun testSuccessfulCardPaymentWithLinkSignUp() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.elementsSession { response ->
            response.testBodyFromFile("elements-sessions-requires_payment_method.json")
        }

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()

        networkRule.enqueueWithPublishableKeyValidation(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { response ->
            response.testBodyFromFile("consumer-session-lookup-success.json")
        }

        // Click the "Save my info" checkbox and fill out Link signup fields
        testRules.compose.onNode(hasText("Save my info for faster checkout with Link"))
            .performClick()

        testRules.compose.onNode(hasText("Email"))
            .performTextReplacement("email@email.com")

        testRules.compose.waitUntil(timeoutMillis = 15_000) {
            testRules.compose
                .onAllNodes(hasText("Phone number"))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }

        testRules.compose.onNode(hasText("Phone number"))
            .performTextReplacement("+12113526421")

        networkRule.enqueueWithPublishableKeyValidation(
            method("POST"),
            path("/v1/consumers/accounts/sign_up"),
        ) { response ->
            response.testBodyFromFile("consumer-accounts-signup-success.json")
        }

        networkRule.enqueueWithPublishableKeyValidation(
            method("POST"),
            path("/v1/consumers/payment_details"),
            bodyPart("card[number]", "4242424242424242"),
        ) { response ->
            response.testBodyFromFile("consumer-payment-details-success.json")
        }

        networkRule.enqueueWithPublishableKeyValidation(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.enqueueWithPublishableKeyValidation(
            method("GET"),
            path("/v1/payment_intents/pi_example"),
        ) { response ->
            response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
        }

        networkRule.enqueueWithPublishableKeyValidation(
            method("POST"),
            path("/v1/payment_intents/pi_example/confirm"),
        ) { response ->
            response.testBodyFromFile("payment-intent-confirm.json")
        }

        formPage.clickPrimaryButton()
        formPage.waitUntilMissing()
    }

    private fun NetworkRule.enqueueWithPublishableKeyValidation(
        vararg requestMatcher: RequestMatcher,
        responseFactory: (MockResponse) -> Unit
    ) {
        enqueue(
            header("Authorization", "Bearer pk_test_123"),
            *requestMatcher,
            responseFactory = responseFactory,
        )
    }
}