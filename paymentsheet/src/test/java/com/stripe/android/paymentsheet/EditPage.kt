package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.paymentsheet.ui.REMOVE_BUTTON_LOADING
import com.stripe.android.common.ui.performClickWithKeyboard
import com.stripe.android.paymentsheet.ui.UPDATE_PM_REMOVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SAVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SCREEN_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG
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

    fun waitUntilMissing() {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag(UPDATE_PM_SCREEN_TEST_TAG))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

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

    fun assertInDropdownButDisabled(cardBrand: String) {
        // Click on the dropdown menu to expand it
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClickWithKeyboard()

        // Attempt to find the node with the specified cardBrand,
        // assert that it is present (displayed) and disabled
        composeTestRule.onNodeWithTag("${TEST_TAG_DROP_DOWN_CHOICE}_$cardBrand")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        // Optionally, close the dropdown menu if it's still open
        composeTestRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .performClickWithKeyboard()
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
            .performClickWithKeyboard()
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
        return composeTestRule.onNode(hasAnyAncestor(hasTestTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG)).and(isFocusable()))
    }

    fun clickRemove() {
        onRemoveButton().performClickWithKeyboard()
        composeTestRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule
                .onAllNodes(hasTestTag(REMOVE_BUTTON_LOADING))
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    fun clickSetAsDefaultCheckbox() {
        composeTestRule.onNodeWithTag(
            UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG
        ).performClick()
    }
}
