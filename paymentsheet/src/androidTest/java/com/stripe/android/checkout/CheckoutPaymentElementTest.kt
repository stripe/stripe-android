package com.stripe.android.checkout

import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.createPaymentMethod
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.utils.TestRules
import org.junit.After
import org.junit.Rule
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementTest {
    private val networkRule = NetworkRule(
        globalMatchers = arrayOf(header("Authorization", "Bearer pk_test_123"))
    )

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val contentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)

    @After
    fun teardown() {
        GooglePayRepository.resetFactory()
    }

    @Test
    fun testBackingOutOfFormPreservesPreviouslySelectedPaymentMethod() {
        runCheckoutPaymentElementTest(
            networkRule = networkRule,
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
            },
        ) { context ->
            // Open the card form, then back out without entering any details.
            contentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            Espresso.pressBack()
            formPage.waitUntilMissing()

            // Select a payment method that does not require a form.
            contentPage.clickOnLpm("cashapp")
            contentPage.assertHasSelectedLpm("cashapp")

            // Re-open the card form and back out again.
            contentPage.clickOnLpm("card")
            formPage.waitUntilVisible()
            Espresso.pressBack()
            formPage.waitUntilMissing()

            // Backing out of the form must not clear the previously selected payment method.
            contentPage.assertHasSelectedLpm("cashapp")
            context.markTestSucceeded()
        }
    }

    @Test
    fun testSuccessfulCardPayment() {
        var checkoutResult: CheckoutController.Result? = null
        runCheckoutPaymentElementTest(
            networkRule = networkRule,
            resultCallback = { result -> checkoutResult = result },
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET).getOrThrow()
            },
        ) { context ->
            networkRule.createPaymentMethod()
            networkRule.checkoutConfirm { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            contentPage.clickOnLpm("card")
            formPage.fillOutCardDetails()
            formPage.clickPrimaryButton()
            context.confirm()
        }

        assertThat(checkoutResult).isInstanceOf(CheckoutController.Result.Completed::class.java)
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
    }
}
