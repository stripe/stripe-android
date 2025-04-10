@file:OptIn(ExperimentalAnalyticEventCallbackApi::class, ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement

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

internal class EmbeddedPaymentElementTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val embeddedContentPage = EmbeddedContentPage(testRules.compose)
    private val managePage = ManagePage(testRules.compose)
    private val editPage = EditPage(testRules.compose)

    private val card1 = CardPaymentMethodDetails("pm_12345", "4242")
    private val card2 = CardPaymentMethodDetails("pm_67890", "5544")

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
}
