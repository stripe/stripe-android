package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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

    fun update() {
        composeTestRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG)
            .performClick()
    }

    fun onRemoveButton(): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG)
    }

    fun clickRemove() {
        onRemoveButton().performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()
    }
}
