package com.stripe.paymentelementtestpages

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.paymentsheet.ui.REMOVE_BUTTON_LOADING
import com.stripe.android.paymentsheet.ui.UPDATE_PM_REMOVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SAVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SCREEN_TEST_TAG
import com.stripe.android.paymentsheet.ui.UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON
import com.stripe.android.uicore.elements.SELECTOR_ITEM_TEST_TAG

@SuppressWarnings("TooManyFunctions")
class EditPage(
    private val composeTestRule: ComposeTestRule
) {
    fun waitUntilVisible() {
        composeTestRule.waitUntilWithIdle {
            composeTestRule
                .onAllNodes(hasTestTag(UPDATE_PM_SCREEN_TEST_TAG))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    fun waitUntilMissing() {
        composeTestRule.waitUntilWithIdle {
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

    fun setCardBrandWithSelector(cardBrand: String) {
        composeTestRule.onNodeWithContentDescription(cardBrand)
            .performClick()
    }

    fun assertInSelectorButDisabled(cardBrand: String) {
        // Attempt to find the node with the specified cardBrand,
        // assert that it is present (displayed) and disabled
        composeTestRule.onNodeWithTag("${SELECTOR_ITEM_TEST_TAG}_$cardBrand")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    fun assertInSelectorAndEnabled(cardBrand: String) {
        // Attempt to find the node with the specified cardBrand,
        // assert that it is present (displayed) and enabled
        composeTestRule.onNodeWithTag("${SELECTOR_ITEM_TEST_TAG}_$cardBrand")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    fun update(waitUntilComplete: Boolean = true) {
        composeTestRule.onNodeWithTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG)
            .performClick()
        if (waitUntilComplete) {
            composeTestRule.waitUntilWithIdle {
                composeTestRule
                    .onAllNodes(
                        hasTestTag(UPDATE_PM_SAVE_BUTTON_TEST_TAG).and(
                            hasTestMetadata(
                                "isLoading=true"
                            )
                        )
                    )
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
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
        composeTestRule.waitUntilWithIdle {
            composeTestRule
                .onAllNodes(hasTestTag(REMOVE_BUTTON_LOADING))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    fun onSetAsDefaultCheckbox(): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(
            UPDATE_PM_SET_AS_DEFAULT_CHECKBOX_TEST_TAG
        )
    }

    fun clickSetAsDefaultCheckbox() {
        onSetAsDefaultCheckbox().performClick()
    }
}
