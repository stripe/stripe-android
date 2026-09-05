package com.stripe.android.checkout

import android.app.Application
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.checkouttesting.createPaymentMethod
import com.stripe.android.elements.PaymentElement
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT
import com.stripe.paymentelementtestpages.BillingDetailsPage
import com.stripe.paymentelementtestpages.VerticalModePage
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject
import org.junit.After
import org.junit.Rule
import org.junit.Test

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val contentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)
    private val billingDetailsPage = BillingDetailsPage(testRules.compose)
    private val verticalModePage = VerticalModePage(testRules.compose)

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

    @Test
    fun testBillingTaxUpdateRefreshesCheckoutSession() = runAutomaticTaxTest { context, controller ->
        enqueueTaxUpdate(automaticTaxResponse(UPDATED_TOTAL, TAX_STATUS_COMPLETE))

        contentPage.clickOnLpm("card")
        fillOutCardAndBillingDetails()
        formPage.clickPrimaryButton()

        waitForSessionTotal(controller, UPDATED_TOTAL)
        assertThat(controller.session.value?.tax?.status)
            .isEqualTo(CheckoutController.Session.Tax.Status.Ready)
        contentPage.assertHasSelectedLpm("card")
        context.markTestSucceeded()
    }

    @Test
    fun testSavedPaymentMethodSelectionRefreshesBillingTaxBeforeCommitting() = runAutomaticTaxTest(
        paymentMethodLayout = PaymentElement.Configuration.PaymentMethodLayout.Vertical,
        checkoutInitResponse = automaticTaxResponseWithSavedPaymentMethod(
            INITIAL_TOTAL,
            TAX_STATUS_REQUIRES_LOCATION,
        ),
    ) { context, controller ->
        enqueueTaxUpdate(automaticTaxResponseWithSavedPaymentMethod(UPDATED_TOTAL, TAX_STATUS_COMPLETE))

        contentPage.clickOnSavedPM(SAVED_PAYMENT_METHOD_ID)

        waitForSessionTotal(controller, UPDATED_TOTAL)
        contentPage.assertHasSelectedSavedPaymentMethod(SAVED_PAYMENT_METHOD_ID)
        context.markTestSucceeded()
    }

    @Test
    fun testBillingTaxUpdateFailureCanRetryFromPaymentOptions() = runAutomaticTaxTest { context, controller ->
        enqueueTaxUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
        }

        context.presentPaymentOptions()
        verticalModePage.clickNewPaymentMethodButton("card")
        fillOutCardAndBillingDetails()
        formPage.clickPrimaryButtonWithoutWaitingForDismissal()

        formPage.assertErrorIsShown(applicationContext.getString(R.string.stripe_something_went_wrong))
        formPage.waitUntilVisible()
        formPage.assertPrimaryButtonIsEnabled()
        assertThat(controller.session.value?.totalSummary?.totalDueToday).isEqualTo(INITIAL_TOTAL)

        enqueueTaxUpdate(automaticTaxResponse(UPDATED_TOTAL, TAX_STATUS_COMPLETE))
        formPage.clickPrimaryButton()

        waitForSessionTotal(controller, UPDATED_TOTAL)
        assertThat(controller.session.value?.tax?.status)
            .isEqualTo(CheckoutController.Session.Tax.Status.Ready)
        contentPage.assertHasSelectedLpm("card")
        context.markTestSucceeded()
    }

    @Test
    fun testBillingTaxUpdateFromVerticalPaymentOptionsRefreshesCheckoutSession() {
        runPaymentOptionsTaxUpdateTest(PaymentElement.Configuration.PaymentMethodLayout.Vertical)
    }

    @Test
    fun testBillingTaxUpdateFromHorizontalPaymentOptionsRefreshesCheckoutSession() {
        runPaymentOptionsTaxUpdateTest(PaymentElement.Configuration.PaymentMethodLayout.Horizontal)
    }

    @Test
    fun testCashAppTaxUpdateFromVerticalPaymentOptionsRefreshesCheckoutSession() {
        runCashAppPaymentOptionsTaxUpdateTest(PaymentElement.Configuration.PaymentMethodLayout.Vertical)
    }

    @Test
    fun testCashAppTaxUpdateFromHorizontalPaymentOptionsRefreshesCheckoutSession() {
        runCashAppPaymentOptionsTaxUpdateTest(PaymentElement.Configuration.PaymentMethodLayout.Horizontal)
    }

    @Test
    fun testPrefilledCashAppTaxUpdateFromVerticalPaymentOptionsRefreshesCheckoutSession() {
        runPrefilledCashAppPaymentOptionsTaxUpdateTest(
            PaymentElement.Configuration.PaymentMethodLayout.Vertical
        )
    }

    @Test
    fun testPrefilledCashAppTaxUpdateFromHorizontalPaymentOptionsRefreshesCheckoutSession() {
        runPrefilledCashAppPaymentOptionsTaxUpdateTest(
            PaymentElement.Configuration.PaymentMethodLayout.Horizontal
        )
    }

    private fun runPaymentOptionsTaxUpdateTest(
        paymentMethodLayout: PaymentElement.Configuration.PaymentMethodLayout,
    ) {
        runAutomaticTaxTest(
            paymentMethodLayout = paymentMethodLayout,
            checkoutInitResponse = automaticTaxResponse(INITIAL_TOTAL, TAX_STATUS_REQUIRES_LOCATION),
        ) { context, controller ->
            enqueueTaxUpdate(automaticTaxResponse(INITIAL_TOTAL, TAX_STATUS_COMPLETE))
            contentPage.clickOnLpm("card")
            fillOutCardAndBillingDetails()
            formPage.clickPrimaryButton()
            contentPage.assertHasSelectedLpm("card")

            enqueueTaxUpdate(automaticTaxResponse(UPDATED_TOTAL, TAX_STATUS_COMPLETE))

            context.presentPaymentOptions()
            preparePaymentOptionsScreen(paymentMethodLayout)
            clickPaymentOptionsPrimaryButton()

            waitForSessionTotal(controller, UPDATED_TOTAL)
            assertThat(controller.session.value?.tax?.status)
                .isEqualTo(CheckoutController.Session.Tax.Status.Ready)
            contentPage.assertHasSelectedLpm("card")
            context.markTestSucceeded()
        }
    }

    private fun runCashAppPaymentOptionsTaxUpdateTest(
        paymentMethodLayout: PaymentElement.Configuration.PaymentMethodLayout,
    ) {
        runAutomaticTaxTest(
            paymentMethodLayout = paymentMethodLayout,
            checkoutInitResponse = automaticTaxResponseWithoutRequiredBilling(
                INITIAL_TOTAL,
                TAX_STATUS_REQUIRES_LOCATION,
            ),
        ) { context, controller ->
            context.presentPaymentOptions()
            selectCashApp(paymentMethodLayout)
            formPage.waitUntilVisible()

            enqueueTaxUpdate(automaticTaxResponseWithoutRequiredBilling(UPDATED_TOTAL, TAX_STATUS_COMPLETE))
            fillOutBillingDetails()
            formPage.clickPrimaryButton()

            waitForSessionTotal(controller, UPDATED_TOTAL)
            assertThat(controller.session.value?.tax?.status)
                .isEqualTo(CheckoutController.Session.Tax.Status.Ready)
            contentPage.assertHasSelectedLpm("cashapp")
            context.markTestSucceeded()
        }
    }

    private fun runPrefilledCashAppPaymentOptionsTaxUpdateTest(
        paymentMethodLayout: PaymentElement.Configuration.PaymentMethodLayout,
    ) {
        enqueueTaxUpdate(automaticTaxResponseWithoutRequiredBilling(INITIAL_TOTAL, TAX_STATUS_COMPLETE))
        runAutomaticTaxTest(
            configuration = checkoutConfigurationWithDefaultBillingAddress(paymentMethodLayout),
            checkoutInitResponse = automaticTaxResponseWithoutRequiredBilling(
                INITIAL_TOTAL,
                TAX_STATUS_REQUIRES_LOCATION,
            ),
        ) { context, controller ->
            context.presentPaymentOptions()
            selectCashApp(paymentMethodLayout)
            formPage.waitUntilVisible()
            assertBillingDetailsArePopulated()

            enqueueTaxUpdate(automaticTaxResponseWithoutRequiredBilling(UPDATED_TOTAL, TAX_STATUS_COMPLETE))
            formPage.clickPrimaryButton()

            waitForSessionTotal(controller, UPDATED_TOTAL)
            assertThat(controller.session.value?.tax?.status)
                .isEqualTo(CheckoutController.Session.Tax.Status.Ready)
            contentPage.assertHasSelectedLpm("cashapp")
            context.markTestSucceeded()
        }
    }

    private fun selectCashApp(
        paymentMethodLayout: PaymentElement.Configuration.PaymentMethodLayout,
    ) {
        when (paymentMethodLayout) {
            PaymentElement.Configuration.PaymentMethodLayout.Vertical -> {
                verticalModePage.clickNewPaymentMethodButton("cashapp")
            }
            PaymentElement.Configuration.PaymentMethodLayout.Horizontal -> {
                val cashAppTag = TEST_TAG_LIST + "cashapp"
                testRules.compose.onNodeWithTag(TEST_TAG_LIST, useUnmergedTree = true)
                    .performScrollToNode(hasTestTag(cashAppTag))
                testRules.compose.onNodeWithTag(cashAppTag).performClick()
            }
            PaymentElement.Configuration.PaymentMethodLayout.Automatic -> {
                error("Expected an explicit layout.")
            }
        }
    }

    private fun runAutomaticTaxTest(
        block: (CheckoutPaymentElementTestRunnerContext, CheckoutController) -> Unit,
    ) = runAutomaticTaxTest(
        configuration = checkoutConfiguration(PaymentElement.Configuration.PaymentMethodLayout.Vertical),
        checkoutInitResponse = automaticTaxResponse(INITIAL_TOTAL, TAX_STATUS_REQUIRES_LOCATION),
        block = block,
    )

    private fun runAutomaticTaxTest(
        paymentMethodLayout: PaymentElement.Configuration.PaymentMethodLayout,
        checkoutInitResponse: (MockResponse) -> Unit,
        block: (CheckoutPaymentElementTestRunnerContext, CheckoutController) -> Unit,
    ) = runAutomaticTaxTest(
        configuration = checkoutConfiguration(paymentMethodLayout),
        checkoutInitResponse = checkoutInitResponse,
        block = block,
    )

    private fun runAutomaticTaxTest(
        configuration: CheckoutController.Configuration,
        checkoutInitResponse: (MockResponse) -> Unit,
        block: (CheckoutPaymentElementTestRunnerContext, CheckoutController) -> Unit,
    ) {
        lateinit var controller: CheckoutController
        runCheckoutPaymentElementTest(
            networkRule = networkRule,
            checkoutInitResponse = checkoutInitResponse,
            setup = {
                controller = it
                it.configure(
                    clientSecret = DEFAULT_CLIENT_SECRET,
                    configuration = configuration,
                ).getOrThrow()
            },
        ) { context ->
            block(context, controller)
        }
    }

    private fun checkoutConfiguration(
        paymentMethodLayout: PaymentElement.Configuration.PaymentMethodLayout,
    ): CheckoutController.Configuration {
        return CheckoutController.Configuration().paymentElement(
            PaymentElement.Configuration().paymentMethodLayout(paymentMethodLayout)
        )
    }

    private fun checkoutConfigurationWithDefaultBillingAddress(
        paymentMethodLayout: PaymentElement.Configuration.PaymentMethodLayout,
    ): CheckoutController.Configuration {
        return checkoutConfiguration(paymentMethodLayout).defaults(
            CheckoutController.Configuration.Defaults().billingDetails(
                CheckoutController.Configuration.Defaults.ContactDetails().address(
                    CheckoutController.Address()
                        .city(BILLING_ADDRESS_CITY)
                        .country("US")
                        .line1(BILLING_ADDRESS_LINE_ONE)
                        .postalCode(BILLING_ADDRESS_ZIP)
                        .state(BILLING_ADDRESS_STATE)
                )
            )
        )
    }

    private fun preparePaymentOptionsScreen(
        paymentMethodLayout: PaymentElement.Configuration.PaymentMethodLayout,
    ) {
        if (paymentMethodLayout == PaymentElement.Configuration.PaymentMethodLayout.Vertical) {
            formPage.waitUntilVisible()
            Espresso.pressBack()
            formPage.waitUntilMissing()
        }
        waitForPaymentOptionsLayout(paymentMethodLayout)
    }

    private fun waitForPaymentOptionsLayout(
        paymentMethodLayout: PaymentElement.Configuration.PaymentMethodLayout,
    ) {
        val layoutTag = when (paymentMethodLayout) {
            PaymentElement.Configuration.PaymentMethodLayout.Vertical -> TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT
            PaymentElement.Configuration.PaymentMethodLayout.Horizontal -> TEST_TAG_LIST
            PaymentElement.Configuration.PaymentMethodLayout.Automatic -> error("Expected an explicit layout.")
        }
        testRules.compose.waitUntil(timeoutMillis = 5_000) {
            testRules.compose.onAllNodes(hasTestTag(layoutTag))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
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

    private fun enqueueTaxUpdate(responseFactory: (MockResponse) -> Unit) {
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", "US"),
            bodyPart("tax_region[line1]", BILLING_ADDRESS_LINE_ONE),
            bodyPart("tax_region[city]", BILLING_ADDRESS_CITY),
            bodyPart("tax_region[state]", BILLING_ADDRESS_STATE),
            bodyPart("tax_region[postal_code]", BILLING_ADDRESS_ZIP),
            bodyPart("elements_session_client[is_aggregation_expected]", "true"),
            responseFactory = responseFactory,
        )
    }

    private fun fillOutCardAndBillingDetails() {
        formPage.fillOutCardDetails()
        fillOutBillingDetails()
    }

    private fun fillOutBillingDetails() {
        billingDetailsPage.country.assertTextContains("United States")
        billingDetailsPage.line1.performTextReplacement(BILLING_ADDRESS_LINE_ONE)
        billingDetailsPage.city.performTextReplacement(BILLING_ADDRESS_CITY)
        billingDetailsPage.state.performScrollTo().performClick()
        testRules.compose.onNodeWithText("California").performClick()
        billingDetailsPage.zipCode.performTextReplacement(BILLING_ADDRESS_ZIP)
    }

    private fun assertBillingDetailsArePopulated() {
        billingDetailsPage.country.performScrollTo().assertTextContains("United States")
        billingDetailsPage.line1.performScrollTo().assertTextContains(BILLING_ADDRESS_LINE_ONE)
        billingDetailsPage.city.performScrollTo().assertTextContains(BILLING_ADDRESS_CITY)
        billingDetailsPage.state.performScrollTo().assertTextContains("California")
        billingDetailsPage.zipCode.performScrollTo().assertTextContains(BILLING_ADDRESS_ZIP)
    }

    private fun waitForSessionTotal(controller: CheckoutController, total: Long) {
        testRules.compose.waitUntil(timeoutMillis = 5_000) {
            controller.session.value?.totalSummary?.totalDueToday == total
        }
    }

    private fun automaticTaxResponse(
        total: Long,
        taxStatus: String,
    ): (MockResponse) -> Unit = automaticTaxResponse(
        total = total,
        taxStatus = taxStatus,
        billingAddressCollection = "required",
    )

    private fun automaticTaxResponseWithoutRequiredBilling(
        total: Long,
        taxStatus: String,
    ): (MockResponse) -> Unit = automaticTaxResponse(
        total = total,
        taxStatus = taxStatus,
        billingAddressCollection = "auto",
    )

    private fun automaticTaxResponseWithSavedPaymentMethod(
        total: Long,
        taxStatus: String,
    ): (MockResponse) -> Unit = automaticTaxResponse(
        total = total,
        taxStatus = taxStatus,
        billingAddressCollection = "auto",
        jsonModifier = { json ->
            json.put("account_settings", JSONObject("""{"country":"US"}"""))
            json.put(
                "customer",
                JSONObject(
                    """
                    {
                        "id": "cus_123",
                        "payment_methods": [{
                            "id": "$SAVED_PAYMENT_METHOD_ID",
                            "object": "payment_method",
                            "type": "card",
                            "billing_details": {
                                "address": {
                                    "line1": "$BILLING_ADDRESS_LINE_ONE",
                                    "city": "$BILLING_ADDRESS_CITY",
                                    "state": "$BILLING_ADDRESS_STATE",
                                    "country": "US",
                                    "postal_code": "$BILLING_ADDRESS_ZIP"
                                }
                            },
                            "card": {
                                "brand": "visa",
                                "exp_month": 12,
                                "exp_year": 2034,
                                "last4": "4242"
                            }
                        }],
                        "can_detach_payment_method": true
                    }
                    """.trimIndent()
                )
            )
        },
    )

    private fun automaticTaxResponse(
        total: Long,
        taxStatus: String,
        billingAddressCollection: String,
    ): (MockResponse) -> Unit = automaticTaxResponse(
        total = total,
        taxStatus = taxStatus,
        billingAddressCollection = billingAddressCollection,
        jsonModifier = {},
    )

    private fun automaticTaxResponse(
        total: Long,
        taxStatus: String,
        billingAddressCollection: String,
        jsonModifier: (JSONObject) -> Unit,
    ): (MockResponse) -> Unit = { response ->
        response.testBodyFromFile("checkout-session-init.json") { json ->
            json.put("customer_email", "checkout@example.com")
            json.put("billing_address_collection", billingAddressCollection)
            json.put(
                "tax_context",
                JSONObject()
                    .put("automatic_tax_enabled", true)
                    .put("automatic_tax_address_source", "session.billing"),
            )
            json.put(
                "tax_meta",
                JSONObject()
                    .put("computation_type", "automatic")
                    .put("status", taxStatus),
            )
            json.put(
                "total_summary",
                JSONObject()
                    .put("subtotal", INITIAL_TOTAL)
                    .put("due", total)
                    .put("total", total),
            )
            json.getJSONObject("elements_session").remove("link_settings")
            json.getJSONObject("server_built_elements_session_params")
                .getJSONObject("deferred_intent")
                .put("amount", total)
            jsonModifier(json)
        }
    }

    private companion object {
        const val DEFAULT_CLIENT_SECRET = "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example"
        const val INITIAL_TOTAL = 5_099L
        const val UPDATED_TOTAL = 5_399L
        const val SAVED_PAYMENT_METHOD_ID = "pm_12345"
        const val BILLING_ADDRESS_LINE_ONE = "510 Townsend St"
        const val BILLING_ADDRESS_CITY = "San Francisco"
        const val BILLING_ADDRESS_STATE = "CA"
        const val BILLING_ADDRESS_ZIP = "94103"
        const val TAX_STATUS_REQUIRES_LOCATION = "requires_location_inputs"
        const val TAX_STATUS_COMPLETE = "complete"
    }
}
