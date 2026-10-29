package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_MANDATE_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
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
    fun `vertical saved payment method mandate is shown above primary button`() = runScenario(
        paymentMethodMetadata = metadata(
            paymentMethodTypes = listOf("card", "sepa_debit"),
            layout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
        selection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
    ) {
        verticalModePage.waitUntilVisible()

        assertMandatePosition(isAbovePrimaryButton = true)
    }

    @Test
    fun `horizontal saved bank account mandate is shown below primary button only while selected`() = runScenario(
        paymentMethodMetadata = metadata(
            paymentMethodTypes = listOf("card", "us_bank_account"),
            layout = PaymentSheet.PaymentMethodLayout.Horizontal,
            isGooglePayReady = true,
        ),
        paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT,
        selection = PaymentSelection.GooglePay,
    ) {
        val mandateText = "By continuing, you agree to authorize payments pursuant to these terms."
        composeTestRule.onNodeWithTag(PaymentOptionsItem.ViewType.GooglePay.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(mandateText).assertDoesNotExist()

        composeTestRule.onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_\u2066···· 6789\u2069")
            .performClick()
        composeTestRule.onNodeWithText(mandateText).performScrollTo().assertIsDisplayed()
        assertMandatePosition(isAbovePrimaryButton = false)
    }

    @Test
    fun `horizontal saved sepa mandate is shown above primary button`() = runScenario(
        paymentMethodMetadata = metadata(
            paymentMethodTypes = listOf("card", "sepa_debit"),
            layout = PaymentSheet.PaymentMethodLayout.Horizontal,
        ),
        paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
        selection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
    ) {
        assertMandatePosition(isAbovePrimaryButton = true)
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

    private fun metadata(
        paymentMethodTypes: List<String>,
        layout: PaymentSheet.PaymentMethodLayout,
        isGooglePayReady: Boolean = false,
    ): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = paymentMethodTypes,
            ),
            paymentMethodLayout = layout,
            isGooglePayReady = isGooglePayReady,
        )
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata,
        paymentMethod: PaymentMethod? = null,
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
                paymentMethods = listOfNotNull(paymentMethod),
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
