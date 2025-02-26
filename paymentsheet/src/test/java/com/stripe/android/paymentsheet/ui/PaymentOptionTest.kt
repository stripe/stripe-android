package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class PaymentOptionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Turns card label into screen reader-friendly text`() {
        val label = "Card ending in 4242"

        setSavedPaymentMethodTabComposeTestRule(
            label = label,
            shouldShowDefaultBadge = false,
        )
        composeTestRule
            .onNodeWithText(label)
            .assertContentDescriptionEquals("Card ending in 4 2 4 2 ")
    }

    @Test
    fun `Correctly hides default badge when not default`() {
        setSavedPaymentMethodTabComposeTestRule(
            shouldShowDefaultBadge = false,
        )
        composeTestRule.onNodeWithTag(
            TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    @Test
    fun `Correctly shows default badge when default`() {
        setSavedPaymentMethodTabComposeTestRule(
            shouldShowDefaultBadge = true,
        )

        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL,
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun `Correctly hide modify badge when not editing`() {
        setSavedPaymentMethodTabComposeTestRule(
            shouldShowDefaultBadge = false,
            shouldShowModifyBadge = false
        )

        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_MODIFY_BADGE,
            useUnmergedTree = true
        ).assertDoesNotExist()
    }

    @Test
    fun `Correctly shows modify badge in editMode`() {
        setSavedPaymentMethodTabComposeTestRule(
            shouldShowDefaultBadge = false,
            shouldShowModifyBadge = true
        )

        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_MODIFY_BADGE,
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun `Correct contentDescription on modify badge`() {
        setSavedPaymentMethodTabComposeTestRule(
            shouldShowDefaultBadge = false,
            shouldShowModifyBadge = true
        )

        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_MODIFY_BADGE,
            useUnmergedTree = true
        ).assertContentDescriptionEquals(modifyBadgeLabel)
    }

    @Test
    fun `Modify badge is clickable`() {
        var didCallOnModifyItem = false

        setSavedPaymentMethodTabComposeTestRule(
            shouldShowDefaultBadge = false,
            shouldShowModifyBadge = true,
            onModifyListener = {
                didCallOnModifyItem = true
            }
        )

        assertThat(didCallOnModifyItem).isFalse()

        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_MODIFY_BADGE,
            useUnmergedTree = true
        ).assertIsEnabled()

        composeTestRule.onNodeWithTag(
            testTag = TEST_TAG_MODIFY_BADGE,
            useUnmergedTree = true
        ).performClick()

        assertThat(didCallOnModifyItem).isTrue()
    }

    private val testLabel: String = "Card ending in 4242"
    private val modifyBadgeLabel: String = "Edit Card ending in 4242"

    private fun setSavedPaymentMethodTabComposeTestRule(
        label: String = testLabel,
        shouldShowDefaultBadge: Boolean,
        shouldShowModifyBadge: Boolean = false,
        onModifyListener: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            SavedPaymentMethodTab(
                viewWidth = 100.dp,
                isSelected = false,
                shouldShowModifyBadge = shouldShowModifyBadge,
                shouldShowDefaultBadge = shouldShowDefaultBadge,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa_ref,
                labelText = label,
                description = label,
                onModifyListener = onModifyListener,
                onItemSelectedListener = {},
                onModifyAccessibilityDescription = "Edit $label"
            )
        }
    }
}
