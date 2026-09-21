package com.stripe.android.checkout

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.elements.PaymentElement
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.paymentelementtestpages.ManagePage
import com.stripe.paymentelementtestpages.VerticalModePage
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementSavedSepaTest {
    private val networkRule = NetworkRule(validationTimeout = 5.seconds)

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val contentPage = EmbeddedContentPage(testRules.compose)
    private val managePage = ManagePage(testRules.compose)
    private val verticalModePage = VerticalModePage(testRules.compose)

    @Test
    fun testPreselectedSavedSepaDisplaysMandateBeforeConfirmation() {
        runSavedSepaTest(
            paymentMethods = savedPaymentMethods(SEPA_PAYMENT_METHOD, CARD_PAYMENT_METHOD),
            renderPaymentElementContent = false,
        ) { context ->
            context.confirm()
            waitForSepaMandate()

            enqueueConfirmation()
            testRules.compose.onNodeWithTag(SEPA_MANDATE_CONTINUE_BUTTON).performClick()
        }
    }

    @Test
    fun testPreselectedSavedSepaConfirmsDirectlyWhenEmbeddedMandateIsDisabled() {
        val configuration = checkoutConfiguration(
            PaymentElement.Configuration().embeddedViewDisplaysMandateText(false)
        )

        runSavedSepaTest(
            paymentMethods = savedPaymentMethods(SEPA_PAYMENT_METHOD, CARD_PAYMENT_METHOD),
            configuration = configuration,
        ) { context ->
            contentPage.assertHasSelectedSavedPaymentMethod(SEPA_PAYMENT_METHOD_ID)

            enqueueConfirmation()
            context.confirm()
        }
    }

    @Test
    fun testReselectedSavedSepaConfirmsWithoutAnotherMandateScreen() {
        runSavedSepaTest(paymentMethods = savedPaymentMethods(CARD_PAYMENT_METHOD, SEPA_PAYMENT_METHOD)) { context ->
            contentPage.assertHasSelectedSavedPaymentMethod(CARD_PAYMENT_METHOD_ID)
            contentPage.clickViewMore()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(SEPA_PAYMENT_METHOD_ID)
            managePage.waitUntilNotVisible()
            contentPage.assertHasSelectedSavedPaymentMethod(SEPA_PAYMENT_METHOD_ID)

            enqueueConfirmation()
            context.confirm()
        }
    }

    @Test
    fun testSavedSepaSelectedInPaymentOptionsConfirmsDirectlyAfterContinue() {
        val configuration = checkoutConfiguration(
            PaymentElement.Configuration().paymentMethodLayout(
                PaymentElement.Configuration.PaymentMethodLayout.Vertical
            )
        )

        runSavedSepaTest(
            paymentMethods = savedPaymentMethods(CARD_PAYMENT_METHOD, SEPA_PAYMENT_METHOD),
            configuration = configuration,
        ) { context ->
            contentPage.assertHasSelectedSavedPaymentMethod(CARD_PAYMENT_METHOD_ID)
            context.presentPaymentOptions()

            verticalModePage.waitUntilVisible()
            verticalModePage.clickViewMore()
            managePage.waitUntilVisible()
            managePage.selectPaymentMethod(SEPA_PAYMENT_METHOD_ID)
            managePage.waitUntilNotVisible()
            verticalModePage.assertHasSelectedSavedPaymentMethod(SEPA_PAYMENT_METHOD_ID)
            clickPaymentOptionsPrimaryButton()
            verticalModePage.waitUntilMissing()
            contentPage.assertHasSelectedSavedPaymentMethod(SEPA_PAYMENT_METHOD_ID)

            enqueueConfirmation()
            context.confirm()
        }
    }

    private fun runSavedSepaTest(
        paymentMethods: JSONArray,
        configuration: CheckoutController.Configuration = checkoutConfiguration(PaymentElement.Configuration()),
        renderPaymentElementContent: Boolean = true,
        block: (CheckoutPaymentElementTestRunnerContext) -> Unit,
    ) {
        var checkoutResult: CheckoutController.Result? = null
        runCheckoutPaymentElementTest(
            networkRule = networkRule,
            resultCallback = { result -> checkoutResult = result },
            checkoutInitResponse = checkoutInitResponse(paymentMethods),
            renderPaymentElementContent = renderPaymentElementContent,
            setup = { controller ->
                controller.configure(DEFAULT_CLIENT_SECRET, configuration).getOrThrow()
            },
            block = block,
        )

        assertThat(checkoutResult).isInstanceOf(CheckoutController.Result.Completed::class.java)
    }

    private fun checkoutInitResponse(paymentMethods: JSONArray): (MockResponse) -> Unit = { response ->
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.put(
                "customer",
                JSONObject()
                    .put("id", "cus_saved_sepa")
                    .put("payment_methods", paymentMethods)
                    .put("can_detach_payment_method", true),
            )
            json.getJSONObject("elements_session").apply {
                remove("link_settings")
                put(
                    "ordered_payment_method_types_and_wallets",
                    JSONArray().put("card").put("sepa_debit"),
                )
                getJSONObject("payment_method_preference").put(
                    "ordered_payment_method_types",
                    JSONArray().put("card").put("sepa_debit"),
                )
            }
            json.getJSONObject("server_built_elements_session_params")
                .getJSONObject("deferred_intent")
                .put("payment_method_types", JSONArray().put("card").put("sepa_debit"))
        }
    }

    private fun enqueueConfirmation() {
        networkRule.checkoutConfirm(
            bodyPart("payment_method", SEPA_PAYMENT_METHOD_ID),
            bodyPart("expected_amount", EXPECTED_AMOUNT),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }
    }

    private fun waitForSepaMandate() {
        testRules.compose.waitUntil(timeoutMillis = 5_000) {
            testRules.compose.onAllNodes(hasTestTag(SEPA_MANDATE_CONTINUE_BUTTON))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        testRules.compose.onNodeWithTag(SEPA_MANDATE_CONTINUE_BUTTON).assertExists()
    }

    private fun clickPaymentOptionsPrimaryButton() {
        testRules.compose.waitUntil(timeoutMillis = 5_000) {
            testRules.compose.onAllNodes(
                hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG).and(isEnabled())
            ).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        testRules.compose.onNodeWithTag(SHEET_PRIMARY_BUTTON_TEST_TAG)
            .performScrollTo()
            .performClick()
    }

    private fun checkoutConfiguration(
        paymentElementConfiguration: PaymentElement.Configuration,
    ): CheckoutController.Configuration {
        return CheckoutController.Configuration().paymentElement(paymentElementConfiguration)
    }

    private fun savedPaymentMethods(vararg paymentMethods: JSONObject): JSONArray {
        return JSONArray().apply {
            paymentMethods.forEach(::put)
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val CARD_PAYMENT_METHOD_ID = "pm_card"
        const val SEPA_PAYMENT_METHOD_ID = "pm_sepa_debit"
        const val EXPECTED_AMOUNT = "5099"
        const val SEPA_MANDATE_CONTINUE_BUTTON = "SEPA_MANDATE_CONTINUE_BUTTON"

        val CARD_PAYMENT_METHOD = JSONObject(
            """
            {
                "id": "$CARD_PAYMENT_METHOD_ID",
                "object": "payment_method",
                "type": "card",
                "billing_details": {"name": "Jenny Rosen"},
                "card": {
                    "brand": "visa",
                    "exp_month": 12,
                    "exp_year": 2034,
                    "last4": "4242"
                }
            }
            """.trimIndent()
        )

        val SEPA_PAYMENT_METHOD = JSONObject(
            """
            {
                "id": "$SEPA_PAYMENT_METHOD_ID",
                "object": "payment_method",
                "type": "sepa_debit",
                "billing_details": {"name": "Jenny Rosen"},
                "sepa_debit": {
                    "bank_code": "37040044",
                    "branch_code": "012",
                    "country": "DE",
                    "fingerprint": "saved-sepa-fingerprint",
                    "last4": "3000"
                }
            }
            """.trimIndent()
        )
    }
}
