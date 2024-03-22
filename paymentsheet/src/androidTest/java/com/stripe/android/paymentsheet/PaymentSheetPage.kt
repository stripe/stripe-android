package com.stripe.android.paymentsheet

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG

private typealias ComposeTestRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

internal class PaymentSheetPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun fillOutCardDetails(fillOutZipCode: Boolean = true) {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("Card number")

        replaceText("Card number", "4242424242424242")
        replaceText("MM / YY", "12/34")
        replaceText("CVC", "123")

        if (fillOutZipCode) {
            replaceText("ZIP Code", "12345")
        }
    }

    fun fillOutLink() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("Save your info for secure 1-click checkout with Link")
        clickViewWithText("Save your info for secure 1-click checkout with Link")
    }

    fun clickOnSaveForFutureUsage(merchantName: String) {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("Save for future $merchantName payments", true)
        clickViewWithText("Save for future $merchantName payments")
    }

    fun clickOnLinkCheckbox() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("Save your info for secure 1-click checkout with Link")
        clickViewWithText("Save your info for secure 1-click checkout with Link")
    }

    fun fillOutLinkEmail(optionalLabel: Boolean = false) {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        val label = if (optionalLabel) "Email (optional)" else "Email"

        waitForText(label)
        replaceText(label, "email@email.com")
    }

    fun selectPhoneNumberCountry(country: String) {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("Phone number")
        composeTestRule.onNode(hasTestTag("DropDown:tiny")).performClick()
        composeTestRule.onNode(hasText(country, substring = true)).performClick()
    }

    fun fillOutLinkPhone(phoneNumber: String = "+12113526421") {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("Phone number", true)
        replaceText("Phone number", phoneNumber, true)
    }

    fun fillOutLinkName() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("Full name")
        replaceText("Full name", "John Doe")
    }

    fun fillOutCardDetailsWithCardBrandChoice(fillOutZipCode: Boolean = true) {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("Card number")

        replaceText("Card number", "4000002500001001")
        replaceText("MM / YY", "12/34")
        replaceText("CVC", "123")

        clickDropdownMenu()
        waitForText("Select card brand (optional)")
        clickViewWithText("Cartes Bancaires")

        if (fillOutZipCode) {
            replaceText("ZIP Code", "12345")
        }
    }

    fun clickPrimaryButton() {
        composeTestRule.waitUntil(5_000) {
            composeTestRule
                .onAllNodes(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG).and(isEnabled()))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNode(hasTestTag(PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG))
            .performScrollTo()
            .performClick()
    }

    fun clickViewWithText(text: String) {
        composeTestRule.onNode(hasText(text))
            .performScrollTo()
            .performClick()
    }

    fun waitForText(text: String, substring: Boolean = false) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasText(text, substring = substring))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun assertNoText(text: String, substring: Boolean = false) {
        composeTestRule
            .onAllNodes(hasText(text, substring = substring))
            .fetchSemanticsNodes().isEmpty()
    }

    fun addPaymentMethod() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("+ Add")

        composeTestRule.onNode(hasText("+ Add"))
            .onParent()
            .onParent()
            .performClick()
    }

    fun replaceText(label: String, text: String, isLabelSubstring: Boolean = false) {
        composeTestRule.onNode(hasText(label, substring = isLabelSubstring))
            .performScrollTo()
            .performTextReplacement(text)
    }

    private fun clickDropdownMenu() {
        composeTestRule.onNode(hasTestTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG))
            .performScrollTo()
            .performClick()
    }
}
