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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickNode
import com.stripe.android.testing.fillCardDetails
import com.stripe.android.testing.inputText
import com.stripe.android.testing.replaceText
import com.stripe.android.testing.waitForExactlyOneNode
import com.stripe.android.testing.waitForNode
import com.stripe.android.testing.waitForText
import com.stripe.android.ui.core.elements.MANDATE_TEST_TAG
import com.stripe.android.ui.core.elements.SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG
import com.stripe.android.ui.core.elements.SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG

internal class PaymentSheetPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun waitForCardForm() {
        composeTestRule.waitForText(
            text = "Card number",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
    }

    fun fillOutCardDetails(fillOutZipCode: Boolean = true) {
        fillOutCardDetailsWithCardNumber("4242424242424242", fillOutZipCode)
    }

    fun fillOutCardDetailsWithCardNumber(cardNumber: String, fillOutZipCode: Boolean = true) {
        waitForCardForm()

        composeTestRule.fillCardDetails(
            cardNumber = cardNumber,
            expirationDate = "12/34",
            cvc = "123",
            zipCode = "12345".takeIf { fillOutZipCode },
            textFieldScrollBehavior = ScrollBehavior.Required,
        )
    }

    fun fillOutBillingCollectionDetails(
        name: String = "John Doe",
        email: String = "email@email.com",
        phone: String = "1234567890",
        addressLineOne: String = "123 Apple Street",
        city: String = "South San Francisco",
        zipCode: String = "12345"
    ) {
        composeTestRule.waitForText(
            text = "Full name",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.replaceText("Full name", name)

        composeTestRule.waitForText(
            text = "Email",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.replaceText("Email", email)

        composeTestRule.waitForText(
            text = "Phone number",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.replaceText("Phone number", phone)

        composeTestRule.waitForText(
            text = "Address line 1",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.replaceText("Address line 1", addressLineOne)

        composeTestRule.waitForText(
            text = "City",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.replaceText("City", city)

        composeTestRule.waitForText(
            text = "State",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.clickNode(
            matcher = hasText("State"),
            scrollBehavior = ScrollBehavior.Required,
        )

        composeTestRule.waitForText(
            text = "California",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.clickNode(
            matcher = hasText("California"),
            scrollBehavior = ScrollBehavior.Required,
        )

        composeTestRule.waitForText(
            text = "ZIP Code",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.replaceText("ZIP Code", zipCode)
    }

    fun clearCard() {
        composeTestRule.waitForText(
            text = "4242 4242 4242 4242",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )

        composeTestRule.replaceText("4242 4242 4242 4242", "")
    }

    fun clickAndFillField(label: String, text: String) {
        composeTestRule.waitForText(
            text = label,
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.clickNode(
            matcher = hasText(label),
            scrollBehavior = ScrollBehavior.Required,
        )
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
        composeTestRule.clickNode(
            matcher = savedCardTagMatcher,
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun clickSavedCardEditBadge(last4: String) {
        val badgeTagMatcher = hasTestTag(TEST_TAG_MODIFY_BADGE)
            .and(hasAnyAncestor(hasText(last4, substring = true)))
        composeTestRule.waitForExactlyOneNode(
            matcher = badgeTagMatcher,
            atLeastOneRootRequired = false,
        )
        composeTestRule.clickNode(
            matcher = badgeTagMatcher,
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun clickEditButton() {
        composeTestRule.waitForText(
            text = "EDIT",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.clickNode(
            matcher = hasText("EDIT"),
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun clickDoneButton() {
        composeTestRule.waitForText(
            text = "DONE",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.clickNode(
            matcher = hasText("DONE"),
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun clickOnSaveForFutureUsage() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG),
            atLeastOneRootRequired = true,
        )
        composeTestRule.clickNode(
            matcher = hasTestTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun clickOnLinkCheckbox() {
        composeTestRule.waitForText(
            text = "Save my info for faster checkout with Link",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.clickNode(
            matcher = hasText("Save my info for faster checkout with Link"),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun fillOutLinkEmail(optionalLabel: Boolean = false) {
        val label = if (optionalLabel) "Email (optional)" else "Email"

        composeTestRule.waitForText(
            text = label,
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.replaceText(label, "email@email.com")
    }

    fun selectPhoneNumberCountry(country: String) {
        composeTestRule.waitForText(
            text = "Phone number",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.clickNode(
            matcher = hasTestTag("DropDown:tiny"),
            scrollBehavior = ScrollBehavior.Never,
        )
        composeTestRule.clickNode(
            matcher = hasText(country, substring = true),
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun fillOutLinkPhone(phoneNumber: String = "+12113526421") {
        composeTestRule.waitForText(
            text = "Phone number",
            substring = true,
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.replaceText("Phone number", phoneNumber, substring = true)
    }

    fun fillOutLinkName() {
        composeTestRule.waitForText(
            text = "Full name",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.replaceText("Full name", "John Doe")
    }

    fun fillOutCardDetailsWithCardBrandChoiceSelector(fillOutZipCode: Boolean = true) {
        waitForCardForm()

        composeTestRule.fillCardDetails(
            cardNumber = "4000002500001001",
            expirationDate = "12/34",
            cvc = "123",
            zipCode = null,
            textFieldScrollBehavior = ScrollBehavior.Required,
        )

        composeTestRule.clickNode(
            matcher = hasContentDescription("Cartes Bancaires"),
            scrollBehavior = ScrollBehavior.Required,
        )

        if (fillOutZipCode) {
            composeTestRule.replaceText("ZIP Code", "12345")
        }
    }

    fun clickPrimaryButton() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG).and(isEnabled()),
            atLeastOneRootRequired = true,
        )

        composeTestRule.clickNode(
            matcher = hasTestTag(SHEET_PRIMARY_BUTTON_TEST_TAG),
            scrollBehavior = ScrollBehavior.Required,
        )

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
        composeTestRule.waitForText(
            text = "Confirm your CVC",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )
        composeTestRule.inputText(
            matcher = hasText("CVC"),
            text = cvc,
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun assertNoText(text: String, substring: Boolean = false) {
        composeTestRule
            .onNode(hasText(text, substring = substring))
            .assertDoesNotExist()
    }

    fun addPaymentMethod() {
        composeTestRule.waitForText(
            text = "+ Add",
            timeoutMillis = PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS,
        )

        composeTestRule.clickNode(
            matcher = hasTestTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_+ Add"),
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun checkSaveForFuture() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG).and(isEnabled()),
            atLeastOneRootRequired = false,
        )
        composeTestRule.clickNode(
            matcher = hasTestTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG),
            scrollBehavior = ScrollBehavior.Required,
        )
        composeTestRule.waitForIdle()
    }

    fun checkSetAsDefaultCheckbox() {
        composeTestRule.waitForNode(
            matcher = hasTestTag(SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG).and(isEnabled()),
            atLeastOneRootRequired = true,
        )
        composeTestRule.clickNode(
            matcher = hasTestTag(SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG),
            scrollBehavior = ScrollBehavior.Required,
        )
        composeTestRule.waitForIdle()
    }

    fun assertNoSetAsDefaultCheckbox() {
        composeTestRule.onNodeWithTag(SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG)
            .assertDoesNotExist()
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

    fun clickOnLpm(code: String, forVerticalMode: Boolean = false) {
        composeTestRule.waitForIdle()
        waitUntilVisible()

        if (forVerticalMode) {
            composeTestRule.waitForNode(
                matcher = hasTestTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT),
                atLeastOneRootRequired = true,
            )

            composeTestRule.clickNode(
                matcher = hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$code"),
                scrollBehavior = ScrollBehavior.Required,
            )
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

    private companion object {
        const val PAYMENT_SHEET_TEXT_WAIT_TIMEOUT_MILLIS = 10_000L
    }
}
