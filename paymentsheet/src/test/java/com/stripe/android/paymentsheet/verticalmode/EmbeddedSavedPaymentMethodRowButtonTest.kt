package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
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
        val paymentMethod = PaymentMethodFixtures.displayableCard()

        composeRule.setContent {
            EmbeddedSavedPaymentMethodRowButton(
                paymentMethods = emptyList(),
                displayedSavedPaymentMethod = paymentMethod,
                savedPaymentMethodAction = PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                selection = PaymentMethodVerticalLayoutInteractor.Selection.Saved,
                pendingSavedPaymentMethodId = paymentMethod.paymentMethod.id,
                linkBrand = LinkBrand.Link,
                isEnabled = false,
                onViewMorePaymentMethods = {},
                onManageOneSavedPaymentMethod = {},
                onSelectSavedPaymentMethod = {},
                appearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            )
        }

        composeRule.onNodeWithTag(
            "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${paymentMethod.paymentMethod.id}",
            useUnmergedTree = true,
        )
            .assertIsNotEnabled()
            .assert(hasAnyDescendant(hasTestTag(EMBEDDED_SAVED_PAYMENT_METHOD_PENDING_TEST_TAG)))
            .assert(hasAnyDescendant(hasTestTag(TEST_TAG_ICON_FROM_RES)).not())
        composeRule.onAllNodesWithTag(
            EMBEDDED_SAVED_PAYMENT_METHOD_PENDING_TEST_TAG,
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onNodeWithTag(TEST_TAG_VIEW_MORE, useUnmergedTree = true).assertExists()
    }
}
