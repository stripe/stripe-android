package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedActivityState
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.PreviousNewSelections
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.uicore.elements.bottomsheet.BottomSheetContentTestTag
import com.stripe.paymentelementtestpages.FormPage
import com.stripe.paymentelementtestpages.ManagePage
import com.stripe.paymentelementtestpages.VerticalModePage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsEmbeddedSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()
    private val formPage = FormPage(composeTestRule)
    private val managePage = ManagePage(composeTestRule)
    private val verticalModePage = VerticalModePage(composeTestRule)
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `loading renders and cancellation returns PaymentOptions`() = launch(
        loading = true,
    ) { scenario ->
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()

        Espresso.pressBack()
        onIdle()

        val result = EmbeddedSheetContract.parseResult(
            scenario.result.resultCode,
            scenario.result.resultData,
        ) as EmbeddedActivityResult.Cancelled
        assertThat(result.customerState).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(result.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
    }

    @Test
    fun `recreated loading activity remains loading`() = launch(
        loading = true,
    ) { scenario ->
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()

        scenario.recreate()
        onIdle()

        scenario.onActivity { activity ->
            assertThat(activity.isFinishing).isFalse()
        }
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `ready intent updates loading content without replacing composition and survives recreation`() = launch(
        loading = true,
    ) { scenario ->
        val readyIntent = EmbeddedSheetContract.createIntent(
            applicationContext,
            createArgs(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                ),
            ),
        )

        scenario.recreate()
        onIdle()
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()

        val initialBottomSheetNodeId = composeTestRule
            .onNodeWithTag(BottomSheetContentTestTag)
            .fetchSemanticsNode()
            .id
        scenario.onActivity { activity ->
            activity.onNewIntent(readyIntent)
        }
        composeTestRule.waitForIdle()

        formPage.waitUntilVisible()
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertDoesNotExist()
        val currentBottomSheetNodeId = composeTestRule
            .onNodeWithTag(BottomSheetContentTestTag)
            .fetchSemanticsNode()
            .id
        assertThat(currentBottomSheetNodeId).isEqualTo(initialBottomSheetNodeId)

        scenario.recreate()
        onIdle()

        formPage.waitUntilVisible()
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `pressing back returns cancelled result with PaymentOptions launch mode`() = launch { scenario ->
        Espresso.pressBack()

        onIdle()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Cancelled>()
        val cancelled = result as EmbeddedActivityResult.Cancelled
        assertThat(cancelled.launchMode).isEqualTo(
            EmbeddedLaunchMode.PaymentOptions
        )
    }

    @Test
    fun `continue returns the selection and previous new selections`() {
        val previousNewSelection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

        launch(
            selection = selection,
            previousNewSelections = PreviousNewSelections.empty.updatedWith(previousNewSelection),
        ) { scenario ->
            composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
                .performScrollTo()
                .assertIsEnabled()
                .performClick()
            onIdle()

            val result = EmbeddedSheetContract.parseResult(
                scenario.result.resultCode,
                scenario.result.resultData,
            ) as EmbeddedActivityResult.Complete
            assertThat(result.selection).isEqualTo(selection)
            assertThat(result.previousNewSelections["cashapp"])
                .isEqualTo(previousNewSelection)
            assertThat(result.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
        }
    }

    @Test
    fun `new selection requiring a form survives recreation and back returns to the list`() = launch(
        selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
    ) { scenario ->
        formPage.waitUntilVisible()

        scenario.recreate()
        onIdle()
        formPage.waitUntilVisible()

        Espresso.pressBack()
        onIdle()

        formPage.assertIsNotDisplayed()
        verticalModePage.waitUntilVisible()
    }

    @Test
    fun `selecting saved payment method from manage returns to payment options`() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)

        launch(
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = paymentMethods,
            ),
        ) {
            verticalModePage.clickViewMore()
            managePage.waitUntilVisible()

            managePage.selectPaymentMethod(paymentMethods.first().id)
            composeTestRule.waitForIdle()

            managePage.assertNotVisible()
            verticalModePage.waitUntilVisible()
        }
    }

    @Test
    fun `cancelled result contains customer state`() = launch { scenario ->
        Espresso.pressBack()

        onIdle()

        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        val cancelled = result as EmbeddedActivityResult.Cancelled
        assertThat(cancelled.customerState).isNotNull()
    }

    @Test
    fun `saved checkout selection displays update error and re-enables payment options`() {
        val selection = savedSelectionWithBillingAddress()
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
        }

        launch(
            selection = selection,
            paymentMethodMetadata = checkoutPaymentMethodMetadata(),
        ) {
            val primaryButton = composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
            val expectedError = applicationContext.getString(R.string.stripe_something_went_wrong)
            primaryButton.performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText(expectedError)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(expectedError).performScrollTo().assertIsDisplayed()
            primaryButton.assertIsEnabled()
            composeTestRule.onNodeWithTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_card").assertIsEnabled()
            verticalModePage.waitUntilVisible()
        }
    }

    @Test
    fun `processing disables payment options and blocks back`() {
        val requestReceived = CountDownLatch(1)
        val releaseResponse = CountDownLatch(1)
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
            requestReceived.countDown()
            check(releaseResponse.await(10, TimeUnit.SECONDS))
        }

        launch(
            selection = savedSelectionWithBillingAddress(),
            paymentMethodMetadata = checkoutPaymentMethodMetadata(),
        ) { scenario ->
            try {
                val cardRow = composeTestRule.onNodeWithTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_card")
                cardRow.assertIsEnabled()
                composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
                    .performScrollTo()
                    .assertIsEnabled()
                    .performClick()
                val processingLabel = applicationContext.getString(
                    R.string.stripe_paymentsheet_primary_button_processing
                )
                composeTestRule.waitUntil(timeoutMillis = 5_000) {
                    composeTestRule.onAllNodesWithText(processingLabel)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
                }
                composeTestRule.waitUntil(timeoutMillis = 5_000) {
                    requestReceived.count == 0L
                }
                cardRow.assertIsNotEnabled()
                composeTestRule.onNodeWithText(processingLabel).assertIsDisplayed()

                scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }

                verticalModePage.waitUntilVisible()
                cardRow.assertIsNotEnabled()
            } finally {
                releaseResponse.countDown()
            }
        }
    }

    private fun launch(
        selection: PaymentSelection? = null,
        previousNewSelections: PreviousNewSelections = PreviousNewSelections.empty,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        loading: Boolean = false,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) = launch(
        selection = selection,
        previousNewSelections = previousNewSelections,
        paymentMethodMetadata = paymentMethodMetadata,
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = (selection as? PaymentSelection.Saved)?.let {
                listOf(it.paymentMethod)
            }.orEmpty(),
        ),
        loading = loading,
        block = block,
    )

    private fun launch(
        customerState: CustomerState,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) = launch(
        selection = null,
        previousNewSelections = PreviousNewSelections.empty,
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        customerState = customerState,
        block = block,
    )

    private fun launch(
        selection: PaymentSelection?,
        previousNewSelections: PreviousNewSelections,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        loading: Boolean = false,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = createArgs(
                    selection = selection,
                    previousNewSelections = previousNewSelections,
                    paymentMethodMetadata = paymentMethodMetadata,
                    customerState = customerState,
                    loading = loading,
                ),
            )
        ).use { scenario ->
            block(scenario)
        }
    }

    private fun createArgs(
        selection: PaymentSelection? = null,
        previousNewSelections: PreviousNewSelections = PreviousNewSelections.empty,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        customerState: CustomerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        loading: Boolean = false,
    ): EmbeddedActivityState {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
        val context = EmbeddedActivityState.Context(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = configuration,
            productUsage = setOf("EmbeddedPaymentElement"),
            statusBarColor = null,
            paymentElementCallbackIdentifier = "PaymentOptionsTestIdentifier",
        )
        return if (loading) {
            EmbeddedActivityState.LoadingPaymentOptions(
                context = context,
                initialSelection = selection,
                previousNewSelections = previousNewSelections,
                customerState = customerState,
            )
        } else {
            EmbeddedActivityState.Ready.PaymentOptions(
                context = context,
                initialSelection = selection,
                previousNewSelections = previousNewSelections,
                customerState = customerState,
            )
        }
    }

    private fun checkoutPaymentMethodMetadata(): PaymentMethodMetadata {
        val response = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )
        return PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = response.id,
                instancesKey = "test_instances_key",
                checkoutSessionResponse = response,
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        )
    }

    private fun savedSelectionWithBillingAddress(): PaymentSelection.Saved {
        return PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                billingDetails = PaymentMethod.BillingDetails(
                    address = Address(
                        city = "San Francisco",
                        country = "US",
                        line1 = "510 Townsend St",
                        line2 = "Suite 100",
                        postalCode = "94103",
                        state = "CA",
                    ),
                ),
            )
        )
    }
}
