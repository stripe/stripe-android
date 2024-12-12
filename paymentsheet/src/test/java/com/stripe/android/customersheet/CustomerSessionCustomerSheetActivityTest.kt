package com.stripe.android.customersheet

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.ResponseReplacement
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.EditPage
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.RemoveDialog
import com.stripe.android.paymentsheet.SavedPaymentMethodsPage
import com.stripe.android.paymentsheet.SavedPaymentMethodsPage.Companion.assertHasModifyBadge
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.PaymentMethodFactory.update
import okhttp3.mockwebserver.MockResponse
import org.json.JSONArray
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCustomerSessionApi::class)
@RunWith(AndroidJUnit4::class)
class CustomerSessionCustomerSheetActivityTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    private val composeTestRule = createAndroidComposeRule<CustomerSheetActivity>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(application))

    private val savedPaymentMethodsPage = SavedPaymentMethodsPage(composeTestRule)
    private val editPage = EditPage(composeTestRule)
    private val removeDialog = RemoveDialog(composeTestRule)

    @After
    fun teardown() {
        CustomerSheetHacks.clear()
    }

    @Test
    fun `When multiple PMs with remove permissions and can remove last PM, should all be enabled when editing`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242"),
                PaymentMethodFactory.card(last4 = "5544", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = true,
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()

            savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "4242").assertIsEnabled()
            savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "5544").assertIsEnabled()
        }

    @Test
    fun `When single PM with remove permissions and can remove last PM, should be enabled when editing`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242"),
            ),
            isPaymentMethodRemoveEnabled = true,
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()

            savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "4242").assertIsEnabled()
        }

    @Test
    fun `When single PM with remove permissions but cannot remove last PM, edit button should not be displayed`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242"),
            ),
            isPaymentMethodRemoveEnabled = true,
            allowsRemovalOfLastSavedPaymentMethod = false,
        ) {
            savedPaymentMethodsPage.onEditButton().assertDoesNotExist()
        }

    @Test
    fun `When multiple PMs but no remove permissions, edit button should not be displayed`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242"),
                PaymentMethodFactory.card(last4 = "5544"),
            ),
            isPaymentMethodRemoveEnabled = false,
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodsPage.onEditButton().assertDoesNotExist()
        }

    @Test
    fun `When multiple PMs with CBC card but no remove permissions, should allow editing CBC card and disable rest`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
                PaymentMethodFactory.card(last4 = "5544"),
            ),
            isPaymentMethodRemoveEnabled = false,
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()

            savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "5544").assertIsNotEnabled()

            val cbcCard = savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()
        }

    @Test
    fun `When multiple PMs with CBC card and has remove permissions, should be able to remove and edit CBC card`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
                PaymentMethodFactory.card(last4 = "5544"),
            ),
            isPaymentMethodRemoveEnabled = true,
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()

            savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "5544").assertIsEnabled()

            val cbcCard = savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            savedPaymentMethodsPage.onModifyBadgeFor(last4 = "4242").performClick()

            editPage.onRemoveButton().assertIsEnabled()
        }

    @Test
    fun `When single CBC card, has remove permissions, and can remove last PM, should be able to remove and edit`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = true,
            isCanRemoveLastPaymentMethodEnabled = true,
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()

            val cbcCard = savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            savedPaymentMethodsPage.onModifyBadgeFor(last4 = "4242").performClick()

            editPage.onRemoveButton().assertIsEnabled()
        }

    @Test
    fun `When single CBC card but no remove permissions, can edit but not remove CBC card`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = false,
            isCanRemoveLastPaymentMethodEnabled = true,
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()

            val cbcCard = savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            savedPaymentMethodsPage.onModifyBadgeFor(last4 = "4242").performClick()

            editPage.onRemoveButton().assertDoesNotExist()
        }

    @Test
    fun `When single CBC card, can remove, but cannot remove last from config, can edit but not remove CBC card`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = true,
            isCanRemoveLastPaymentMethodEnabled = true,
            allowsRemovalOfLastSavedPaymentMethod = false,
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()

            val cbcCard = savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            savedPaymentMethodsPage.onModifyBadgeFor(last4 = "4242").performClick()

            editPage.onRemoveButton().assertDoesNotExist()
        }

    @Test
    fun `When single CBC card, can remove, but cannot remove last from server, can edit but not remove CBC card`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = true,
            isCanRemoveLastPaymentMethodEnabled = false,
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()

            val cbcCard = savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            savedPaymentMethodsPage.onModifyBadgeFor(last4 = "4242").performClick()

            editPage.onRemoveButton().assertDoesNotExist()
        }

    @Test
    fun `On detach, should remove payment method & un-shown duplicates from saved PMs screen`() = runTest(
        cards = listOf(
            createDuplicateCard(id = "pm_1"),
            PaymentMethodFactory.card(last4 = "1001"),
        ),
        isPaymentMethodRemoveEnabled = true,
        allowsRemovalOfLastSavedPaymentMethod = true,
    ) {
        savedPaymentMethodsPage.onEditButton().performClick()
        savedPaymentMethodsPage.onRemoveBadgeFor(last4 = "4242").performClick()

        enqueuePaymentMethods(
            cards = listOf(
                createDuplicateCard(id = "pm_1"),
                createDuplicateCard(id = "pm_2"),
                createDuplicateCard(id = "pm_3"),
                createDuplicateCard(id = "pm_4"),
            )
        )

        enqueueDetachPaymentMethod(id = "pm_1")
        enqueueDetachPaymentMethod(id = "pm_2")
        enqueueDetachPaymentMethod(id = "pm_3")
        enqueueDetachPaymentMethod(id = "pm_4")

        removeDialog.confirm()
        savedPaymentMethodsPage.waitUntilVisible()
        savedPaymentMethodsPage.waitForSavedPaymentMethodToBeRemoved(last4 = "4242")
    }

    @Test
    fun `On detach from edit screen, should remove payment method & un-shown duplicates from saved PMs screen`() =
        runTest(
            cards = listOf(
                createDuplicateCard(id = "pm_1", addCbcNetworks = true),
                PaymentMethodFactory.card(last4 = "1001"),
            ),
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()
            savedPaymentMethodsPage.onModifyBadgeFor(last4 = "4242").performClick()

            enqueuePaymentMethods(
                cards = listOf(
                    createDuplicateCard(id = "pm_1", addCbcNetworks = true),
                    createDuplicateCard(id = "pm_2", addCbcNetworks = true),
                    createDuplicateCard(id = "pm_3", addCbcNetworks = true),
                    createDuplicateCard(id = "pm_4", addCbcNetworks = true),
                )
            )

            enqueueDetachPaymentMethod(id = "pm_1")
            enqueueDetachPaymentMethod(id = "pm_2")
            enqueueDetachPaymentMethod(id = "pm_3")
            enqueueDetachPaymentMethod(id = "pm_4")

            editPage.onRemoveButton().performClick()
            removeDialog.confirm()

            savedPaymentMethodsPage.waitUntilVisible()
            savedPaymentMethodsPage.waitForSavedPaymentMethodToBeRemoved(last4 = "4242")
        }

    @Test
    fun `On update, should update payment method in saved payment methods screen`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(id = "pm_1")
                    .update(last4 = "1001", addCbcNetworks = true),
            ),
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()
            savedPaymentMethodsPage.onModifyBadgeFor(last4 = "1001").performClick()

            editPage.setCardBrand("Visa")
            editPage.update()

            enqueueUpdatePaymentMethod(id = "pm_1")

            savedPaymentMethodsPage.waitUntilVisible()
            savedPaymentMethodsPage.onSavedPaymentMethod(last4 = "1001").assertExists()
        }

    @OptIn(ExperimentalCustomerSessionApi::class)
    private fun runTest(
        cards: List<PaymentMethod> = listOf(),
        isPaymentMethodRemoveEnabled: Boolean = true,
        isCanRemoveLastPaymentMethodEnabled: Boolean = true,
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
        test: (CustomerSheetActivity) -> Unit,
    ) {
        CustomerSheetHacks.initialize(
            application = application,
            lifecycleOwner = TestLifecycleOwner(),
            integration = CustomerSheetIntegration.CustomerSession(
                customerSessionProvider = object : CustomerSheet.CustomerSessionProvider() {
                    override suspend fun providesCustomerSessionClientSecret(): Result<
                        CustomerSheet.CustomerSessionClientSecret
                        > {
                        return Result.success(
                            CustomerSheet.CustomerSessionClientSecret(
                                customerId = "cus_1",
                                clientSecret = "cuss_123"
                            )
                        )
                    }

                    override suspend fun intentConfiguration(): Result<CustomerSheet.IntentConfiguration> {
                        return Result.success(
                            CustomerSheet.IntentConfiguration(
                                paymentMethodTypes = listOf("card", "us_bank_account"),
                            )
                        )
                    }

                    override suspend fun provideSetupIntentClientSecret(customerId: String): Result<String> {
                        return Result.success("seti_123_secret_123")
                    }
                }
            )
        )

        val countDownLatch = CountDownLatch(1)

        enqueueElementsSession(
            savedCards = cards,
            isPaymentMethodRemoveEnabled = isPaymentMethodRemoveEnabled,
            isCanRemoveLastPaymentMethodEnabled = isCanRemoveLastPaymentMethodEnabled,
        )

        ActivityScenario.launch<CustomerSheetActivity>(
            CustomerSheetContract().createIntent(
                ApplicationProvider.getApplicationContext(),
                CustomerSheetContract.Args(
                    integrationType = CustomerSheetIntegration.Type.CustomerSession,
                    configuration = CustomerSheet.Configuration(
                        merchantDisplayName = "Merchant, Inc.",
                        allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
                        preferredNetworks = listOf(CardBrand.CartesBancaires, CardBrand.Visa),
                    ),
                    statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
                )
            )
        ).use { scenario ->
            scenario.onActivity { activity ->
                composeTestRule.waitUntil(timeoutMillis = 5_000) {
                    composeTestRule
                        .onAllNodes(
                            hasTestTag(FORM_ELEMENT_TEST_TAG)
                                .or(hasTestTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG))
                        )
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
                }

                test(activity)

                countDownLatch.countDown()
            }

            countDownLatch.await(10, TimeUnit.SECONDS)
            networkRule.validate()
        }
    }

    private fun enqueueElementsSession(
        savedCards: List<PaymentMethod> = listOf(),
        isPaymentMethodRemoveEnabled: Boolean,
        isCanRemoveLastPaymentMethodEnabled: Boolean,
    ) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
            query("type", "deferred_intent"),
            query(urlEncode("deferred_intent[setup_future_usage]"), "off_session"),
            query(urlEncode("deferred_intent[mode]"), "setup"),
            query(urlEncode("deferred_intent[payment_method_types][0]"), "card"),
            query(urlEncode("deferred_intent[payment_method_types][1]"), "us_bank_account"),
            query("customer_session_client_secret", "cuss_123"),
        ) { response ->
            response.createElementsSessionResponse(
                cards = savedCards,
                isPaymentMethodRemoveEnabled = isPaymentMethodRemoveEnabled,
                isCanRemoveLastPaymentMethodEnabled = isCanRemoveLastPaymentMethodEnabled,
            )
        }
    }

    private fun enqueuePaymentMethods(
        cards: List<PaymentMethod> = listOf(),
    ) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", "card"),
            query("customer", "cus_12345"),
            query("limit", "100")
        ) { response ->
            response.createPaymentMethodsResponse(cards)
        }
    }

    private fun enqueueDetachPaymentMethod(id: String) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods/$id/detach")
        ) { response ->
            response.createPaymentMethodDetachResponse(id = id)
        }
    }

    private fun enqueueUpdatePaymentMethod(id: String) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods/$id")
        ) { response ->
            response.createPaymentMethodUpdateResponse(id)
        }
    }

    private fun createDuplicateCard(id: String, addCbcNetworks: Boolean = false): PaymentMethod {
        return PaymentMethodFactory.card(id = id)
            .update(last4 = "4242", addCbcNetworks = addCbcNetworks)
            .run {
                copy(
                    card = card?.copy(
                        fingerprint = "fingerprint1"
                    )
                )
            }
    }

    private fun MockResponse.createPaymentMethodsResponse(
        cards: List<PaymentMethod>,
    ): MockResponse {
        val cardsArray = JSONArray()

        cards.forEach { card ->
            cardsArray.put(PaymentMethodFactory.convertCardToJson(card))
        }

        return testBodyFromFile(
            filename = "payment-methods-get-success.json",
            replacements = listOf(
                ResponseReplacement(
                    original = "[PAYMENT_METHODS_HERE]",
                    new = cardsArray.toString(2)
                )
            )
        )
    }

    private fun MockResponse.createPaymentMethodDetachResponse(
        id: String,
    ): MockResponse {
        return testBodyFromFile(
            filename = "payment-method-detach.json",
            replacements = listOf(
                ResponseReplacement(
                    original = "PAYMENT_METHOD_ID_HERE",
                    new = id
                )
            )
        )
    }

    private fun MockResponse.createPaymentMethodUpdateResponse(id: String): MockResponse {
        return testBodyFromFile(
            filename = "payment-method-update.json",
            replacements = listOf(
                ResponseReplacement(
                    original = "PAYMENT_METHOD_ID_HERE",
                    new = id
                )
            )
        )
    }

    private fun MockResponse.createElementsSessionResponse(
        cards: List<PaymentMethod>,
        isPaymentMethodRemoveEnabled: Boolean,
        isCanRemoveLastPaymentMethodEnabled: Boolean,
    ): MockResponse {
        val removeFeature = if (isPaymentMethodRemoveEnabled) {
            "enabled"
        } else {
            "disabled"
        }

        val removeLastFeature = if (isCanRemoveLastPaymentMethodEnabled) {
            "enabled"
        } else {
            "disabled"
        }

        val cardsArray = JSONArray()

        cards.forEach { card ->
            cardsArray.put(PaymentMethodFactory.convertCardToJson(card))
        }

        val cardsStringified = cardsArray.toString(2)

        return testBodyFromFile(
            filename = "elements-sessions-customer_sheet_customer_session.json",
            replacements = listOf(
                ResponseReplacement(
                    original = "PAYMENT_METHOD_REMOVE_FEATURE",
                    new = removeFeature,
                ),
                ResponseReplacement(
                    original = "PAYMENT_METHOD_REMOVE_LAST_FEATURE",
                    new = removeLastFeature,
                ),
                ResponseReplacement(
                    original = "[PAYMENT_METHODS_HERE]",
                    new = cardsStringified
                )
            ),
        )
    }
}
