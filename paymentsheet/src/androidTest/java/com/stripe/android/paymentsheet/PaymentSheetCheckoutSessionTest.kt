package com.stripe.android.paymentsheet

import android.content.Context
import androidx.compose.ui.test.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.checkout.Checkout
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.checkouttesting.createPaymentMethod
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.not
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
import org.json.JSONObject
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
            saveEnabled = false,
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
            saveEnabled = false,
            confirmFile = "checkout-session-confirm-setup.json",
            noSaveCheckbox = true,
            expectedAllowRedisplay = "limited",
        )

    // endregion

    /**
     * Runs a checkout session test verifying that allow_redisplay is sent with the expected
     * value on PM creation. Injects customer and save offer fields into the init fixture
     * using JSON modification.
     */
    private fun runCheckoutSessionAllowRedisplayTest(
        initFile: String,
        confirmFile: String,
        saveEnabled: Boolean = true,
        clickSaveCheckbox: Boolean = false,
        noSaveCheckbox: Boolean = false,
        expectedAllowRedisplay: String,
    ) = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.checkoutInit { response ->
            response.testBodyFromFile(initFile) { json ->
                json.put("customer", JSONObject("""
                    {
                        "id": "cus_12345",
                        "payment_methods": [],
                        "can_detach_payment_method": true
                    }
                """.trimIndent()))
                json.put("customer_managed_saved_payment_methods_offer_save", JSONObject("""
                    {"enabled": $saveEnabled, "status": "not_accepted"}
                """.trimIndent()))
            }
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

    private suspend fun PaymentSheetTestRunnerContext.presentWithCheckout(
        configuration: PaymentSheet.Configuration = defaultConfiguration,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val checkout = Checkout.configure(
            context = context,
            checkoutSessionClientSecret = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
        ).getOrThrow()

        presentPaymentSheet {
            presentWithCheckout(
                checkout = checkout,
                configuration = configuration,
            )
        }
    }

    // region customer_email billing details tests

    @Test
    fun testCheckoutSessionCustomerEmailIsAttachedToPaymentMethod() = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init-with-email.json")
        }

        val noEmailConfiguration = PaymentSheet.Configuration.Builder("Test Merchant")
            .link(
                PaymentSheet.LinkConfiguration.Builder()
                    .display(PaymentSheet.LinkConfiguration.Display.Never)
                    .build()
            )
            .build()

        testContext.presentWithCheckout(configuration = noEmailConfiguration)

        page.fillOutCardDetails()

        networkRule.createPaymentMethod(
            bodyPart("billing_details[email]", "session@example.com"),
        )

        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        page.clickPrimaryButton()
    }

    @Test
    fun testMerchantEmailTakesPrecedenceOverCheckoutSessionEmail() = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init-with-email.json")
        }

        val merchantEmailConfiguration = PaymentSheet.Configuration.Builder("Test Merchant")
            .defaultBillingDetails(PaymentSheet.BillingDetails(email = "merchant@example.com"))
            .link(
                PaymentSheet.LinkConfiguration.Builder()
                    .display(PaymentSheet.LinkConfiguration.Display.Never)
                    .build()
            )
            .build()

        testContext.presentWithCheckout(configuration = merchantEmailConfiguration)

        page.fillOutCardDetails()

        networkRule.createPaymentMethod(
            bodyPart("billing_details[email]", "merchant@example.com"),
        )

        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        page.clickPrimaryButton()
    }

    // endregion

    // region SFU mandate tests

    /**
     * PMO SFU sets setup_future_usage at the payment_method_options level
     * (e.g., card and cashapp each have setup_future_usage: "off_session")
     * rather than at the top-level deferred intent.
     */
    @Test
    fun testMandateDisplayedWithPmoSfu() = runMandateTest { json ->
        json.getJSONObject("server_built_elements_session_params")
            .getJSONObject("deferred_intent")
            .put("payment_method_options", JSONObject("""
                {
                    "card": {"setup_future_usage": "off_session"},
                    "cashapp": {"setup_future_usage": "off_session"}
                }
            """.trimIndent()))
    }

    /**
     * Top-level SFU applies setup_future_usage: "off_session" to all payment methods
     * at the deferred intent level.
     */
    @Test
    fun testMandateDisplayedWithSfu() = runMandateTest { json ->
        json.getJSONObject("server_built_elements_session_params")
            .getJSONObject("deferred_intent")
            .put("setup_future_usage", "off_session")
    }

    /**
     * Mixed PMO SFU: card has setup_future_usage but cashapp has "none", which
     * overrides and suppresses the mandate for cashapp while card still shows it.
     */
    @Test
    fun testMandateOnlyForCardWithMixedPmoSfu() = runMandateTest(
        expectCashappMandate = false,
    ) { json ->
        json.getJSONObject("server_built_elements_session_params")
            .getJSONObject("deferred_intent")
            .put("payment_method_options", JSONObject("""
                {
                    "card": {"setup_future_usage": "off_session"},
                    "cashapp": {"setup_future_usage": "none"}
                }
            """.trimIndent()))
    }

    /**
     * Without any SFU configuration, no mandates should be displayed for card or cashapp.
     */
    @Test
    fun testNoMandateWithoutSfu() = runMandateTest(
        expectCardMandate = false,
        expectCashappMandate = false,
    )

    private fun runMandateTest(
        expectCardMandate: Boolean = true,
        expectCashappMandate: Boolean = true,
        jsonModifier: (JSONObject) -> Unit = {},
    ) = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::assertCompleted,
    ) { testContext ->
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json", jsonModifier)
        }

        testContext.presentWithCheckout()

        page.clickOnLpm("card")
        page.waitForCardForm()
        if (expectCardMandate) {
            page.assertHasMandate(CARD_SFU_MANDATE)
        } else {
            page.assertMandateIsMissing()
        }

        page.clickOnLpm("cashapp")
        if (expectCashappMandate) {
            page.assertHasMandate(CASHAPP_SFU_MANDATE)
        } else {
            composeTestRule.onNode(hasText(CASHAPP_SFU_MANDATE)).assertDoesNotExist()
        }

        testContext.markTestSucceeded()
    }

    // endregion

    private companion object {
        const val CARD_SFU_MANDATE =
            "By providing your card information, you allow Checkout Session Test to charge your card" +
                " for future payments in accordance with their terms."

        const val CASHAPP_SFU_MANDATE =
            "By continuing, you authorize Checkout Session Test to debit your Cash App account" +
                " for this payment and future payments in accordance with Checkout Session Test's" +
                " terms, until this authorization is revoked. You can change this anytime in your" +
                " Cash App Settings."
    }
}
