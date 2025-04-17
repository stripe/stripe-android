@file:OptIn(ExperimentalAnalyticEventCallbackApi::class, ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import com.stripe.paymentelementnetwork.setupPaymentMethodDetachResponse
import com.stripe.paymentelementnetwork.setupV1PaymentMethodsResponse
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.ManagePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test

internal class EmbeddedPaymentElementTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val managePage = ManagePage(testRules.compose)
    private val editPage = EditPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    private val card1 = CardPaymentMethodDetails("pm_12345", "4242")
    private val card2 = CardPaymentMethodDetails("pm_67890", "5544")

    @Test
    fun testSuccessfulCardPayment_withFormSheetActionConfirm() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
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

        testContext.configure {
            formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()

        enqueueDeferredIntentConfirmationRequests()

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
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
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

        embeddedContentPage.assertHasSelectedLpm("card")
        embeddedContentPage.clickOnLpm("card")
        formPage.clickPrimaryButton() // Ensures the form has the previous values.
        formPage.waitUntilMissing()

        enqueueDeferredIntentConfirmationRequests()

        testContext.confirm()
    }

    @Test
    fun testRemoveCard() = runEmbeddedPaymentElementTest(
        networkRule = networkRule,
        createIntentCallback = { _, shouldSavePaymentMethod ->
            assertThat(shouldSavePaymentMethod).isFalse()
            CreateIntentResult.Success("pi_example_secret_12345")
        },
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
        }
        networkRule.setupV1PaymentMethodsResponse(card1, card2)

        testContext.configure {
            customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
        }

        embeddedContentPage.clickViewMore()

        managePage.waitUntilVisible()
        managePage.clickEdit()
        managePage.clickEdit(card1.id)
        editPage.waitUntilVisible()

        networkRule.setupPaymentMethodDetachResponse(card1.id)

        editPage.clickRemove()
        managePage.waitUntilVisible()
        managePage.waitUntilGone(card1.id)
        managePage.clickDone()

        testContext.markTestSucceeded()
    }

    @Test
    fun testStateCanBeTakenFromOneInstanceToAnother() {
        var state: EmbeddedPaymentElement.State? = null

        // Instance 1
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
            networkRule.setupV1PaymentMethodsResponse(card1, card2)

            testContext.configure {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }

            state = testContext.embeddedPaymentElement.state
            assertThat(state.paymentMethods()).hasSize(2)

            testContext.markTestSucceeded()
        }

        // Instance 2 - no network requests, no configure call -- just a state set.
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
        ) { testContext ->
            testContext.embeddedPaymentElement.paymentOption.test {
                assertThat(awaitItem()).isNull()
                ensureAllEventsConsumed()
                withContext(Dispatchers.Main) {
                    testContext.embeddedPaymentElement.state = state
                }
                assertThat(awaitItem()?.paymentMethodType).isEqualTo("Card")
            }

            embeddedContentPage.clickViewMore()

            testContext.markTestSucceeded()
        }
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

private fun EmbeddedPaymentElement.State?.paymentMethods(): List<PaymentMethod>? {
    return this?.customer?.paymentMethods
}
