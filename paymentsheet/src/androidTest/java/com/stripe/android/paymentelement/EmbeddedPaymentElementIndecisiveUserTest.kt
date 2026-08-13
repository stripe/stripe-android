package com.stripe.android.paymentelement

import androidx.test.espresso.Espresso
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.elementsSession
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.paymentelementnetwork.CardPaymentMethodDetails
import com.stripe.paymentelementnetwork.setupV1PaymentMethodsResponse
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.ManagePage
import org.junit.After
import org.junit.Rule
import org.junit.Test

internal class EmbeddedPaymentElementIndecisiveUserTest {
    private val networkRule = NetworkRule(
        defaultMatcher = header("Authorization", "Bearer pk_test_123"),
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val managePage = ManagePage(testRules.compose)
    private val editPage = EditPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    private val card1 = CardPaymentMethodDetails("pm_12345", "4242")
    private val card2 = CardPaymentMethodDetails("pm_67890", "5544")

    @After
    fun teardown() {
        GooglePayRepository.resetFactory()
    }

    @Test
    fun testRepeatedFormCancellationAllowsLaterCompletion() = runScenario {
        repeat(3) {
            embeddedContentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            Espresso.pressBack()
            formPage.waitUntilMissing()

            resultCalls.assertCanceled()
        }

        embeddedContentPage.clickOnLpm("card")
        formPage.fillOutCardDetails()
        formPage.clickPrimaryButton()
        testContext.consumePaymentOptionEvent("card", "4242")

        enqueueNewPaymentMethodConfirmationRequests()
        testContext.confirm()
        resultCalls.assertCompleted()
        assertThat(testContext.paymentOptionTurbine.awaitItem()).isNull()
    }

    @Test
    fun testRepeatedManageNavigationAllowsLaterCompletion() = runScenario {
        repeat(3) {
            embeddedContentPage.clickViewMore()
            managePage.waitUntilVisible()

            Espresso.pressBack()
            managePage.waitUntilNotVisible()

            resultCalls.expectNoEvents()
        }

        embeddedContentPage.assertHasSelectedSavedPaymentMethod(card1.id)

        enqueueSavedPaymentMethodConfirmationRequests()
        testContext.confirm()

        resultCalls.assertCompleted()
        assertThat(testContext.paymentOptionTurbine.awaitItem()).isNull()
    }

    @Test
    fun testFormCancellationPreservesChangingSelections() = runScenario {
        embeddedContentPage.clickOnLpm("cashapp")
        testContext.consumePaymentOptionEvent("cashapp", "Cash App Pay")
        embeddedContentPage.assertHasSelectedLpm("cashapp")

        embeddedContentPage.clickOnLpm("card")
        formPage.waitUntilVisible()
        Espresso.pressBack()
        formPage.waitUntilMissing()

        resultCalls.assertCanceled()
        embeddedContentPage.assertHasSelectedLpm("cashapp")
        testContext.paymentOptionTurbine.expectNoEvents()

        embeddedContentPage.clickOnSavedPM(card1.id)
        testContext.consumePaymentOptionEvent("card", "4242")
        embeddedContentPage.assertHasSelectedSavedPaymentMethod(card1.id)

        embeddedContentPage.clickOnLpm("card")
        formPage.waitUntilVisible()
        Espresso.pressBack()
        formPage.waitUntilMissing()

        resultCalls.assertCanceled()
        embeddedContentPage.assertHasSelectedSavedPaymentMethod(card1.id)
        testContext.paymentOptionTurbine.expectNoEvents()
    }

    @Test
    fun testManageAndEditNavigationAllowsLaterCompletion() = runScenario {
        embeddedContentPage.clickViewMore()
        managePage.waitUntilVisible()

        managePage.clickEdit()
        managePage.clickEdit(card1.id)
        editPage.waitUntilVisible()
        Espresso.pressBack()
        editPage.waitUntilMissing()
        managePage.waitUntilVisible()

        managePage.clickEdit(card2.id)
        editPage.waitUntilVisible()
        Espresso.pressBack()
        editPage.waitUntilMissing()
        managePage.waitUntilVisible()

        managePage.clickDone()
        Espresso.pressBack()
        managePage.waitUntilNotVisible()

        resultCalls.expectNoEvents()
        embeddedContentPage.assertHasSelectedSavedPaymentMethod(card1.id)

        embeddedContentPage.clickOnLpm("card")
        formPage.waitUntilVisible()
        Espresso.pressBack()
        formPage.waitUntilMissing()

        resultCalls.assertCanceled()
        embeddedContentPage.assertHasSelectedSavedPaymentMethod(card1.id)

        enqueueSavedPaymentMethodConfirmationRequests()
        testContext.confirm()

        resultCalls.assertCompleted()
        assertThat(testContext.paymentOptionTurbine.awaitItem()).isNull()
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) {
        val resultCalls = Turbine<EmbeddedPaymentElement.Result>()
        runEmbeddedPaymentElementTest(
            networkRule = networkRule,
            createIntentCallback = { _, shouldSavePaymentMethod ->
                assertThat(shouldSavePaymentMethod).isFalse()
                CreateIntentResult.Success("pi_example_secret_12345")
            },
            resultCallback = resultCalls::add,
        ) { testContext ->
            networkRule.elementsSession { response ->
                response.testBodyFromFile("elements-sessions-deferred_payment_intent_no_link.json")
            }
            networkRule.setupV1PaymentMethodsResponse(card1, card2)
            networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.USBankAccount.code)
            networkRule.setupV1PaymentMethodsResponse(type = PaymentMethod.Type.SepaDebit.code)

            testContext.configure {
                customer(PaymentSheet.CustomerConfiguration("cus_123", "ek_test"))
            }
            testContext.consumePaymentOptionEvent("card", "4242")

            Scenario(
                testContext = testContext,
                resultCalls = resultCalls,
            ).block()

            resultCalls.ensureAllEventsConsumed()
        }
    }

    private fun enqueueNewPaymentMethodConfirmationRequests() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
        enqueueSavedPaymentMethodConfirmationRequests()
    }

    private fun enqueueSavedPaymentMethodConfirmationRequests() {
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

    private suspend fun ReceiveTurbine<EmbeddedPaymentElement.Result>.assertCanceled() {
        assertThat(awaitItem()).isInstanceOf(EmbeddedPaymentElement.Result.Canceled::class.java)
    }

    private suspend fun ReceiveTurbine<EmbeddedPaymentElement.Result>.assertCompleted() {
        assertThat(awaitItem()).isInstanceOf(EmbeddedPaymentElement.Result.Completed::class.java)
    }

    private data class Scenario(
        val testContext: EmbeddedPaymentElementTestRunnerContext,
        val resultCalls: ReceiveTurbine<EmbeddedPaymentElement.Result>,
    )
}
