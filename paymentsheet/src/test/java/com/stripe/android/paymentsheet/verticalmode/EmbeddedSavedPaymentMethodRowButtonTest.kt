package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.ui.TEST_TAG_ICON_FROM_RES
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class EmbeddedSavedPaymentMethodRowButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `pending row is disabled and replaces its icon with a spinner`() {
        val scenario = runScenario(
            isSelectionPending = true,
            isEnabled = false,
        )

        composeRule.onNodeWithTag(scenario.rowTestTag, useUnmergedTree = true)
            .assertIsNotEnabled()
        assertRowIconState(scenario.rowTestTag, isSelectionPending = true)
        composeRule.onNodeWithTag(TEST_TAG_VIEW_MORE, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `idle row is enabled and keeps its icon`() {
        val scenario = runScenario(
            isSelectionPending = false,
            isEnabled = true,
        )

        composeRule.onNodeWithTag(scenario.rowTestTag, useUnmergedTree = true)
            .assertIsEnabled()
        assertRowIconState(scenario.rowTestTag, isSelectionPending = false)
    }

    @Test
    fun `row with a different pending method keeps its icon`() {
        val scenario = runScenario(
            isSelectionPending = false,
            isEnabled = false,
        )

        composeRule.onNodeWithTag(scenario.rowTestTag, useUnmergedTree = true)
            .assertIsNotEnabled()
        assertRowIconState(scenario.rowTestTag, isSelectionPending = false)
    }

    private fun runScenario(
        isSelectionPending: Boolean,
        isEnabled: Boolean,
    ): Scenario {
        val card = PaymentMethodFixtures.displayableCard()
        val paymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = card.displayName,
            paymentMethod = card.paymentMethod,
            isSelectionPending = isSelectionPending,
        )
        composeRule.setContent {
            EmbeddedSavedPaymentMethodRowButton(
                paymentMethods = emptyList(),
                displayedSavedPaymentMethod = paymentMethod,
                savedPaymentMethodAction = PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                selection = PaymentMethodVerticalLayoutInteractor.Selection.Saved,
                linkBrand = LinkBrand.Link,
                isEnabled = isEnabled,
                onViewMorePaymentMethods = {},
                onManageOneSavedPaymentMethod = {},
                onSelectSavedPaymentMethod = {},
                appearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            )
        }
        return Scenario(
            rowTestTag = "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${paymentMethod.paymentMethod.id}",
        )
    }

    private fun assertRowIconState(
        rowTestTag: String,
        isSelectionPending: Boolean,
    ) {
        val displayedIconTag = if (isSelectionPending) {
            SAVED_PAYMENT_METHOD_PENDING_TEST_TAG
        } else {
            TEST_TAG_ICON_FROM_RES
        }
        val hiddenIconTag = if (isSelectionPending) {
            TEST_TAG_ICON_FROM_RES
        } else {
            SAVED_PAYMENT_METHOD_PENDING_TEST_TAG
        }

        composeRule.onNodeWithTag(rowTestTag, useUnmergedTree = true)
            .assert(hasAnyDescendant(hasTestTag(displayedIconTag)))
            .assert(hasAnyDescendant(hasTestTag(hiddenIconTag)).not())
        composeRule.onAllNodesWithTag(
            SAVED_PAYMENT_METHOD_PENDING_TEST_TAG,
            useUnmergedTree = true,
        ).assertCountEquals(if (isSelectionPending) 1 else 0)
    }

    private data class Scenario(
        val rowTestTag: String,
    )
}
