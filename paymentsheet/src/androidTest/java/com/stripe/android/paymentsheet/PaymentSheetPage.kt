package com.stripe.android.paymentsheet

import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.ui.FORM_ELEMENT_TEST_TAG
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_ERROR_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_MANDATE_TEST_TAG
import com.stripe.android.paymentsheet.ui.SHEET_PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_LIST
import com.stripe.android.paymentsheet.ui.TEST_TAG_MODIFY_BADGE
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.testing.fillExpirationDate
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.replaceText
import com.stripe.android.testing.waitForExactlyOneNode
import com.stripe.android.testing.waitForNoNodes
import com.stripe.android.testing.waitForNode
import com.stripe.android.ui.core.elements.MANDATE_TEST_TAG
import com.stripe.android.ui.core.elements.SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG
import com.stripe.android.ui.core.elements.SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG

internal class PaymentSheetPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun waitForCardForm() {
        waitForText("Card number")
    }

    fun fillOutCardDetails(fillOutZipCode: Boolean = true) {
        fillOutCardDetailsWithCardNumber("4242424242424242", fillOutZipCode)
    }

    fun fillOutCardDetailsWithCardNumber(cardNumber: String, fillOutZipCode: Boolean = true) {
        waitForCardForm()

        composeTestRule.replaceText("Card number", cardNumber)
        composeTestRule.fillExpirationDate("12/34")
        composeTestRule.replaceText("CVC", "123")

        if (fillOutZipCode) {
            composeTestRule.replaceText("ZIP Code", "12345")
        }
    }

    fun fillOutBillingCollectionDetails(
        name: String = "John Doe",
        email: String = "email@email.com",
        phone: String = "1234567890",
        addressLineOne: String = "123 Apple Street",
        city: String = "South San Francisco",
        zipCode: String = "12345"
    ) {
        waitForText("Full name")
        composeTestRule.replaceText("Full name", name)

        waitForText("Email")
        composeTestRule.replaceText("Email", email)

        waitForText("Phone number")
        composeTestRule.replaceText("Phone number", phone)

        waitForText("Address line 1")
        composeTestRule.replaceText("Address line 1", addressLineOne)

        waitForText("City")
        composeTestRule.replaceText("City", city)

        waitForText("State")
        clickViewWithText("State")

        waitForText("California")
        clickViewWithText("California")

        waitForText("ZIP Code")
        composeTestRule.replaceText("ZIP Code", zipCode)
    }

    fun clearCard() {
        waitForText("4242 4242 4242 4242")

        composeTestRule.replaceText("4242 4242 4242 4242", "")
    }

    fun fillCard() {
        waitForText("Card number")
        composeTestRule.replaceText("Card number", "4242424242424242")
    }

    fun fillOutLink() {
        waitForText("Save my info for faster checkout with Link")
        clickViewWithText("Save my info for faster checkout with Link")
    }

    fun fillOutFieldWithLabel(label: String, text: String) {
        waitForText(label)
        composeTestRule.replaceText(label, text)
    }

    fun clickAndFillField(label: String, text: String) {
        waitForText(label)
        composeTestRule.onNode(hasText(label))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.replaceText(
            matcher = hasText(label),
            text = text,
            scrollBehavior = ScrollBehavior.Never,
            settleAfterReplacement = false,
        )
    }

    fun clickSavedCard(last4: String) {
        val savedCardTagMatcher = hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG)
            .and(hasText(last4, substring = true))
        composeTestRule.waitForExactlyOneNode(
            matcher = savedCardTagMatcher,
            atLeastOneRootRequired = false,
        )
        composeTestRule.onNode(savedCardTagMatcher).performClick()
    }

    fun clickSavedCardEditBadge(last4: String) {
        val badgeTagMatcher = hasTestTag(TEST_TAG_MODIFY_BADGE)
            .and(hasAnyAncestor(hasText(last4, substring = true)))
        composeTestRule.waitForExactlyOneNode(
            matcher = badgeTagMatcher,
            atLeastOneRootRequired = false,
        )
        composeTestRule.onNode(badgeTagMatcher).performClick()
    }

    fun clickEditButton() {
        waitForText("EDIT")
        composeTestRule
            .onNodeWithText("EDIT")
            .performClick()
    }

    fun clickDoneButton() {
        waitForText("DONE")
        composeTestRule
            .onNodeWithText("DONE")
            .performClick()
    }

    fun clickOnSaveForFutureUsage() {
        waitForTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG)
        clickViewWithTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG)
    }

    fun clickOnLinkCheckbox() {
        waitForText("Save my info for faster checkout with Link")
        clickViewWithText("Save my info for faster checkout with Link")
    }

    fun fillOutLinkEmail(optionalLabel: Boolean = false) {
        val label = if (optionalLabel) "Email (optional)" else "Email"

        waitForText(label)
        composeTestRule.replaceText(label, "email@email.com")
    }

    fun selectPhoneNumberCountry(country: String) {
        waitForText("Phone number")
        composeTestRule.onNode(hasTestTag("DropDown:tiny")).performClick()
        composeTestRule.onNode(hasText(country, substring = true)).performClick()
    }

    fun fillOutLinkPhone(phoneNumber: String = "+12113526421") {
        waitForText("Phone number", true)
        composeTestRule.replaceText("Phone number", phoneNumber, substring = true)
    }

    fun fillOutLinkName() {
        waitForText("Full name")
        composeTestRule.replaceText("Full name", "John Doe")
    }

    fun fillOutCardDetailsWithCardBrandChoice(fillOutZipCode: Boolean = true) {
        waitForText("Card number")

        composeTestRule.replaceText("Card number", "4000002500001001")
        composeTestRule.fillExpirationDate("12/34")
        composeTestRule.replaceText("CVC", "123")

        clickDropdownMenu()
        waitForText("Select card brand (optional)")
        clickViewWithText("Cartes Bancaires")

        if (fillOutZipCode) {
            composeTestRule.replaceText("ZIP Code", "12345")
        }
    }

    fun fillOutCardDetailsWithCardBrandChoiceSelector(fillOutZipCode: Boolean = true) {
        waitForText("Card number")

        composeTestRule.replaceText("Card number", "4000002500001001")
        composeTestRule.fillExpirationDate("12/34")
        composeTestRule.replaceText("CVC", "123")

        clickViewWithContentDescription("Cartes Bancaires")

        if (fillOutZipCode) {
            composeTestRule.replaceText("ZIP Code", "12345")
        }
    }

    fun clickPrimaryButton() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG).and(isEnabled()),
            atLeastOneRootRequired = true,
        )

        composeTestRule.onNode(hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG))
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
    }

    fun assertPrimaryButton(expectedStateDescription: String, canPay: Boolean) {
        onView(withId(R.id.primary_button)).check { view, _ ->
            val nodeInfo = AccessibilityNodeInfo()
            view.onInitializeAccessibilityNodeInfo(nodeInfo)
            assertThat(nodeInfo.stateDescription).isEqualTo(expectedStateDescription)
            assertThat(nodeInfo.className).isEqualTo(Button::class.java.name)
            if (canPay) {
                assertThat(nodeInfo.isClickable).isTrue()
                assertThat(nodeInfo.isEnabled).isTrue()
            } else {
                assertThat(nodeInfo.isEnabled).isFalse()
            }
        }
    }

    fun assertErrorMessageShown() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SHEET_ERROR_TEST_TAG),
            atLeastOneRootRequired = true,
        )

        composeTestRule.onNodeWithTag(SHEET_ERROR_TEST_TAG).assertIsDisplayed()
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

    fun clickViewWithContentDescription(description: String) {
        composeTestRule.onNode(hasContentDescription(description))
            .performScrollTo()
            .performClick()
    }

    fun waitForTag(testTag: String) {
        composeTestRule.waitForNode(
            matcher = hasTestTag(testTag),
            atLeastOneRootRequired = true,
        )
    }

    fun waitForText(text: String, substring: Boolean = false) {
        composeTestRule.waitForNode(
            matcher = hasText(text, substring = substring),
            timeoutMillis = 10_000,
        )
    }

    fun waitForContentDescription(description: String) {
        composeTestRule.waitForNode(
            matcher = hasContentDescription(description),
            timeoutMillis = 10_000,
            atLeastOneRootRequired = false,
        )
    }

    fun assertNoText(text: String, substring: Boolean = false) {
        composeTestRule
            .onAllNodes(hasText(text, substring = substring))
            .fetchSemanticsNodes().isEmpty()
    }

    fun addPaymentMethod() {
        waitForText("+ Add")

        composeTestRule.onNode(hasTestTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_+ Add"))
            .performClick()
    }

    private fun clickDropdownMenu() {
        composeTestRule.onNode(hasTestTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG))
            .performScrollTo()
            .performClick()
    }

    fun checkSaveForFuture() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG).and(isEnabled()),
            atLeastOneRootRequired = false,
        )
        composeTestRule.onNode(hasTestTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    fun checkSetAsDefaultCheckbox() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG).and(isEnabled()),
            atLeastOneRootRequired = true,
        )
        composeTestRule.onNode(hasTestTag(SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    fun assertNoSetAsDefaultCheckbox() {
        composeTestRule.onAllNodesWithTag(
            SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG
        ).fetchSemanticsNodes().isEmpty()
    }

    fun assertSetAsDefaultCheckboxNotChecked() {
        val testTag = SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG
        composeTestRule.waitForNode(
            matcher = hasTestTag(testTag).and(isToggleable()).and(isOff()),
            atLeastOneRootRequired = true,
        )
    }

    fun assertSetAsDefaultCheckboxChecked() {
        val testTag = SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG
        composeTestRule.waitForNode(
            matcher = hasTestTag(testTag).and(isToggleable()).and(isOn()),
            atLeastOneRootRequired = true,
        )
    }

    fun assertNoSaveForFutureCheckbox() {
        composeTestRule.onNodeWithTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG)
            .assertDoesNotExist()
    }

    fun assertSaveForFutureCheckboxNotChecked() {
        val testTag = SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG
        composeTestRule.waitForNode(
            matcher = hasTestTag(testTag).and(isToggleable()).and(isOff()),
            atLeastOneRootRequired = true,
        )
    }

    fun assertSaveForFutureUseCheckboxChecked() {
        val testTag = SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG
        composeTestRule.waitForNode(
            matcher = hasTestTag(testTag).and(isToggleable()).and(isOn()),
            atLeastOneRootRequired = true,
        )
    }

    fun waitUntilVisible() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG),
            atLeastOneRootRequired = false,
        )
    }

    fun waitUntilMissing() {
        composeTestRule.waitForNoNodes(
            matcher = hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG),
            atLeastOneRootRequired = false,
        )
    }

    fun clickOnLpm(code: String, forVerticalMode: Boolean = false) {
        composeTestRule.waitForIdle()
        waitUntilVisible()

        if (forVerticalMode) {
            composeTestRule.waitForNode(
                matcher = hasTestTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT),
                atLeastOneRootRequired = true,
            )

            composeTestRule.onNode(hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code"))
                .performScrollTo()
                .performClick()
        } else {
            composeTestRule.waitForExactlyOneNode(
                matcher = hasTestTag(FORM_ELEMENT_TEST_TAG),
                atLeastOneRootRequired = false,
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

    fun assertLayout(isVerticalMode: Boolean) {
        val testTagForLayout = if (isVerticalMode) {
            TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT
        } else {
            TEST_TAG_LIST
        }

        composeTestRule.waitForNode(
            matcher = hasTestTag(testTagForLayout),
            atLeastOneRootRequired = true,
        )
        composeTestRule.onNodeWithTag(testTagForLayout).assertExists()
    }

    fun assertIsOnFormPage() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(FORM_ELEMENT_TEST_TAG),
            atLeastOneRootRequired = false,
        )
    }

    fun assertLpmSelected(code: String) {
        composeTestRule.waitForNode(
            matcher = hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code").and(isSelected()),
            atLeastOneRootRequired = false,
        )
    }

    fun fillOutKonbini(fullName: String, email: String, phone: String) {
        composeTestRule.replaceText("Full name", fullName)
        composeTestRule.replaceText("Email", email)
        composeTestRule.replaceText("Phone (optional)", phone)
    }

    fun assertGooglePayIsDisplayed() {
        composeTestRule.onNode(hasTestTag(GOOGLE_PAY_BUTTON_TEST_TAG)).assertIsDisplayed()
    }

    fun assertHasMandate(mandateText: String, substring: Boolean = false) {
        composeTestRule
            .onNode(hasText(mandateText, substring = substring))
            .assertExists()
    }

    fun assertMandateIsMissing() {
        waitUntilVisible()
        assertIsOnFormPage()

        composeTestRule.onNodeWithTag(MANDATE_TEST_TAG)
            .assertDoesNotExist()

        composeTestRule.onNodeWithTag(SHEET_MANDATE_TEST_TAG)
            .assertDoesNotExist()
    }

    fun assertSavedSelection(paymentMethodId: String) {
        waitUntilVisible()

        composeTestRule.waitForNode(
            matcher = hasTestTag("${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodId")
                .and(isSelected()),
            atLeastOneRootRequired = true,
        )
    }
}
