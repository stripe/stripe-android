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
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
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
            selectionState = SavedPaymentMethodSelectionState.Pending,
            isEnabled = false,
        )

        savedPaymentMethodRow(scenario.rowTestTag)
            .assertIsNotEnabled()
            .assert(hasAnyDescendant(hasTestTag(SAVED_PAYMENT_METHOD_PENDING_TEST_TAG)))
            .assert(hasAnyDescendant(hasTestTag(TEST_TAG_ICON_FROM_RES)).not())
        pendingSavedPaymentMethodRows().assertCountEquals(1)
        composeRule.onNodeWithTag(TEST_TAG_VIEW_MORE, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `idle row is enabled and keeps its icon`() {
        val scenario = runScenario(
            selectionState = SavedPaymentMethodSelectionState.Idle,
            isEnabled = true,
        )

        savedPaymentMethodRow(scenario.rowTestTag)
            .assertIsEnabled()
            .assert(hasAnyDescendant(hasTestTag(TEST_TAG_ICON_FROM_RES)))
            .assert(hasAnyDescendant(hasTestTag(SAVED_PAYMENT_METHOD_PENDING_TEST_TAG)).not())
        pendingSavedPaymentMethodRows().assertCountEquals(0)
    }

    @Test
    fun `disabled row that is not pending keeps its icon`() {
        val scenario = runScenario(
            selectionState = SavedPaymentMethodSelectionState.Idle,
            isEnabled = false,
        )

        savedPaymentMethodRow(scenario.rowTestTag)
            .assertIsNotEnabled()
            .assert(hasAnyDescendant(hasTestTag(TEST_TAG_ICON_FROM_RES)))
            .assert(hasAnyDescendant(hasTestTag(SAVED_PAYMENT_METHOD_PENDING_TEST_TAG)).not())
        pendingSavedPaymentMethodRows().assertCountEquals(0)
    }

    private fun runScenario(
        selectionState: SavedPaymentMethodSelectionState,
        isEnabled: Boolean,
    ): Scenario {
        val card = PaymentMethodFixtures.displayableCard()
        val paymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = card.displayName,
            paymentMethod = card.paymentMethod,
            selectionState = selectionState,
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

    private fun savedPaymentMethodRow(rowTestTag: String) = composeRule.onNodeWithTag(
        rowTestTag,
        useUnmergedTree = true,
    )

    private fun pendingSavedPaymentMethodRows() = composeRule.onAllNodesWithTag(
        SAVED_PAYMENT_METHOD_PENDING_TEST_TAG,
        useUnmergedTree = true,
    )

    private data class Scenario(
        val rowTestTag: String,
    )
}
