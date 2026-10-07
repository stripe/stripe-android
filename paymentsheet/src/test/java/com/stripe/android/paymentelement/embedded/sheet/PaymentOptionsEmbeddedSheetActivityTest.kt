package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
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
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.previousNewSelection
import com.stripe.android.paymentelement.embedded.stashNewSelection
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.android.uicore.elements.bottomsheet.BottomSheetContentTestTag
import com.stripe.paymentelementtestpages.FormPage
import com.stripe.paymentelementtestpages.ManagePage
import com.stripe.paymentelementtestpages.VerticalModePage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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
        .around(createComposeCleanupRule())
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `loading renders and cancellation returns PaymentOptions`() = launch(
        presentationState = EmbeddedActivityArgs.PresentationState.Loading,
    ) { scenario ->
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()

        Espresso.pressBack()
        onIdle()

        val result = EmbeddedSheetContract.parseResult(
            scenario.result.resultCode,
            scenario.result.resultData,
        ) as EmbeddedActivityResult.Cancelled
        assertThat(result.customerState).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(result.linkAccountInfo.lastUpdateReason)
            .isEqualTo(LinkAccountUpdate.Value.UpdateReason.LoggedOut)
        assertThat(result.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
    }

    @Test
    fun `recreated loading activity remains loading`() = launch(
        presentationState = EmbeddedActivityArgs.PresentationState.Loading,
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
        presentationState = EmbeddedActivityArgs.PresentationState.Loading,
    ) { scenario ->
        val readyIntent = EmbeddedSheetContract.createIntent(
            applicationContext,
            createArgs(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                ),
                presentationState = EmbeddedActivityArgs.PresentationState.Ready,
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
            previousNewSelections = Bundle().apply {
                stashNewSelection(previousNewSelection)
            },
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
            assertThat(result.previousNewSelections.previousNewSelection("cashapp"))
                .isEqualTo(previousNewSelection)
            assertThat(result.linkAccountInfo.lastUpdateReason)
                .isEqualTo(LinkAccountUpdate.Value.UpdateReason.LoggedOut)
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
    fun `vertical layout with one payment method opens form directly`() = launch(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        formPage.waitUntilVisible()
    }

    @Test
    fun `horizontal saved payment options selects saved method and continues`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        launchHorizontal(customerState = customerStateWith(paymentMethod)) { scenario ->
            composeTestRule.onNodeWithTag(
                "${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_\u2066···· 4242\u2069"
            ).performClick()
            composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
                .performScrollTo()
                .assertIsEnabled()
                .performClick()
            onIdle()

            val result = EmbeddedSheetContract.parseResult(
                scenario.result.resultCode,
                scenario.result.resultData,
            ) as EmbeddedActivityResult.Complete
            assertThat(result.selection).isEqualTo(PaymentSelection.Saved(paymentMethod))
        }
    }

    @Test
    fun `horizontal bank Continue collects account before returning selection`() = launch(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("us_bank_account"),
                paymentMethodOptionsJsonString = """{"us_bank_account":{"verification_method":"automatic"}}""",
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
    ) { scenario ->
        formPage.waitUntilVisible()
        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG).assertIsNotEnabled()
        formPage.fillOutName()
        formPage.fillOutEmail()
        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        onIdle()

        scenario.onActivity { activity ->
            assertThat(activity.isFinishing).isFalse()
            val shadowActivity = shadowOf(activity)
            val launched = requireNotNull(shadowActivity.nextStartedActivityForResult)
            assertThat(launched.intent.component?.className).isEqualTo(CollectBankAccountActivity::class.java.name)
            val args = requireNotNull(CollectBankAccountContract.Args.fromIntent(launched.intent))
            assertThat(args).isInstanceOf<CollectBankAccountContract.Args.ForPaymentIntent>()
            val configuration = args.configuration as CollectBankAccountConfiguration.USBankAccountInternal
            assertThat(configuration.name).isEqualTo("Jane Doe")
            assertThat(configuration.email).isEqualTo("janedoe@example.com")
            assertThat(shadowActivity.nextStartedActivityForResult).isNull()

            shadowActivity.receiveResult(
                launched.intent,
                Activity.RESULT_OK,
                Intent().putExtras(collectedBankAccountResult().toBundle()),
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Test Bank •••• 6789").performScrollTo().assertIsDisplayed()
        scenario.onActivity { activity ->
            assertThat(activity.isFinishing).isFalse()
        }
        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        onIdle()

        val result = EmbeddedSheetContract.parseResult(
            scenario.result.resultCode,
            scenario.result.resultData,
        ) as EmbeddedActivityResult.Complete
        assertThat(result.selection).isInstanceOf<PaymentSelection.New.USBankAccount>()
        val selection = result.selection as PaymentSelection.New.USBankAccount
        assertThat(selection.screenState.linkedBankAccount?.bankName).isEqualTo("Test Bank")
        assertThat(selection.screenState.linkedBankAccount?.last4).isEqualTo("6789")
        assertThat(selection.input.name).isEqualTo("Jane Doe")
        assertThat(selection.input.email).isEqualTo("janedoe@example.com")
    }

    @Test
    fun `reopening selected horizontal bank restores details and enables Continue`() = runReopenedBankScenario(
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
    )

    @Test
    fun `reopening selected vertical bank restores details and enables Continue`() = runReopenedBankScenario(
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
    )

    @Test
    fun `horizontal saved payment options add opens payment method form`() {
        launchHorizontal(customerState = customerStateWith(PaymentMethodFixtures.CARD_PAYMENT_METHOD)) {
            composeTestRule.onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_+ Add")
                .performClick()

            formPage.waitUntilVisible()
        }
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

            composeTestRule.waitUntilWithIdle {
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
                composeTestRule.waitUntilWithIdle {
                    composeTestRule.onAllNodesWithText(processingLabel)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
                }
                composeTestRule.waitUntilWithIdle {
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
        previousNewSelections: Bundle = Bundle(),
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        presentationState: EmbeddedActivityArgs.PresentationState = EmbeddedActivityArgs.PresentationState.Ready,
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
        presentationState = presentationState,
        block = block,
    )

    private fun launch(
        customerState: CustomerState,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) = launch(
        selection = null,
        previousNewSelections = Bundle(),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        customerState = customerState,
        block = block,
    )

    private fun launch(
        selection: PaymentSelection?,
        previousNewSelections: Bundle,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        presentationState: EmbeddedActivityArgs.PresentationState = EmbeddedActivityArgs.PresentationState.Ready,
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
                    presentationState = presentationState,
                ),
            )
        ).use { scenario ->
            block(scenario)
        }
    }

    private fun launchHorizontal(
        customerState: CustomerState,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) = launch(
        selection = null,
        previousNewSelections = Bundle(),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        customerState = customerState,
        block = block,
    )

    private fun customerStateWith(paymentMethod: PaymentMethod): CustomerState {
        return PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = listOf(paymentMethod),
        )
    }

    private fun createArgs(
        selection: PaymentSelection? = null,
        previousNewSelections: Bundle = Bundle(),
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        customerState: CustomerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        presentationState: EmbeddedActivityArgs.PresentationState,
    ): EmbeddedActivityArgs {
        return EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            productUsage = setOf("EmbeddedPaymentElement"),
            statusBarColor = null,
            paymentElementCallbackIdentifier = "PaymentOptionsTestIdentifier",
            selection = selection,
            previousNewSelections = previousNewSelections,
            customerState = customerState,
            linkAccountInfo = LinkAccountUpdate.Value(
                account = null,
                lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.LoggedOut,
            ),
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            presentationState = presentationState,
        )
    }

    private fun runReopenedBankScenario(paymentMethodLayout: PaymentSheet.PaymentMethodLayout) {
        val selection = PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION.copy(
            input = PaymentMethodFixtures.US_BANK_PAYMENT_SELECTION.input.copy(
                name = "Jane Doe",
                email = "janedoe@example.com",
            ),
        )
        val linkedBankAccount = requireNotNull(selection.screenState.linkedBankAccount)
        launch(
            selection = selection,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("us_bank_account"),
                    paymentMethodOptionsJsonString = """{"us_bank_account":{"verification_method":"automatic"}}""",
                ),
                paymentMethodLayout = paymentMethodLayout,
            ),
        ) { scenario ->
            formPage.waitUntilVisible()
            composeTestRule.onNodeWithText("Full name").assertTextContains(selection.input.name)
            composeTestRule.onNodeWithText("Email").assertTextContains(requireNotNull(selection.input.email))
            composeTestRule.onNodeWithText("${linkedBankAccount.bankName} •••• ${linkedBankAccount.last4}")
                .performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
                .performScrollTo().assertIsEnabled().performClick()
            onIdle()

            val result = EmbeddedSheetContract.parseResult(
                scenario.result.resultCode,
                scenario.result.resultData,
            ) as EmbeddedActivityResult.Complete
            val returnedSelection = result.selection as PaymentSelection.New.USBankAccount
            assertThat(returnedSelection.input.name).isEqualTo(selection.input.name)
            assertThat(returnedSelection.input.email).isEqualTo(selection.input.email)
            assertThat(returnedSelection.screenState.linkedBankAccount?.resultIdentifier)
                .isEqualTo(linkedBankAccount.resultIdentifier)
            assertThat(returnedSelection.screenState.linkedBankAccount?.last4).isEqualTo(linkedBankAccount.last4)
            assertThat(result.hasBeenConfirmed).isFalse()
        }
    }

    private fun collectedBankAccountResult(): CollectBankAccountContract.Result {
        return CollectBankAccountContract.Result(
            CollectBankAccountResultInternal.Completed(
                CollectBankAccountResponseInternal(
                    intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                    usBankAccountData = CollectBankAccountResponseInternal.USBankAccountData(
                        financialConnectionsSession = FinancialConnectionsSession(
                            clientSecret = "fcs_test_secret",
                            id = "fcs_test",
                            livemode = false,
                            paymentAccount = FinancialConnectionsAccount(
                                created = 123,
                                id = "fca_test",
                                institutionName = "Test Bank",
                                livemode = false,
                                last4 = "6789",
                                supportedPaymentMethodTypes = listOf(
                                    FinancialConnectionsAccount.SupportedPaymentMethodTypes.US_BANK_ACCOUNT,
                                ),
                            ),
                        ),
                    ),
                    instantDebitsData = null,
                ),
            ),
        )
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
