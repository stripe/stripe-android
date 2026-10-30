package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_MANDATE_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.paymentelementtestpages.FormPage
import com.stripe.paymentelementtestpages.VerticalModePage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsEmbeddedSheetMandateTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()
    private val formPage = FormPage(composeTestRule)
    private val verticalModePage = VerticalModePage(composeTestRule)

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(createComposeCleanupRule())
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `vertical mandate only selection shows mandate and restores it after returning from a form`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp"),
                setupFutureUsage = StripeIntent.Usage.OffSession,
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
    ) {
        verticalModePage.waitUntilVisible()
        verticalModePage.assertMandateDoesNotExists()

        verticalModePage.clickOnNewLpm("cashapp")
        composeTestRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG)
            .performScrollTo()
            .assertTextContains("Cash App", substring = true)

        verticalModePage.clickOnNewLpm("card")
        formPage.waitUntilVisible()
        composeTestRule.onNode(hasText("Cash App", substring = true).and(hasTestTag(SHEET_MANDATE_TEST_TAG)))
            .assertDoesNotExist()

        Espresso.pressBack()
        verticalModePage.waitUntilVisible()
        verticalModePage.assertLpmIsSelected("cashapp")
        composeTestRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG)
            .performScrollTo()
            .assertTextContains("Cash App", substring = true)
    }

    @Test
    fun `vertical saved sepa mandate is shown above primary button`() = runScenario(
        paymentMethodMetadata = metadata(
            paymentMethodTypes = listOf("card", "sepa_debit"),
            layout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        paymentMethods = listOf(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
        selection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
    ) {
        verticalModePage.waitUntilVisible()

        assertMandatePosition(isAbovePrimaryButton = true)
    }

    @Test
    fun `vertical saved card shows Continue without a mandate`() = runScenario(
        paymentMethodMetadata = metadata(
            paymentMethodTypes = listOf("card"),
            layout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
    ) {
        verticalModePage.waitUntilVisible()

        composeTestRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `horizontal saved card hides Continue and mandate`() = runScenario(
        paymentMethodMetadata = metadata(
            paymentMethodTypes = listOf("card", "us_bank_account"),
            layout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD, PaymentMethodFixtures.US_BANK_ACCOUNT),
        selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
    ) {
        composeTestRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `horizontal saved bank account shows mandate below Continue and waits for Continue`() = runScenario(
        paymentMethodMetadata = metadata(
            paymentMethodTypes = listOf("card", "us_bank_account"),
            layout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD, PaymentMethodFixtures.US_BANK_ACCOUNT),
        selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
    ) { scenario ->
        composeTestRule.onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_\u2066···· 6789\u2069")
            .performClick()

        scenario.onActivity { activity -> assertThat(activity.isFinishing).isFalse() }
        assertMandatePosition(isAbovePrimaryButton = false)

        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG).performScrollTo().performClick()

        val result = scenario.awaitResult()
        assertThat(result.selection).isEqualTo(PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT))
        assertThat(result.hasBeenConfirmed).isFalse()
    }

    @Test
    fun `horizontal saved sepa mandate is shown above Continue`() = runScenario(
        paymentMethodMetadata = metadata(
            paymentMethodTypes = listOf("card", "sepa_debit"),
            layout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        paymentMethods = listOf(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
        selection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
    ) {
        assertMandatePosition(isAbovePrimaryButton = true)
    }

    @Test
    fun `horizontal saved sepa mandate respects terms display`() = runScenario(
        paymentMethodMetadata = metadata(
            paymentMethodTypes = listOf("card", "sepa_debit"),
            layout = PaymentSheet.PaymentMethodLayout.Horizontal,
            termsDisplay = mapOf(PaymentMethod.Type.SepaDebit to PaymentSheet.TermsDisplay.NEVER),
        ),
        paymentMethods = listOf(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
        selection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
    ) {
        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG).assertDoesNotExist()
    }

    private fun assertMandatePosition(isAbovePrimaryButton: Boolean) {
        val mandatePosition = composeTestRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .positionInRoot
            .y
        val primaryButtonPosition = composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
            .fetchSemanticsNode()
            .positionInRoot
            .y
        if (isAbovePrimaryButton) {
            assertThat(mandatePosition).isLessThan(primaryButtonPosition)
        } else {
            assertThat(mandatePosition).isGreaterThan(primaryButtonPosition)
        }
    }

    private fun ActivityScenario<EmbeddedSheetActivity>.awaitResult(): EmbeddedActivityResult.Complete {
        lateinit var activity: EmbeddedSheetActivity
        onActivity { activity = it }
        composeTestRule.waitUntilWithIdle(
            conditionDescription = "embedded sheet activity to finish",
        ) {
            activity.isFinishing
        }
        return EmbeddedSheetContract.parseResult(
            result.resultCode,
            result.resultData,
        ) as EmbeddedActivityResult.Complete
    }

    private fun metadata(
        paymentMethodTypes: List<String>,
        layout: PaymentSheet.PaymentMethodLayout,
        termsDisplay: Map<PaymentMethod.Type, PaymentSheet.TermsDisplay> = emptyMap(),
    ): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes,
            ),
            paymentMethodLayout = layout,
            termsDisplay = termsDisplay,
        )
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethods: List<PaymentMethod> = emptyList(),
        selection: PaymentSelection? = null,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            productUsage = setOf("EmbeddedPaymentElement"),
            statusBarColor = null,
            paymentElementCallbackIdentifier = "PaymentOptionsMandateTestIdentifier",
            selection = selection,
            previousNewSelections = Bundle(),
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = paymentMethods,
            ),
            linkAccountInfo = LinkAccountUpdate.Value(
                account = null,
                lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.LoggedOut,
            ),
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
        )
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(context = applicationContext, input = args),
        ).use { scenario ->
            block(scenario)
        }
    }
}
