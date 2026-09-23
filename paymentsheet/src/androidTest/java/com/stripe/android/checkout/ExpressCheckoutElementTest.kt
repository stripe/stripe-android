package com.stripe.android.checkout

import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class ExpressCheckoutElementTest {
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
            around(FeatureFlagTestRule(FeatureFlags.nativeLinkEnabled, isEnabled = true))
            .around(IntentsRule())
    }

    private val page = ExpressCheckoutElementPage(testRules.compose)

    @Test
    fun testSuccessfulGooglePayPayment() {
        // This is called twice during load - once for ECE, once for PE
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).contains(
                    ExpressCheckoutElement.PaymentMethod.GooglePay()
                )
            },
        ) {
            val paymentMethod = PaymentMethodFactory.card()

            enqueueSuccessfulGooglePayPayment(
                paymentMethod = paymentMethod,
            )

            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                bodyPart("expected_amount", "5099"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testSuccessfulNativeLinkPayment() {
        // This is called twice during load - once for ECE, once for PE
        repeat (2) {
            networkRule.enqueueLinkAccountLookup()
        }

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Completed::class.java)
            },
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).contains(
                    ExpressCheckoutElement.PaymentMethod.Link()
                )
            },
        ) {
            enqueueSuccessfulNativeLinkPayment()

            repeat(2) {
                networkRule.enqueueLinkAccountLookup()
            }
            networkRule.checkoutInit()

            page.clickLinkButton()
        }

        assertNativeLinkCalled()
    }

    @Test
    fun testGooglePayOnlyLoad() {
        // This is called for PE, but we skip this account lookup for ECE since its Link config sets display to never.
        networkRule.enqueueLinkAccountLookup()

        runExpressCheckoutElementTest(
            networkRule = networkRule,
            assertions = { controller ->
                assertThat(
                    controller.session.value?.availableExpressCheckoutPaymentMethods
                ).containsExactly(
                    ExpressCheckoutElement.PaymentMethod.GooglePay()
                )
            },
            configurationUpdates = {
                it.linkConfiguration(
                    ExpressCheckoutElement.Configuration.LinkConfiguration()
                        .display(ExpressCheckoutElement.Configuration.LinkConfiguration.Display.Never)
                )
            }
        ) { testContext ->
            // Just testing load, no need to confirm.
            testContext.markTestSucceeded()
        }
    }

    @Test
    fun testFailedGooglePayPayment() {
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
        }

        val expectedErrorMessage = "Google Pay failed"
        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Failed::class.java)
                val error = (result as CheckoutController.Result.Failed).error
                assertThat(error).hasMessageThat().isEqualTo(expectedErrorMessage)
            },
        ) {
            enqueueFailedGooglePayPayment(IllegalStateException(expectedErrorMessage))

            repeat(2) {
                networkRule.enqueueLinkAccountLookup()
            }
            networkRule.checkoutInit()

            page.clickGooglePayButton()
        }

        assertGooglePayCalled()
    }

    @Test
    fun testFailedNativeLinkPayment() {
        repeat(2) {
            networkRule.enqueueLinkAccountLookup()
        }

        val expectedErrorMessage = "Link failed"
        runExpressCheckoutElementTest(
            networkRule = networkRule,
            resultCallback = { result ->
                assertThat(result).isInstanceOf(CheckoutController.Result.Failed::class.java)
                val error = (result as CheckoutController.Result.Failed).error
                assertThat(error).hasMessageThat().isEqualTo(expectedErrorMessage)
            },
        ) {
            enqueueFailedNativeLinkPayment(IllegalStateException(expectedErrorMessage))

            repeat(2) {
                networkRule.enqueueLinkAccountLookup()
            }
            networkRule.checkoutInit()

            page.clickLinkButton()
        }

        assertNativeLinkCalled()
    }
}
