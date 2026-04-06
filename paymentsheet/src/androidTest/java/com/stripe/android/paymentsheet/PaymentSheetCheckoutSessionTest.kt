package com.stripe.android.paymentsheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.checkouttesting.createPaymentMethod
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.utils.PaymentSheetTestRunnerContext
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.assertFailed
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import com.stripe.paymentelementtestpages.CurrencySelector
import com.stripe.paymentelementtestpages.FormPage
import com.stripe.paymentelementtestpages.VerticalModePage
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
    private val formPage: FormPage = FormPage(composeTestRule)
    private val verticalModePage: VerticalModePage = VerticalModePage(composeTestRule)
    private val currencySelector: CurrencySelector = CurrencySelector(composeTestRule)

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

        networkRule.createPaymentMethod()

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
        networkRule.createPaymentMethod()

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

    @Test
    fun testAdaptivePricingShowsCorrectPageBasedOnNumberOfLPMsAvailable() = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-adaptive-pricing-default.json")
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val checkout = Checkout.configure(
            context = context,
            checkoutSessionClientSecret = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
        ).getOrThrow()

        testContext.presentPaymentSheet {
            presentWithCheckout(
                checkout = checkout,
                configuration = defaultConfiguration.newBuilder()
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
                    .build(),
            )
        }

        verticalModePage.waitUntilVisible()

        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-adaptive-pricing-integration-currency.json")
        }
        currencySelector.assertCurrencyOptionIsSelected("EUR")
        currencySelector.clickCurrencyOption("USD")

        formPage.waitUntilVisible()
        currencySelector.assertCurrencyOptionIsSelected("USD")
        formPage.fillOutCardDetails()

        networkRule.createPaymentMethod()

        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "5099"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        page.clickPrimaryButton()
    }

    // region allow_redisplay tests

    @Test
    fun allowRedisplayIsUnspecifiedWhenNotSavingWithPayment() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init.json",
            confirmFile = "checkout-session-confirm.json",
            expectedAllowRedisplay = "unspecified",
        )

    @Test
    fun allowRedisplayIsAlwaysWhenSavingWithPayment() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init.json",
            confirmFile = "checkout-session-confirm.json",
            clickSaveCheckbox = true,
            expectedAllowRedisplay = "always",
        )

    @Test
    fun allowRedisplayIsUnspecifiedWhenSaveDisabledWithPayment() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init.json",
            initReplacements = listOf(SAVE_DISABLED_REPLACEMENT),
            confirmFile = "checkout-session-confirm.json",
            noSaveCheckbox = true,
            expectedAllowRedisplay = "unspecified",
        )

    @Test
    fun allowRedisplayIsLimitedWhenNotSavingWithSetup() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init-setup.json",
            confirmFile = "checkout-session-confirm-setup.json",
            expectedAllowRedisplay = "limited",
        )

    @Test
    fun allowRedisplayIsAlwaysWhenSavingWithSetup() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init-setup.json",
            confirmFile = "checkout-session-confirm-setup.json",
            clickSaveCheckbox = true,
            expectedAllowRedisplay = "always",
        )

    @Test
    fun allowRedisplayIsLimitedWhenSaveDisabledWithSetup() =
        runCheckoutSessionAllowRedisplayTest(
            initFile = "checkout-session-init-setup.json",
            initReplacements = listOf(SAVE_DISABLED_REPLACEMENT),
            confirmFile = "checkout-session-confirm-setup.json",
            noSaveCheckbox = true,
            expectedAllowRedisplay = "limited",
        )

    // endregion

    /**
     * Runs a checkout session test verifying that allow_redisplay is sent with the expected
     * value on PM creation. CUSTOMER_REPLACEMENT is always applied first to inject the customer
     * and save offer fields; [initReplacements] (e.g. SAVE_DISABLED_REPLACEMENT) are applied
     * after and may depend on fields injected by CUSTOMER_REPLACEMENT.
     */
    private fun runCheckoutSessionAllowRedisplayTest(
        initFile: String,
        confirmFile: String,
        initReplacements: List<ResponseReplacement> = emptyList(),
        clickSaveCheckbox: Boolean = false,
        noSaveCheckbox: Boolean = false,
        expectedAllowRedisplay: String,
    ) = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.checkoutInit { response ->
            response.testBodyFromFile(initFile, listOf(CUSTOMER_REPLACEMENT) + initReplacements)
        }

        testContext.presentWithCheckout()

        page.fillOutCardDetails()

        if (noSaveCheckbox) {
            page.assertNoSaveForFutureCheckbox()
        } else if (clickSaveCheckbox) {
            page.clickOnSaveForFutureUsage()
        }

        networkRule.createPaymentMethod(
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

    private companion object {
        // Injects customer + save offer fields into base checkout-session-init fixtures.
        // Note: testBodyFromFile concatenates lines without newlines, so all JSON ends up
        // on one line. We anchor on the unique "ui_mode" field and append inline.
        val CUSTOMER_REPLACEMENT = ResponseReplacement(
            original = """"ui_mode": "custom",""",
            new = """"ui_mode": "custom", """ +
                """"customer": {"id": "cus_12345", "payment_methods": [], "can_detach_payment_method": true}, """ +
                """"customer_managed_saved_payment_methods_offer_save": {"enabled": true, "status": "not_accepted"},""",
        )

        // Must be applied after CUSTOMER_REPLACEMENT, which injects the field this matches on.
        val SAVE_DISABLED_REPLACEMENT = ResponseReplacement(
            original = """"customer_managed_saved_payment_methods_offer_save": {"enabled": true,""",
            new = """"customer_managed_saved_payment_methods_offer_save": {"enabled": false,""",
        )
    }
}
