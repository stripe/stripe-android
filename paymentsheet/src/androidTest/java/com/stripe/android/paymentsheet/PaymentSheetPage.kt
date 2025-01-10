@file:OptIn(ExperimentalTestApi::class)

package com.stripe.android.paymentsheet

import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT
import com.stripe.android.ui.core.elements.SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG

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

    fun clearCard() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("4242 4242 4242 4242")

        replaceText("4242 4242 4242 4242", "")
    }

    fun fillCard() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("Card number")
        replaceText("Card number", "4242424242424242")
    }

    fun fillOutLink() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForText("Save your info for secure 1-click checkout with Link")
        clickViewWithText("Save your info for secure 1-click checkout with Link")
    }

    fun clickOnSaveForFutureUsage() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG)
        clickViewWithTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG)
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

    fun assertPrimaryButton(expectedContentDescription: String, canPay: Boolean) {
        onView(withId(R.id.primary_button)).check { view, _ ->
            val nodeInfo = AccessibilityNodeInfo()
            view.onInitializeAccessibilityNodeInfo(nodeInfo)
            assertThat(nodeInfo.contentDescription).isEqualTo(expectedContentDescription)
            assertThat(nodeInfo.className).isEqualTo(Button::class.java.name)
            if (canPay) {
                matches(isClickable())
                matches(ViewMatchers.isEnabled())
            } else {
                matches(isNotEnabled())
            }
        }
    }

    fun fillCvcRecollection(cvc: String) {
        waitForText("Confirm your CVC")
        composeTestRule
            .onNodeWithText("CVC")
            .performTextInput(cvc)
    }

    fun clickViewWithText(text: String) {
        composeTestRule.onNode(hasText(text))
            .performScrollTo()
            .performClick()
    }

    fun clickViewWithTag(testTag: String) {
        composeTestRule.onNode(hasTestTag(testTag))
            .performScrollTo()
            .performClick()
    }

    fun waitForTag(testTag: String) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag(testTag))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun waitForText(text: String, substring: Boolean = false) {
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
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

    fun checkSaveForFuture() {
        composeTestRule.onNode(hasTestTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG))
            .performScrollTo()
            .performClick()
    }

    fun clickOnLpm(code: String, forVerticalMode: Boolean = false) {
        if (forVerticalMode) {
            composeTestRule.waitUntil {
                composeTestRule
                    .onAllNodes(hasTestTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeTestRule.onNode(hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code"))
                .performScrollTo()
                .performClick()
        } else {
            composeTestRule.waitUntilExactlyOneExists(
                hasTestTag(FORM_ELEMENT_TEST_TAG)
            )

            val paymentMethodMatcher = hasTestTag(TEST_TAG_LIST + code)

            composeTestRule.onNodeWithTag(TEST_TAG_LIST, true)
                .performScrollToNode(paymentMethodMatcher)
            composeTestRule.waitForIdle()
            composeTestRule
                .onNode(paymentMethodMatcher)
                .assertIsDisplayed()
                .assertIsEnabled()
                .performClick()
        }

        composeTestRule.waitForIdle()
    }

    fun assertIsOnFormPage() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag(FORM_ELEMENT_TEST_TAG))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun assertLpmSelected(code: String) {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodes(hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code").and(isSelected()))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    fun fillOutKonbini(fullName: String, email: String, phone: String) {
        replaceText("Full name", fullName)
        replaceText("Email", email)
        replaceText("Phone (optional)", phone)
    }
}
