package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.pressBack
import app.cash.turbine.Event
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.StripeIntent
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerPaymentOptionsResult
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerSheetArgsFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.android.uicore.elements.bottomsheet.BottomSheetContentTestTag
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers FlowController's payment options behavior when FlowController presents [EmbeddedSheetActivity].
 */
@OptIn(ExperimentalAnalyticEventCallbackApi::class)
@RunWith(RobolectricTestRunner::class)
internal class FlowControllerPaymentOptionsSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(createComposeCleanupRule())
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    private val continueButton
        get() = composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)

    @After
    fun tearDown() {
        PaymentElementCallbackReferences.remove(CALLBACK_IDENTIFIER)
    }

    @Test
    fun `dismissing bottom sheet before selection should return cancel result without selection`() {
        launch { scenario ->
            pressBack()

            val result = scenario.awaitResult() as EmbeddedActivityResult.Cancelled
            assertThat(result.customerState).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
            assertThat(FlowControllerPaymentOptionsResult.from(result, initialPaymentSelection = null)).isEqualTo(
                FlowControllerPaymentOptionsResult.Canceled(
                    paymentSelection = null,
                    linkAccountInfo = LinkAccountUpdate.Value(null),
                    paymentMethods = emptyList(),
                )
            )
        }
    }

    @Test
    fun `dismissing bottom sheet should return cancel result even if there is a selection`() {
        val initialSelection = PaymentSelection.Saved(PaymentMethodFixtures.createCard())
        val usBankAccount = PaymentMethodFixtures.US_BANK_ACCOUNT
        val paymentMethods = listOf(initialSelection.paymentMethod, usBankAccount)

        launch(
            paymentMethods = paymentMethods,
            paymentSelection = initialSelection,
        ) { scenario ->
            // US Bank Account selections wait for Continue because of their mandate.
            composeTestRule
                .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_${usBankAccount.label()}")
                .performClick()
            composeTestRule.waitForIdle()

            pressBack()

            val result = scenario.awaitResult() as EmbeddedActivityResult.Cancelled
            assertThat(FlowControllerPaymentOptionsResult.from(result, initialSelection)).isEqualTo(
                FlowControllerPaymentOptionsResult.Canceled(
                    paymentSelection = initialSelection,
                    linkAccountInfo = LinkAccountUpdate.Value(null),
                    paymentMethods = paymentMethods,
                )
            )
        }
    }

    @Test
    fun `ContinueButton should be hidden when showing payment options`() = launch(
        paymentMethods = PaymentMethodFixtures.createCards(5),
    ) {
        continueButton.assertDoesNotExist()
    }

    @Test
    fun `ContinueButton should be visible when showing add payment method form`() = launch(
        paymentMethodMetadata = metadata(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
        ),
    ) {
        continueButton.performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ContinueButton should be hidden when returning to payment options`() = launch(
        paymentMethods = PaymentMethodFixtures.createCards(5),
    ) {
        continueButton.assertDoesNotExist()

        composeTestRule
            .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_+ Add")
            .performClick()
        onIdle()
        continueButton.performScrollTo().assertIsDisplayed()

        pressBack()
        onIdle()

        continueButton.assertDoesNotExist()
    }

    @Test
    fun `Verify Ready state updates the add button label`() = launch {
        continueButton.assert(hasText("Continue"))
    }

    @Test
    fun `Verify bottom sheet expands on start`() = launch {
        composeTestRule.onNodeWithTag(BottomSheetContentTestTag).assertIsDisplayed()
    }

    @Test
    fun `Verify selecting a payment method closes the sheet`() = launch(
        paymentMethodMetadata = metadata(isGooglePayReady = true),
    ) { scenario ->
        composeTestRule
            .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_Google Pay")
            .performClick()

        val result = scenario.awaitResult() as EmbeddedActivityResult.Complete
        assertThat(result.selection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(result.hasBeenConfirmed).isFalse()
    }

    @Test
    fun `notes visibility is set correctly`() {
        val usBankAccount = PaymentMethodFixtures.US_BANK_ACCOUNT
        val mandateText = "By continuing, you agree to authorize payments pursuant to these terms."

        launch(
            paymentMethodMetadata = metadata(isGooglePayReady = true),
            paymentMethods = listOf(usBankAccount),
        ) { scenario ->
            composeTestRule
                .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_${usBankAccount.label()}")
                .performClick()

            composeTestRule.onNodeWithText(mandateText).assertIsDisplayed()

            composeTestRule
                .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_Google Pay")
                .performClick()

            composeTestRule.onNodeWithText(mandateText).assertDoesNotExist()
            val result = scenario.awaitResult() as EmbeddedActivityResult.Complete
            assertThat(result.selection).isEqualTo(PaymentSelection.GooglePay)
        }
    }

    @Test
    fun `primary button appearance is set`() = launch(
        paymentMethodMetadata = metadata(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_WITHOUT_LINK,
        ),
        configuration = PaymentSheetFixtures.CONFIG_MINIMUM.newBuilder()
            .appearance(
                PaymentSheet.Appearance(
                    primaryButton = PaymentSheet.PrimaryButton(
                        colorsLight = PaymentSheet.PrimaryButtonColors(
                            background = Color.Magenta,
                            onBackground = Color.Magenta,
                            border = Color.Magenta,
                        ),
                        shape = PaymentSheet.PrimaryButtonShape(),
                        typography = PaymentSheet.PrimaryButtonTypography(),
                    )
                )
            ).build(),
    ) {
        continueButton.performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `Reports payment method selection in payment method form screen`() {
        val analyticEvents = Turbine<AnalyticEvent>()
        PaymentElementCallbackReferences[CALLBACK_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .analyticEventCallback { event -> analyticEvents.add(event) }
            .build()

        launch(
            paymentMethodMetadata = metadata(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp", "klarna"),
                ),
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
            ),
        ) {
            composeTestRule
                .onNodeWithTag(TEST_TAG_LIST + PaymentMethod.Type.CashAppPay.code)
                .performClick()
            composeTestRule.waitForIdle()
        }

        // The initial selection isn't reported, as it's not a user selection.
        runTest {
            val selectedTypes = analyticEvents.cancelAndConsumeRemainingEvents()
                .filterIsInstance<Event.Item<AnalyticEvent>>()
                .map { it.value }
                .filterIsInstance<AnalyticEvent.SelectedPaymentMethodType>()
                .map { it.paymentMethodType }
            assertThat(selectedTypes).containsExactly(PaymentMethod.Type.CashAppPay.code)
        }
    }

    @Test
    fun `promotion message is displayed when selecting klarna from saved PMs screen`() = launch(
        paymentMethodMetadata = metadata(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "klarna"),
            ),
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "arb_123",
                experimentAssignments = mapOf(
                    ElementsSession.ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS to "treatment"
                ),
            ),
        ),
        paymentMethods = PaymentMethodFixtures.createCards(1),
        promotions = FakePaymentMethodMessagePromotionsHelper.promotions,
    ) {
        composeTestRule.onNodeWithTag(PaymentOptionsItem.AddCard.viewType.name).performClick()

        composeTestRule.onNodeWithTag("PaymentMethodsUITestTagklarna").performClick()

        composeTestRule.onNodeWithText("This is a message", substring = true).assertIsDisplayed()
    }

    @Test
    fun `tapping a saved payment method in vertical mode returns it without Continue`() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        launch(
            paymentMethodMetadata = metadata(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "cashapp"),
                ),
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
            ),
            paymentMethods = listOf(paymentMethod),
        ) { scenario ->
            composeTestRule
                .onNodeWithTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${paymentMethod.id}")
                .performClick()

            val result = scenario.awaitResult() as EmbeddedActivityResult.Complete
            assertThat(result.selection).isEqualTo(PaymentSelection.Saved(paymentMethod))
        }
    }

    @Test
    fun `vertical payment options list does not validate from the disabled primary button`() = launch(
        paymentMethodMetadata = metadata(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SHEET_PRIMARY_BUTTON_DISABLED_OVERLAY_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `selected result returns the FlowController launch mode`() = launch(
        paymentMethodMetadata = metadata(isGooglePayReady = true),
    ) { scenario ->
        composeTestRule
            .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_Google Pay")
            .performClick()

        val result = scenario.awaitResult() as EmbeddedActivityResult.Complete
        assertThat(result.launchMode).isEqualTo(scenarioArgs.launchMode)
    }

    private lateinit var scenarioArgs: EmbeddedActivityArgs

    private fun launch(
        paymentMethodMetadata: PaymentMethodMetadata = metadata(),
        paymentMethods: List<PaymentMethod> = emptyList(),
        paymentSelection: PaymentSelection? = null,
        configuration: PaymentSheet.Configuration = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
        promotions: List<PaymentMethodMessagePromotion>? = null,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        val args = FlowControllerSheetArgsFactory.create(
            paymentSheetState = PaymentSheetState.Full(
                customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(paymentMethods = paymentMethods),
                config = configuration.asCommonConfiguration(),
                paymentSelection = null,
                validationError = null,
                paymentMethodMetadata = paymentMethodMetadata,
            ),
            configuration = configuration,
            paymentSelection = paymentSelection,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            walletButtonsRendered = false,
            productUsage = setOf("PaymentSheet.FlowController"),
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
            promotions = promotions,
        )
        scenarioArgs = args
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(applicationContext, args)
        ).use { scenario ->
            composeTestRule.waitForIdle()
            block(scenario)
        }
    }

    private fun ActivityScenario<EmbeddedSheetActivity>.awaitResult(): EmbeddedActivityResult {
        lateinit var activity: EmbeddedSheetActivity
        onActivity { activity = it }
        composeTestRule.waitUntilWithIdle(
            conditionDescription = "embedded sheet activity to finish",
        ) {
            activity.isFinishing
        }
        return EmbeddedSheetContract.parseResult(result.resultCode, result.resultData)
    }

    private fun PaymentMethod.label(): String? {
        return getLabel(linkBrand = LinkBrand.Link, canShowSublabel = false)?.resolve(applicationContext)
    }

    private fun metadata(
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        isGooglePayReady: Boolean = false,
        paymentMethodLayout: PaymentSheet.PaymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        experimentsData: ElementsSession.ExperimentsData? = null,
    ): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = stripeIntent,
            experimentsData = experimentsData,
            isGooglePayReady = isGooglePayReady,
            hasCustomerConfiguration = true,
            paymentMethodLayout = paymentMethodLayout,
        )
    }

    private companion object {
        const val CALLBACK_IDENTIFIER = "FlowControllerPaymentOptionsSheetActivityTest"
    }
}
