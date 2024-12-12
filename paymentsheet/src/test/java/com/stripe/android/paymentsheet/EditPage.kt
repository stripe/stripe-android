package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_EDIT_SCREEN_REMOVE_BUTTON
import com.stripe.android.paymentsheet.ui.TEST_TAG_EDIT_SCREEN_UPDATE_BUTTON
import com.stripe.android.paymentsheet.ui.TEST_TAG_PAYMENT_SHEET_EDIT_SCREEN
import com.stripe.android.paymentsheet.ui.UPDATE_PM_REMOVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SAVE_BUTTON_TEST_TAG
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG
import com.stripe.android.uicore.elements.TEST_TAG_DROP_DOWN_CHOICE

internal class EditPage(
    private val composeTestRule: ComposeTestRule
) {
    fun assertIsVisible() {
        composeTestRule
            .onNodeWithTag(TEST_TAG_PAYMENT_SHEET_EDIT_SCREEN)
            .assertExists()
    }

    fun setCardBrand(cardBrand: String) {
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClick()

        composeTestRule.onNodeWithTag("${TEST_TAG_DROP_DOWN_CHOICE}_$cardBrand")
            .performClick()
    }

    fun assertNotInDropdown(cardBrand: String) {
        // Click on the dropdown menu to expand it
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClick()

        // Attempt to find the node with the specified cardBrand
        // and assert that it does not exist
        composeTestRule.onNodeWithTag("${TEST_TAG_DROP_DOWN_CHOICE}_$cardBrand")
            .assertDoesNotExist()

        // Optionally, close the dropdown menu if it's still open
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClick()
    }

    fun update() {
        composeTestRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG)
            .performClick()
    }

    fun onRemoveButton(): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG)
    }
}
