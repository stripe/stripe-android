package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.common.ui.performClickWithKeyboard
import com.stripe.android.paymentsheet.ui.UPDATE_PM_REMOVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SAVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SCREEN_TEST_TAG
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG
import com.stripe.android.uicore.elements.TEST_TAG_DROP_DOWN_CHOICE

internal class EditPage(
    private val composeTestRule: ComposeTestRule
) {
    fun assertIsVisible() {
        composeTestRule
            .onNodeWithTag(UPDATE_PM_SCREEN_TEST_TAG)
            .assertExists()
    }

    fun setCardBrand(cardBrand: String) {
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClickWithKeyboard()

        composeTestRule.onNodeWithTag("${TEST_TAG_DROP_DOWN_CHOICE}_$cardBrand")
            .performClickWithKeyboard()
    }

    fun assertNotInDropdown(cardBrand: String) {
        // Click on the dropdown menu to expand it
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClickWithKeyboard()

        // Attempt to find the node with the specified cardBrand
        // and assert that it does not exist
        composeTestRule.onNodeWithTag("${TEST_TAG_DROP_DOWN_CHOICE}_$cardBrand")
            .assertDoesNotExist()

        // Optionally, close the dropdown menu if it's still open
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClickWithKeyboard()
    }

    fun update() {
        composeTestRule.onNode(hasAnyAncestor(hasTestTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG)).and(isFocusable()))
            .performClickWithKeyboard()
    }

    fun onRemoveButton(): SemanticsNodeInteraction {
        return composeTestRule.onNode(hasAnyAncestor(hasTestTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG)).and(isFocusable()))
    }

    fun clickRemove() {
        onRemoveButton().performClickWithKeyboard()
        composeTestRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClickWithKeyboard()
    }
}
