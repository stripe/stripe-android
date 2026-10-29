package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.testing.waitUntilWithIdle
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HorizontalSavedPaymentOptionsSelectionTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(createComposeCleanupRule())
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `tapping a saved card returns the selection without Continue`() = runScenario(
        paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
    ) {
        composeTestRule.onNodeWithTag(CARD_TAG).performClick()

        val result = awaitResult()
        assertThat(result.selection).isEqualTo(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        assertThat(result.hasBeenConfirmed).isFalse()
        assertThat(result.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions(visibleWallets = WalletType.entries))
    }

    @Test
    fun `tapping a saved card requiring CVC recollection returns the selection without Continue`() = runScenario(
        paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        requireCvcRecollection = true,
    ) {
        composeTestRule.onNodeWithTag(CARD_TAG).performClick()

        val result = awaitResult()
        assertThat(result.selection).isEqualTo(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        assertThat(result.hasBeenConfirmed).isFalse()
    }

    @Test
    fun `tapping a saved bank account requiring confirmation waits for Continue`() = runScenario(
        paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT,
        paymentMethodTypes = listOf("card", "us_bank_account"),
    ) {
        composeTestRule.onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_\u2066···· 6789\u2069")
            .performClick()
        composeTestRule.waitForIdle()
        onActivity { activity -> assertThat(activity.isFinishing).isFalse() }

        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        val result = awaitResult()
        assertThat(result.selection).isEqualTo(PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT))
        assertThat(result.hasBeenConfirmed).isFalse()
    }

    private fun runScenario(
        paymentMethod: PaymentMethod,
        paymentMethodTypes: List<String> = listOf("card"),
        requireCvcRecollection: Boolean = false,
        block: ActivityScenario<EmbeddedSheetActivity>.() -> Unit,
    ) {
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = paymentMethodTypes,
                    paymentMethodOptionsJsonString =
                        """{"card":{"require_cvc_recollection":$requireCvcRecollection}}""",
                ),
                paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
            ),
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            productUsage = setOf("EmbeddedPaymentElement"),
            paymentElementCallbackIdentifier = "HorizontalSavedPaymentOptionsSelectionTestIdentifier",
            statusBarColor = null,
            selection = null,
            previousNewSelections = Bundle(),
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = listOf(paymentMethod),
            ),
            linkAccountInfo = LinkAccountUpdate.Value(
                account = null,
                lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.LoggedOut,
            ),
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.PaymentOptions(visibleWallets = WalletType.entries),
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

    private companion object {
        const val CARD_TAG = "${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_\u2066···· 4242\u2069"
    }
}
