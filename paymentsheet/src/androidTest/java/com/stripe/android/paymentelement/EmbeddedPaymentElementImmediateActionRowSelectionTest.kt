package com.stripe.android.paymentelement

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

    @Test
    fun testSuccessfulCardPayment_withFormSheetActionContinue() {
        runEmbeddedPaymentElementRowSelectionTest(
            responseTestBodyFileName = "elements-sessions-requires_payment_method.json",
            configureBlock = {
                formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            }
        ) { testContext ->
            embeddedContentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            formPage.fillOutCardDetails()
            formPage.clickPrimaryButton()
            testContext.assertNextCardRowSelectionItem("4242")

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
            formPage.waitUntilVisible()
            formPage.fillOutCardDetails("5555555555554444")
            formPage.clickPrimaryButton()
            testContext.assertNextCardRowSelectionItem("4444")

            formPage.waitUntilMissing()
            embeddedContentPage.assertHasSelectedLpm("card")
            embeddedContentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            formPage.fillOutCardDetails("4242424242424242")
            formPage.clickPrimaryButton()
            testContext.assertNextCardRowSelectionItem("4242")

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
            formPage.waitUntilVisible()
            formPage.fillOutCardDetails()
            formPage.clickPrimaryButton()
            testContext.assertNextCardRowSelectionItem("4242")

            formPage.waitUntilMissing()
            embeddedContentPage.assertHasSelectedLpm("card")
            embeddedContentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            formPage.clickPrimaryButton()
            testContext.assertNextCardRowSelectionItem("4242")

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
            formPage.fillOutCardDetails()

            enqueueDeferredIntentConfirmationRequests()

            formPage.clickPrimaryButton()

            // rowSelectionCallback should not be invoked because formSheetAction = confirm.
            testContext.rowSelectionCalls.expectNoEvents()

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
            testContext.assertNextRowSelectionItem("cashapp", "Cash App Pay")

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
            testContext.assertNextRowSelectionItem("cashapp", "Cash App Pay")

            embeddedContentPage.clickOnLpm("cashapp")
            testContext.assertNextRowSelectionItem("cashapp", "Cash App Pay")

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
            testContext.assertNextRowSelectionItem("cashapp", "Cash App Pay")

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
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            testContext.assertNextCardRowSelectionItem("4242")

            enqueueSavedCardIntentConfirmationRequests()
            testContext.confirm()
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
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            testContext.assertNextCardRowSelectionItem("4242")

            managePage.waitUntilNotVisible()
            embeddedContentPage.clickViewMore()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card2.id)
            testContext.assertNextCardRowSelectionItem("5544")

            enqueueSavedCardIntentConfirmationRequests()
            testContext.confirm()
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
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            testContext.assertNextCardRowSelectionItem("4242")

            managePage.waitUntilNotVisible()
            embeddedContentPage.clickViewMore()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(card1.id)
            testContext.assertNextCardRowSelectionItem("4242")

            enqueueSavedCardIntentConfirmationRequests()
            testContext.confirm()
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
            testContext.assertNextCardRowSelectionItem("4242")

            enqueueSavedCardIntentConfirmationRequests()
            testContext.confirm()
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
            testContext.assertNextCardRowSelectionItem("4242")

            embeddedContentPage.clickOnSavedPM(card1.id)
            testContext.assertNextCardRowSelectionItem("4242")

            enqueueSavedCardIntentConfirmationRequests()
            testContext.confirm()
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
            managePage.waitUntilVisible()
            managePage.clickEdit()
            managePage.clickEdit(card1.id)
            editPage.waitUntilVisible()

            networkRule.setupPaymentMethodDetachResponse(card1.id)

            editPage.clickRemove()
            managePage.waitUntilVisible()
            managePage.waitUntilGone(card1.id)
            managePage.clickDone()

            testContext.rowSelectionCalls.expectNoEvents()

            testContext.markTestSucceeded()
        }
    }

    private fun runEmbeddedPaymentElementRowSelectionTest(
        responseTestBodyFileName: String,
        shouldSetupV1PaymentMethodsResponse: Boolean = false,
        configureBlock: EmbeddedPaymentElement.Configuration.Builder.() -> EmbeddedPaymentElement.Configuration.Builder = { this },
        testBlock: suspend (EmbeddedPaymentElementTestRunnerContext) -> Unit,
    ) {
        val rowSelectionCalls = Turbine<RowSelectionCall>()
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            builder = {
                rowSelectionBehavior =
                    EmbeddedPaymentElement.RowSelectionBehavior.immediateAction { embeddedPaymentElement ->
                        val paymentOption = embeddedPaymentElement.paymentOption.value
                        rowSelectionCalls.add(
                            RowSelectionCall(
                                paymentMethodType = paymentOption?.paymentMethodType,
                                paymentOptionLabel = paymentOption?.label.toString(),
                            )
                        )
                    }
            },
            rowSelectionCalls = rowSelectionCalls,
            resultCallback = ::assertCompleted,
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

    private suspend fun EmbeddedPaymentElementTestRunnerContext.assertNextRowSelectionItem(
        paymentMethodType: String,
        label: String
    ) {
        val nextItem = rowSelectionCalls.awaitItem()
        assertThat(nextItem.paymentMethodType).isEqualTo(paymentMethodType)
        assertThat(nextItem.paymentOptionLabel).isEqualTo(label)
    }

    private suspend fun EmbeddedPaymentElementTestRunnerContext.assertNextCardRowSelectionItem(last4: String) {
        val nextItem = rowSelectionCalls.awaitItem()
        assertThat(nextItem.paymentMethodType).isEqualTo("card")
        assertThat(nextItem.paymentOptionLabel).isEqualTo(getCardLabel(last4))
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

    private fun enqueueSavedCardIntentConfirmationRequests() {
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
