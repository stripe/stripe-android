@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement

import androidx.test.espresso.Espresso
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
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
import org.junit.Rule
import org.junit.Test

internal class EmbeddedPaymentElementImmediateActionRowSelectionTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val managePage = ManagePage(testRules.compose)
    private val editPage = EditPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    private val card1 = CardPaymentMethodDetails("pm_12345", "4242")
    private val card2 = CardPaymentMethodDetails("pm_67890", "5544")

    private val _rowSelectionCalls = Turbine<RowSelectionCall>()
    private val rowSelectionCalls: ReceiveTurbine<RowSelectionCall> = _rowSelectionCalls

    @Test
    fun testSuccessfulCardPayment_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-requires_payment_method.json",
            configureBlock = {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickOnLpm("card")
            Espresso.onIdle()
            formPage.waitUntilVisible()
            formPage.fillOutCardDetails()
            formPage.clickPrimaryButton()
            formPage.waitUntilMissing()
            assertNextItemCardLabel("4242")

            enqueueDeferredIntentConfirmationRequests()

            testContext.confirm()
        }
    }

    @Test
    fun testSuccessfulCardPayment_withFormEdit_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-requires_payment_method.json",
            configureBlock = {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickOnLpm("card")
            Espresso.onIdle()
            formPage.waitUntilVisible()
            formPage.fillOutCardDetails("5555555555554444")
            formPage.clickPrimaryButton()
            formPage.waitUntilMissing()
            assertNextItemCardLabel("4444")

            embeddedContentPage.assertHasSelectedLpm("card")
            embeddedContentPage.clickOnLpm("card")
            Espresso.onIdle()
            formPage.waitUntilVisible()
            formPage.fillOutCardDetails("4242424242424242")
            formPage.clickPrimaryButton() // Ensures the form has the previous values.
            formPage.waitUntilMissing()
            assertNextItemCardLabel("4242")

            enqueueDeferredIntentConfirmationRequests()
            testContext.confirm()
        }
    }

    @Test
    fun testSuccessfulCardPayment_withReselection_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-requires_payment_method.json",
            configureBlock = {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickOnLpm("card")
            Espresso.onIdle()
            formPage.waitUntilVisible()
            formPage.fillOutCardDetails()
            formPage.clickPrimaryButton()
            formPage.waitUntilMissing()
            assertNextItemCardLabel("4242")

            embeddedContentPage.assertHasSelectedLpm("card")
            embeddedContentPage.clickOnLpm("card")
            Espresso.onIdle()
            formPage.waitUntilVisible()
            formPage.clickPrimaryButton() // Ensures the form has the previous values.
            formPage.waitUntilMissing()
            assertNextItemCardLabel("4242")

            enqueueDeferredIntentConfirmationRequests()
            testContext.confirm()
        }
    }

    @Test
    fun testSuccessfulCardPayment_withFormSheetActionConfirm() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-requires_payment_method.json",
            configureBlock = {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
            }
        ) { testContext ->
            embeddedContentPage.clickOnLpm("card")
            Espresso.onIdle()
            formPage.fillOutCardDetails()

            enqueueDeferredIntentConfirmationRequests()

            formPage.clickPrimaryButton()
            formPage.waitUntilMissing()

            // row selection should not be invoked
            rowSelectionCalls.expectNoEvents()

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testSuccessfulNonFormRowPayment_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-requires_payment_method.json",
            configureBlock = {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickOnLpm("cashapp")
            assertNextItemLabel("Cash App Pay")

            enqueueDeferredIntentConfirmationRequests()

            testContext.confirm()
        }
    }

    @Test
    fun testSuccessfulNonFormRowPayment_withReselection_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-requires_payment_method.json",
            configureBlock = {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickOnLpm("cashapp")
            assertNextItemLabel("Cash App Pay")

            Espresso.onIdle()
            embeddedContentPage.clickOnLpm("cashapp")
            assertNextItemLabel("Cash App Pay")

            enqueueDeferredIntentConfirmationRequests()

            testContext.confirm()
        }
    }

    @Test
    fun testSuccessfulNonFormRowPayment_withFormSheetActionConfirm() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-requires_payment_method.json",
            configureBlock = {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
            }
        ) { testContext ->
            embeddedContentPage.clickOnLpm("cashapp")
            assertNextItemLabel("Cash App Pay")

            enqueueDeferredIntentConfirmationRequests()
            testContext.confirm()
        }
    }

    @Test
    fun testSavedCardSelection_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-deferred_payment_intent_no_link.json",
            shouldSetupV1PaymentMethodsResponse = true,
            configureBlock = {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickViewMore()
            Espresso.onIdle()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            managePage.waitUntilNotVisible()
            assertNextItemCardLabel("4242")

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testSavedCardChangeSelection_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-deferred_payment_intent_no_link.json",
            shouldSetupV1PaymentMethodsResponse = true,
            configureBlock = {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickViewMore()
            Espresso.onIdle()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            managePage.waitUntilNotVisible()
            assertNextItemCardLabel("4242")

            embeddedContentPage.clickViewMore()
            Espresso.onIdle()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card2.id)
            managePage.waitUntilNotVisible()
            assertNextItemCardLabel("5544")

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testSavedCardReselection_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-deferred_payment_intent_no_link.json",
            shouldSetupV1PaymentMethodsResponse = true,
            configureBlock = {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickViewMore()
            Espresso.onIdle()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            managePage.waitUntilNotVisible()
            assertNextItemCardLabel("4242")

            embeddedContentPage.clickViewMore()
            Espresso.onIdle()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            managePage.waitUntilNotVisible()
            assertNextItemCardLabel("4242")

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testSavedCardRowSelection_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-deferred_payment_intent_no_link.json",
            shouldSetupV1PaymentMethodsResponse = true,
            configureBlock = {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickOnSavedPM(card1.id)
            assertNextItemCardLabel("4242")

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testSavedCardRowReselection_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-deferred_payment_intent_no_link.json",
            shouldSetupV1PaymentMethodsResponse = true,
            configureBlock = {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickOnSavedPM(card1.id)
            assertNextItemCardLabel("4242")

            Espresso.onIdle()
            embeddedContentPage.clickOnSavedPM(card1.id)
            assertNextItemCardLabel("4242")

            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testRemoveCard_doesNotInvokeCallback() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-deferred_payment_intent_no_link.json",
            shouldSetupV1PaymentMethodsResponse = true,
            configureBlock = {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
            }
        ) { testContext ->
            embeddedContentPage.clickViewMore()
            Espresso.onIdle()
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(card1.id)
            editPage.waitUntilVisible()

            networkRule.setupPaymentMethodDetachResponse(card1.id)

            editPage.clickRemove()
            Espresso.onIdle()
            managePage.waitUntilVisible()
            managePage.waitUntilGone(card1.id)
            managePage.clickDone()

            rowSelectionCalls.expectNoEvents()

            testContext.markTestSucceeded()
        }
    }

    private fun runEmbeddedPaymentElementRowSelectionTest(
        responseTestBodyFileName: String,
        shouldSetupV1PaymentMethodsResponse: Boolean = false,
        configureBlock: EmbeddedPaymentElement.Configuration.Builder.() -> EmbeddedPaymentElement.Configuration.Builder = { this },
        testBlock: suspend (EmbeddedPaymentElementTestRunnerContext) -> Unit,
    ) {
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = ::assertCompleted,
            rowSelectionBehavior = EmbeddedPaymentElement.RowSelectionBehavior.ImmediateAction { embeddedPaymentElement ->
                val paymentOption = embeddedPaymentElement.paymentOption.value
                _rowSelectionCalls.add(
                    RowSelectionCall(
                        paymentOptionLabel = paymentOption?.label.toString(),
                    )
                )
            },
        ) { testContext ->
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { response ->
                response.testBodyFromFile(responseTestBodyFileName)
            }
            if (shouldSetupV1PaymentMethodsResponse) {
                networkRule.setupV1PaymentMethodsResponse(card1, card2)
            }
            testContext.configure {
                configureBlock()
            }
            testBlock(testContext)
        }
    }

    private suspend fun assertNextItemLabel(label: String) {
        assertThat(rowSelectionCalls.awaitItem().paymentOptionLabel).isEqualTo(label)
    }

    private suspend fun assertNextItemCardLabel(last4: String) {
        assertThat(rowSelectionCalls.awaitItem().paymentOptionLabel).isEqualTo(getCardLabel(last4))
    }

    private fun getCardLabel(last4: String): String {
        return "···· $last4"
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

    data class RowSelectionCall(
        val paymentOptionLabel: String?,
    )
}
