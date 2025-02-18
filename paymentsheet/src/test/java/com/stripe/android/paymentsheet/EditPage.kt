package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.paymentsheet.ui.REMOVE_BUTTON_LOADING
import com.stripe.android.paymentsheet.ui.UPDATE_PM_REMOVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SAVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SCREEN_TEST_TAG
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG
import com.stripe.android.uicore.elements.TEST_TAG_DROP_DOWN_CHOICE

internal class EditPage(
    private val composeTestRule: ComposeTestRule
) {
    fun waitUntilVisible() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(UPDATE_PM_SCREEN_TEST_TAG))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun assertIsVisible() {
        composeTestRule
            .onNodeWithTag(UPDATE_PM_SCREEN_TEST_TAG)
            .assertExists()
    }

    fun setCardBrand(cardBrand: String) {
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClick()

        composeTestRule.onNodeWithTag("${TEST_TAG_DROP_DOWN_CHOICE}_$cardBrand")
            .performClick()
    }

    fun assertInDropdownButDisabled(cardBrand: String) {
        // Click on the dropdown menu to expand it
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClick()

        // Attempt to find the node with the specified cardBrand,
        // assert that it is present (displayed) and disabled
        composeTestRule.onNodeWithTag("${TEST_TAG_DROP_DOWN_CHOICE}_$cardBrand")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        // Optionally, close the dropdown menu if it's still open
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClick()
    }

    fun assertInDropdownAndEnabled(cardBrand: String) {
        // Click on the dropdown menu to expand it
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClick()

        // Attempt to find the node with the specified cardBrand,
        // assert that it is present (displayed) and enabled
        composeTestRule.onNodeWithTag("${TEST_TAG_DROP_DOWN_CHOICE}_$cardBrand")
            .assertIsDisplayed()
            .assertIsEnabled()

        // Optionally, close the dropdown menu if it's still open
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClick()
    }

    fun update(waitUntilComplete: Boolean = true) {
        composeTestRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG)
            .performClick()
        if (waitUntilComplete) {
            composeTestRule.waitUntil(timeoutMillis = 5_000L) {
                composeTestRule
                    .onAllNodes(hasTestTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).and(hasTestMetadata("isLoading=true")))
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
        }
    }

    fun onRemoveButton(): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG)
    }

    fun clickRemove() {
        onRemoveButton().performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule
                .onAllNodes(hasTestTag(REMOVE_BUTTON_LOADING))
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }
}
