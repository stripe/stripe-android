package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.stripe.android.customersheet.ui.CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG
import com.stripe.android.customersheet.ui.CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG

internal class CustomerSheetPage(
    private val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>,
) {
    fun waitForText(text: String, substring: Boolean = false) {
        waitUntil(hasText(text, substring = substring))
    }

    fun fillOutCardDetails(
        cardNumber: String = CARD_NUMBER
    ) {
        replaceText("Card number", cardNumber)
        replaceText("MM / YY", "$EXPIRY_MONTH/$${EXPIRY_YEAR.substring(startIndex = 2)}")
        replaceText("CVC", CVC)
        replaceText("ZIP Code", ZIP_CODE)
    }

    fun changeCardBrandChoice() {
        clickDropdownMenu()
        clickOnCartesBancaires()
    }

    fun clickSaveButton() {
        clickPrimaryButton(CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG)
    }

    fun clickConfirmButton() {
        clickPrimaryButton(CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG)
    }

    private fun clickPrimaryButton(tag: String) {
        waitUntil(hasTestTag(tag).and(isEnabled()))
        click(hasTestTag(tag))
        waitForIdle()
    }

    private fun waitForIdle() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    private fun waitUntil(matcher: SemanticsMatcher) {
        waitForIdle()

        composeTestRule.waitUntil(5_000) {
            composeTestRule
                .onAllNodes(matcher.and(isEnabled()))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun click(matcher: SemanticsMatcher) {
        composeTestRule.onNode(matcher)
            .performScrollTo()
            .performClick()
    }

    private fun replaceText(label: String, text: String, isLabelSubstring: Boolean = false) {
        waitForText(label, substring = isLabelSubstring)

        composeTestRule.onNode(hasText(label, substring = isLabelSubstring))
            .performScrollTo()
            .performTextReplacement(text)
    }

    private fun clickDropdownMenu() {
        waitForIdle()

        composeTestRule.onNode(hasTestTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG))
            .performScrollTo()
            .performClick()
    }

    private fun clickOnCartesBancaires() {
        waitForText("Select card brand (optional)")
        click(hasText("Cartes Bancaires"))
    }

    companion object {
        const val CARD_NUMBER = "4242424242424242"
        const val EXPIRY_MONTH = "12"
        const val EXPIRY_YEAR = "2034"
        const val CVC = "123"
        const val ZIP_CODE = "12345"
        const val COUNTRY = "US"
    }
}
