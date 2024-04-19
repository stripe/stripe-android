package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import com.stripe.android.customersheet.ui.CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG
import com.stripe.android.customersheet.ui.CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG

internal class CustomerSheetPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun waitForText(text: String, substring: Boolean = false) {
        waitUntil(hasText(text, substring = substring))
    }

    fun fillOutFullBillingAddress() {
        replaceText("Address line 1", ADDRESS_LINE_ONE)
        replaceText("Address line 2 (optional)", ADDRESS_LINE_TWO)
        replaceText("City", CITY)

        click(hasText("State"))
        click(hasText(STATE_NAME))
    }

    fun fillOutContactInformation() {
        replaceText("Email", EMAIL)
        replaceText("Phone number", PHONE_NUMBER)
    }

    fun fillOutName() {
        replaceText("Name on card", NAME)
    }

    fun fillOutCardDetails(
        cardNumber: String = CARD_NUMBER,
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

    fun closeKeyboard() {
        Espresso.closeSoftKeyboard()
    }

    fun clickSaveButton() {
        clickPrimaryButton(CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG)
    }

    fun clickConfirmButton() {
        clickPrimaryButton(CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG)
    }

    fun clickSavedPaymentMethod(endsWith: String) {
        val savedPaymentMethodMatcher = hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG)
            .and(hasText(endsWith, substring = true))

        waitUntil(savedPaymentMethodMatcher)
        click(savedPaymentMethodMatcher)
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
        const val EMAIL = "email@email.com"
        const val PHONE_NUMBER = "1234567890"
        const val NAME = "John Doe"
        const val EXPIRY_MONTH = "12"
        const val EXPIRY_YEAR = "2034"
        const val CVC = "123"
        const val ADDRESS_LINE_ONE = "354 Oyster Point Blvd"
        const val ADDRESS_LINE_TWO = "Levels 1-5"
        const val CITY = "South San Francisco"
        private const val STATE_NAME = "California"
        const val STATE = "CA"
        const val ZIP_CODE = "12345"
        const val COUNTRY = "US"
    }
}
