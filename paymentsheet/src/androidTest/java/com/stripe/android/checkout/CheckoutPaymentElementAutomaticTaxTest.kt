package com.stripe.android.checkout

import android.app.Application
import app.cash.turbine.Turbine
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
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.elements.PaymentElement
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.paymentelementtestpages.BillingDetailsPage
import com.stripe.paymentelementtestpages.VerticalModePage
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutPaymentElementAutomaticTaxTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule(validationTimeout = 5.seconds)

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule) {
        around(FeatureFlagTestRule(FeatureFlags.nativeLinkEnabled, isEnabled = true))
    }

    private val contentPage = EmbeddedContentPage(testRules.compose)
    private val formPage = EmbeddedFormPage(testRules.compose)
    private val billingDetailsPage = BillingDetailsPage(testRules.compose)
    private val verticalModePage = VerticalModePage(testRules.compose)

    @After
    fun teardown() {
        GooglePayRepository.resetFactory()
    }

    @Test
    fun testBillingTaxUpdateRefreshesCheckoutSession() = runAutomaticTaxTest {
        enqueueTaxUpdate(automaticTaxResponse(UPDATED_TOTAL, TAX_STATUS_COMPLETE))

        contentPage.clickOnLpm("card")
        fillOutCardAndBillingDetails()
        formPage.clickPrimaryButton()

        waitForSessionTotal(controller, UPDATED_TOTAL)
        assertThat(controller.session.value?.tax?.status)
            .isEqualTo(CheckoutController.Session.Tax.Status.Ready)
        contentPage.assertHasSelectedLpm("card")
        markTestSucceeded()
    }

    @Test
    fun testSavedPaymentMethodSelectionRefreshesBillingTaxAndInvokesImmediateAction() {
        runSavedPaymentMethodSelectionFromCashAppScenario {
            val taxUpdateRequests = Turbine<Unit>()
            val releaseTaxUpdateResponse = CountDownLatch(1)
            enqueueSavedPaymentMethodTaxUpdate { response ->
                taxUpdateRequests.add(Unit)
                check(releaseTaxUpdateResponse.await(10, TimeUnit.SECONDS)) {
                    "Timed out waiting to release the Checkout Session update response."
                }
                automaticTaxResponse(
                    total = UPDATED_TOTAL,
                    taxStatus = TAX_STATUS_COMPLETE,
                    billingAddressCollection = "auto",
                    hasSavedPaymentMethod = true,
                )(response)
            }

            try {
                contentPage.clickOnSavedPM(SAVED_PAYMENT_METHOD_ID)

                taxUpdateRequests.awaitItem()
                contentPage.assertPaymentMethodRowsAreEnabled(false)
                contentPage.assertHasSelectedLpm("cashapp")
                assertThat(controller.session.value?.totals?.total?.minorUnitsAmount)
                    .isEqualTo(INITIAL_TOTAL.toDouble())
                immediateActionCalls.expectNoEvents()
            } finally {
                releaseTaxUpdateResponse.countDown()
            }

            immediateActionCalls.awaitItem()
            assertSavedPaymentMethodSession(checkNotNull(controller.session.value))
            contentPage.assertHasSelectedSavedPaymentMethod(SAVED_PAYMENT_METHOD_ID)
            contentPage.assertPaymentMethodRowsAreEnabled(true)
            taxUpdateRequests.ensureAllEventsConsumed()
        }
    }

    @Test
    fun testSavedPaymentMethodSelectionFailureDoesNotInvokeImmediateActionAndCanRetry() {
        runSavedPaymentMethodSelectionFromCashAppScenario {
            val taxUpdateRequests = Turbine<Unit>()
            val releaseTaxUpdateResponse = CountDownLatch(1)
            enqueueSavedPaymentMethodTaxUpdate { response ->
                taxUpdateRequests.add(Unit)
                check(releaseTaxUpdateResponse.await(10, TimeUnit.SECONDS)) {
                    "Timed out waiting to release the Checkout Session update response."
                }
                response.setResponseCode(400)
                response.setBody("""{"error":{"message":"Invalid tax region"}}""")
            }

            try {
                contentPage.clickOnSavedPM(SAVED_PAYMENT_METHOD_ID)
                taxUpdateRequests.awaitItem()
                contentPage.assertPaymentMethodRowsAreEnabled(false)
            } finally {
                releaseTaxUpdateResponse.countDown()
            }

            contentPage.assertPaymentMethodRowsAreEnabled(true)
            contentPage.assertHasSelectedLpm("cashapp")
            assertThat(controller.session.value?.totals?.total?.minorUnitsAmount)
                .isEqualTo(INITIAL_TOTAL.toDouble())
            immediateActionCalls.expectNoEvents()

            enqueueSavedPaymentMethodTaxUpdate(
                automaticTaxResponse(
                    total = UPDATED_TOTAL,
                    taxStatus = TAX_STATUS_COMPLETE,
                    billingAddressCollection = "auto",
                    hasSavedPaymentMethod = true,
                )
            )
            contentPage.clickOnSavedPM(SAVED_PAYMENT_METHOD_ID)

            immediateActionCalls.awaitItem()
            assertSavedPaymentMethodSession(checkNotNull(controller.session.value))
            contentPage.assertHasSelectedSavedPaymentMethod(SAVED_PAYMENT_METHOD_ID)
            contentPage.assertPaymentMethodRowsAreEnabled(true)
            taxUpdateRequests.ensureAllEventsConsumed()
        }
    }

    private fun runSavedPaymentMethodSelectionFromCashAppScenario(
        block: suspend Scenario.() -> Unit,
    ) {
        lateinit var scenario: Scenario
        runAutomaticTaxTest(
            paymentMethodLayout = PaymentElement.Configuration.PaymentMethodLayout.Vertical,
            checkoutInitResponse = automaticTaxResponse(
                total = INITIAL_TOTAL,
                taxStatus = TAX_STATUS_REQUIRES_LOCATION,
                billingAddressCollection = "auto",
                hasSavedPaymentMethod = true,
            ),
            rowSelectionBehavior = PaymentElement.RowSelectionBehavior.immediateAction {
                scenario.immediateActionCalls.add(Unit)
            },
        ) {
            scenario = this
            selectCashAppAndAwaitCallback()
            block()
            markTestSucceeded()
        }
    }

    private suspend fun Scenario.selectCashAppAndAwaitCallback() {
        contentPage.clickOnLpm("cashapp")
        formPage.waitUntilVisible()

        enqueueTaxUpdate(
            automaticTaxResponse(
                total = INITIAL_TOTAL,
                taxStatus = TAX_STATUS_COMPLETE,
                billingAddressCollection = "auto",
                hasSavedPaymentMethod = true,
            )
        )
        fillOutBillingDetails()
        formPage.clickPrimaryButton()

        immediateActionCalls.awaitItem()
        val session = checkNotNull(controller.session.value)
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(INITIAL_TOTAL.toDouble())
        assertThat(session.paymentOption?.paymentMethodType).isEqualTo("cashapp")
        assertThat(session.paymentOption?.billingDetails?.address?.line1)
            .isEqualTo(BILLING_ADDRESS_LINE_ONE)
        contentPage.assertHasSelectedLpm("cashapp")
    }

    private fun assertSavedPaymentMethodSession(session: CheckoutController.Session) {
        assertThat(session.totals.total.minorUnitsAmount).isEqualTo(UPDATED_TOTAL.toDouble())
        assertThat(session.paymentOption?.paymentMethodType).isEqualTo("card")
        assertThat(session.paymentOption?.billingDetails?.address?.line1)
            .isEqualTo(SAVED_BILLING_ADDRESS_LINE_ONE)
    }

    @Test
    fun testBillingTaxUpdateFailureCanRetryFromPaymentOptions() = runAutomaticTaxTest {
        enqueueTaxUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
        }

        presentPaymentOptions()
        verticalModePage.clickNewPaymentMethodButton("card")
        fillOutCardAndBillingDetails()
        formPage.clickPrimaryButtonWithoutWaitingForDismissal()

        formPage.assertErrorIsShown(applicationContext.getString(R.string.stripe_something_went_wrong))
        formPage.waitUntilVisible()
        formPage.assertPrimaryButtonIsEnabled()
        assertThat(controller.session.value?.totals?.total?.minorUnitsAmount).isEqualTo(INITIAL_TOTAL.toDouble())

        enqueueTaxUpdate(automaticTaxResponse(UPDATED_TOTAL, TAX_STATUS_COMPLETE))
        formPage.clickPrimaryButton()

        waitForSessionTotal(controller, UPDATED_TOTAL)
        assertThat(controller.session.value?.tax?.status)
            .isEqualTo(CheckoutController.Session.Tax.Status.Ready)
        contentPage.assertHasSelectedLpm("card")
        markTestSucceeded()
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
        ) {
            enqueueTaxUpdate(automaticTaxResponse(INITIAL_TOTAL, TAX_STATUS_COMPLETE))
            contentPage.clickOnLpm("card")
            fillOutCardAndBillingDetails()
            formPage.clickPrimaryButton()
            contentPage.assertHasSelectedLpm("card")

            enqueueTaxUpdate(automaticTaxResponse(UPDATED_TOTAL, TAX_STATUS_COMPLETE))

            presentPaymentOptions()
            preparePaymentOptionsScreen(paymentMethodLayout)
            clickPaymentOptionsPrimaryButton()

            waitForSessionTotal(controller, UPDATED_TOTAL)
            assertThat(controller.session.value?.tax?.status)
                .isEqualTo(CheckoutController.Session.Tax.Status.Ready)
            contentPage.assertHasSelectedLpm("card")
            markTestSucceeded()
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
        ) {
            presentPaymentOptions()
            selectCashApp(paymentMethodLayout)
            formPage.waitUntilVisible()

            enqueueTaxUpdate(automaticTaxResponseWithoutRequiredBilling(UPDATED_TOTAL, TAX_STATUS_COMPLETE))
            fillOutBillingDetails()
            formPage.clickPrimaryButton()

            waitForSessionTotal(controller, UPDATED_TOTAL)
            assertThat(controller.session.value?.tax?.status)
                .isEqualTo(CheckoutController.Session.Tax.Status.Ready)
            contentPage.assertHasSelectedLpm("cashapp")
            markTestSucceeded()
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
        ) {
            presentPaymentOptions()
            selectCashApp(paymentMethodLayout)
            formPage.waitUntilVisible()
            assertBillingDetailsArePopulated()

            enqueueTaxUpdate(automaticTaxResponseWithoutRequiredBilling(UPDATED_TOTAL, TAX_STATUS_COMPLETE))
            formPage.clickPrimaryButton()

            waitForSessionTotal(controller, UPDATED_TOTAL)
            assertThat(controller.session.value?.tax?.status)
                .isEqualTo(CheckoutController.Session.Tax.Status.Ready)
            contentPage.assertHasSelectedLpm("cashapp")
            markTestSucceeded()
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
        block: suspend Scenario.() -> Unit,
    ) = runAutomaticTaxTest(
        configuration = checkoutConfiguration(PaymentElement.Configuration.PaymentMethodLayout.Vertical),
        checkoutInitResponse = automaticTaxResponse(INITIAL_TOTAL, TAX_STATUS_REQUIRES_LOCATION),
        block = block,
    )

    private fun runAutomaticTaxTest(
        paymentMethodLayout: PaymentElement.Configuration.PaymentMethodLayout,
        checkoutInitResponse: (MockResponse) -> Unit,
        rowSelectionBehavior: PaymentElement.RowSelectionBehavior = PaymentElement.RowSelectionBehavior.default(),
        block: suspend Scenario.() -> Unit,
    ) = runAutomaticTaxTest(
        configuration = checkoutConfiguration(paymentMethodLayout),
        checkoutInitResponse = checkoutInitResponse,
        rowSelectionBehavior = rowSelectionBehavior,
        block = block,
    )

    private fun runAutomaticTaxTest(
        configuration: CheckoutController.Configuration,
        checkoutInitResponse: (MockResponse) -> Unit,
        rowSelectionBehavior: PaymentElement.RowSelectionBehavior = PaymentElement.RowSelectionBehavior.default(),
        block: suspend Scenario.() -> Unit,
    ) {
        lateinit var controller: CheckoutController
        runCheckoutPaymentElementTest(
            networkRule = networkRule,
            checkoutInitResponse = checkoutInitResponse,
            rowSelectionBehavior = rowSelectionBehavior,
            setup = { configuredController ->
                controller = configuredController
                configuredController.configure(
                    clientSecret = DEFAULT_CLIENT_SECRET,
                    configuration = configuration,
                ).getOrThrow()
            },
        ) { runnerContext ->
            runBlocking {
                val scenario = Scenario(
                    runnerContext = runnerContext,
                    controller = controller,
                )
                scenario.block()
                scenario.immediateActionCalls.ensureAllEventsConsumed()
            }
        }
    }

    private class Scenario(
        private val runnerContext: CheckoutPaymentElementTestRunnerContext,
        val controller: CheckoutController,
    ) {
        val immediateActionCalls = Turbine<Unit>()

        fun presentPaymentOptions() {
            runnerContext.presentPaymentOptions()
        }

        fun confirm() {
            runnerContext.confirm()
        }

        fun markTestSucceeded() {
            runnerContext.markTestSucceeded()
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

    private fun enqueueSavedPaymentMethodTaxUpdate(responseFactory: (MockResponse) -> Unit) {
        networkRule.checkoutUpdate(
            bodyPart("tax_region[country]", "US"),
            bodyPart("tax_region[line1]", SAVED_BILLING_ADDRESS_LINE_ONE),
            bodyPart("tax_region[city]", SAVED_BILLING_ADDRESS_CITY),
            bodyPart("tax_region[state]", SAVED_BILLING_ADDRESS_STATE),
            bodyPart("tax_region[postal_code]", SAVED_BILLING_ADDRESS_ZIP),
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
            controller.session.value?.totals?.total?.minorUnitsAmount == total.toDouble()
        }
    }

    private fun automaticTaxResponseWithoutRequiredBilling(
        total: Long,
        taxStatus: String,
    ): (MockResponse) -> Unit = automaticTaxResponse(
        total = total,
        taxStatus = taxStatus,
        billingAddressCollection = "auto",
    )

    private fun automaticTaxResponse(
        total: Long,
        taxStatus: String,
        billingAddressCollection: String = "required",
        hasSavedPaymentMethod: Boolean = false,
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
            json.getJSONArray("checkout_items").getJSONObject(0)
                .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                .put("subtotal", INITIAL_TOTAL)
                .put("total", total)
                .put("tax_exclusive", total - INITIAL_TOTAL)
            json.getJSONObject("elements_session").remove("link_settings")
            json.getJSONObject("server_built_elements_session_params")
                .getJSONObject("deferred_intent")
                .put("amount", total)
            if (hasSavedPaymentMethod) {
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
                                        "line1": "$SAVED_BILLING_ADDRESS_LINE_ONE",
                                        "city": "$SAVED_BILLING_ADDRESS_CITY",
                                        "state": "$SAVED_BILLING_ADDRESS_STATE",
                                        "country": "US",
                                        "postal_code": "$SAVED_BILLING_ADDRESS_ZIP"
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
            }
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
        const val SAVED_BILLING_ADDRESS_LINE_ONE = "123 Main St"
        const val SAVED_BILLING_ADDRESS_CITY = "Denver"
        const val SAVED_BILLING_ADDRESS_STATE = "CO"
        const val SAVED_BILLING_ADDRESS_ZIP = "80202"
        const val TAX_STATUS_REQUIRES_LOCATION = "requires_location_inputs"
        const val TAX_STATUS_COMPLETE = "complete"
    }
}
