package com.stripe.android.paymentsheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.assertFailed
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
internal class PaymentSheetCheckoutSessionTest {
    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val composeTestRule = testRules.compose
    private val networkRule = testRules.networkRule

    private val page: PaymentSheetPage = PaymentSheetPage(composeTestRule)

    private val defaultConfiguration = PaymentSheet.Configuration.Builder(
        merchantDisplayName = "Checkout Session Test",
    ).paymentMethodLayout(paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal)
        .defaultBillingDetails(PaymentSheet.BillingDetails(email = "email@email.com"))
        .link(PaymentSheet.LinkConfiguration.Builder().display(PaymentSheet.LinkConfiguration.Display.Never).build())
        .build()

    /**
     * Test a successful card setup flow with checkout session (setup mode).
     *
     * Flow:
     * 1. Present PaymentSheet with checkout session client secret
     * 2. Initialize checkout session in setup mode (POST /v1/payment_pages/{cs_id}/init)
     * 3. Fill out card details
     * 4. Create payment method (POST /v1/payment_methods)
     * 5. Confirm checkout session — returns setup_intent (POST /v1/payment_pages/{cs_id}/confirm)
     * 6. Verify setup completed successfully
     */
    @Test
    fun testSuccessfulCardSetupWithCheckoutSession() = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init-setup.json")
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val checkout = Checkout.configure(
            context = context,
            checkoutSessionClientSecret = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
        ).getOrThrow()

        testContext.presentPaymentSheet {
            presentWithCheckout(
                checkout = checkout,
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        networkRule.checkoutConfirm(
            not(hasBodyPart("expected_amount")),
            not(hasBodyPart("save_payment_method")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm-setup.json")
        }

        page.clickPrimaryButton()
    }

    /**
     * Test a successful card payment flow with checkout session.
     *
     * Flow:
     * 1. Present PaymentSheet with checkout session client secret
     * 2. Initialize checkout session (POST /v1/payment_pages/{cs_id}/init)
     * 3. Fill out card details
     * 4. Create payment method (POST /v1/payment_methods)
     * 5. Confirm checkout session (POST /v1/payment_pages/{cs_id}/confirm)
     * 6. Verify payment completed successfully
     */
    @Test
    fun testSuccessfulCardPaymentWithCheckoutSession() = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        // Mock checkout session init API
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val checkout = Checkout.configure(
            context = context,
            checkoutSessionClientSecret = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
        ).getOrThrow()

        testContext.presentPaymentSheet {
            presentWithCheckout(
                checkout = checkout,
                configuration = defaultConfiguration,
            )
        }

        page.fillOutCardDetails()

        // Mock payment method creation
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods"),
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }

        // Mock checkout session confirm API
        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "5099"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        page.clickPrimaryButton()
    }

    /**
     * Test that PaymentSheet fails to load when the init response already contains a confirmed
     * payment intent alongside the elements_session.
     *
     * When the parser sees both elements_session and payment_intent, it replaces the deferred
     * intent stub in elements_session with the confirmed intent. Since the confirmed intent is
     * in a terminal state (status = "succeeded"), StripeIntentValidator rejects it and
     * PaymentSheet reports a failure.
     */
    @Test
    fun testFailsToLoadWhenInitResponseContainsConfirmedIntent() = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertFailed,
    ) { testContext ->
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init-already-confirmed.json")
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val checkout = Checkout.configure(
            context = context,
            checkoutSessionClientSecret = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
        ).getOrThrow()

        testContext.presentPaymentSheet {
            presentWithCheckout(
                checkout = checkout,
                configuration = defaultConfiguration,
            )
        }
    }

    // region allow_redisplay tests

    @Test
    fun allowRedisplayIsUnspecifiedWhenNotSavingWithPayment() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init-with-customer.json",
            confirmFile = "checkout-session-confirm.json",
            expectedAllowRedisplay = "unspecified",
        )

    @Test
    fun allowRedisplayIsAlwaysWhenSavingWithPayment() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init-with-customer.json",
            confirmFile = "checkout-session-confirm.json",
            clickSaveCheckbox = true,
            expectedAllowRedisplay = "always",
        )

    @Test
    fun allowRedisplayIsUnspecifiedWhenSaveDisabledWithPayment() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init-with-customer.json",
            initReplacements = listOf(SAVE_DISABLED_REPLACEMENT),
            confirmFile = "checkout-session-confirm.json",
            expectedAllowRedisplay = "unspecified",
        )

    @Test
    fun allowRedisplayIsLimitedWhenNotSavingWithSetup() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init-setup-with-customer.json",
            confirmFile = "checkout-session-confirm-setup.json",
            expectedAllowRedisplay = "limited",
        )

    @Test
    fun allowRedisplayIsAlwaysWhenSavingWithSetup() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init-setup-with-customer.json",
            confirmFile = "checkout-session-confirm-setup.json",
            clickSaveCheckbox = true,
            expectedAllowRedisplay = "always",
        )

    @Test
    fun allowRedisplayIsLimitedWhenSaveDisabledWithSetup() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init-setup-with-customer.json",
            initReplacements = listOf(SAVE_DISABLED_REPLACEMENT),
            confirmFile = "checkout-session-confirm-setup.json",
            expectedAllowRedisplay = "limited",
        )

    // endregion

    private fun runCheckoutSessionAllowRedisplayTest(
        initFile: String,
        confirmFile: String,
        initReplacements: List<ResponseReplacement> = emptyList(),
        clickSaveCheckbox: Boolean = false,
        expectedAllowRedisplay: String,
    ) = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.checkoutInit { response ->
            response.testBodyFromFile(initFile, initReplacements)
        }

        testContext.presentWithCheckout()

        page.fillOutCardDetails()

        if (clickSaveCheckbox) {
            page.clickOnSaveForFutureUsage()
        }

        enqueuePaymentMethodCreation(
            bodyPart("allow_redisplay", expectedAllowRedisplay),
        )

        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile(confirmFile)
        }

        page.clickPrimaryButton()
    }

    private suspend fun PaymentSheetTestRunnerContext.presentWithCheckout() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val checkout = Checkout.configure(
            context = context,
            checkoutSessionClientSecret = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
        ).getOrThrow()

        presentPaymentSheet {
            presentWithCheckout(
                checkout = checkout,
                configuration = defaultConfiguration,
            )
        }
    }

    private fun enqueuePaymentMethodCreation(vararg requestMatchers: RequestMatcher) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods"),
            *requestMatchers,
        ) { response ->
            response.testBodyFromFile("payment-methods-create.json")
        }
    }

    private companion object {
        val SAVE_DISABLED_REPLACEMENT = ResponseReplacement(
            original = "\"enabled\": true",
            new = "\"enabled\": false",
        )
    }
}
