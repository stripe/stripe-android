package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.testing.waitUntilWithIdle
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HorizontalSavedPaymentOptionsWalletsTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(createComposeCleanupRule())
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `horizontal saved options offers Link only in the sheet header`() = runScenario(isGooglePayReady = false) {
        composeTestRule.onNodeWithTag(LinkButtonTestTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PaymentOptionsItem.ViewType.Link.name).assertDoesNotExist()

        composeTestRule.onNodeWithTag(LinkButtonTestTag).performClick()

        val result = awaitResult()
        assertThat(result.selection).isEqualTo(PaymentSelection.Link(LinkBrand.Link))
        assertThat(result.hasBeenConfirmed).isFalse()
    }

    @Test
    fun `horizontal saved options keeps Google Pay inline with Link in the header`() = runScenario(
        isGooglePayReady = true,
    ) {
        composeTestRule.onNodeWithTag(LinkButtonTestTag).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PaymentOptionsItem.ViewType.Link.name).assertDoesNotExist()
        composeTestRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PaymentOptionsItem.ViewType.GooglePay.name)
            .assertIsDisplayed()
            .performClick()

        val result = awaitResult()
        assertThat(result.selection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(result.hasBeenConfirmed).isFalse()
    }

    private fun runScenario(
        isGooglePayReady: Boolean,
        block: ActivityScenario<EmbeddedSheetActivity>.() -> Unit,
    ) {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                isGooglePayReady = isGooglePayReady,
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION,
                    loginState = LinkState.LoginState.LoggedOut,
                    signupMode = null,
                ),
            ),
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            productUsage = setOf("EmbeddedPaymentElement"),
            paymentElementCallbackIdentifier = "HorizontalSavedPaymentOptionsWalletsTestIdentifier",
            statusBarColor = null,
            selection = PaymentSelection.Saved(paymentMethod),
            previousNewSelections = Bundle(),
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = listOf(paymentMethod),
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
            scenario.block()
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
        val activityResult = result
        return EmbeddedSheetContract.parseResult(
            activityResult.resultCode,
            activityResult.resultData,
        ) as EmbeddedActivityResult.Complete
    }
}
