package com.stripe.android.paymentsheet

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.espresso.Espresso
import com.stripe.android.customersheet.ui.CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG
import com.stripe.android.customersheet.ui.CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_MODIFY_BADGE
import com.stripe.android.paymentsheet.ui.UPDATE_PM_REMOVE_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.readNumbersAsIndividualDigits
import com.stripe.android.testing.ScrollBehavior
import com.stripe.android.testing.clickNode
import com.stripe.android.testing.fillCardDetails
import com.stripe.android.testing.replaceText
import com.stripe.android.testing.waitForNode
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON

internal class CustomerSheetPage(
    private val composeTestRule: ComposeTestRule,
) {
    fun fillOutFullBillingAddress() {
        composeTestRule.replaceText("Address line 1", ADDRESS_LINE_ONE)
        composeTestRule.replaceText("Address line 2 (optional)", ADDRESS_LINE_TWO)
        composeTestRule.replaceText("City", CITY)

        composeTestRule.clickNode(
            matcher = hasText("State"),
            scrollBehavior = ScrollBehavior.Required,
        )
        composeTestRule.clickNode(
            matcher = hasText(STATE_NAME),
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun fillOutContactInformation() {
        composeTestRule.replaceText("Email", EMAIL)
        composeTestRule.replaceText("Phone number", PHONE_NUMBER)
    }

    fun fillOutName() {
        composeTestRule.replaceText("Name on card", NAME)
    }

    fun fillOutCardDetails(
        cardNumber: String = CARD_NUMBER,
    ) {
        composeTestRule.fillCardDetails(
            cardNumber = cardNumber,
            expirationDate = "$EXPIRY_MONTH/$${EXPIRY_YEAR.substring(startIndex = 2)}",
            cvc = CVC,
            zipCode = ZIP_CODE,
            textFieldScrollBehavior = ScrollBehavior.Required,
        )
    }

    fun selectCartesBancaire() {
        composeTestRule.waitForNode(
            matcher = hasContentDescription("Cartes Bancaires").and(isEnabled()),
            atLeastOneRootRequired = false,
        )
        composeTestRule.clickNode(
            matcher = hasContentDescription("Cartes Bancaires"),
            scrollBehavior = ScrollBehavior.Required,
        )
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

    fun clickEditButton() {
        val editButtonMatcher = hasTestTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG)

        composeTestRule.waitForNode(
            matcher = editButtonMatcher.and(isEnabled()),
            atLeastOneRootRequired = false,
        )
        composeTestRule.clickNode(
            matcher = editButtonMatcher,
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun clickModifyButton(forEndsWith: String) {
        val deleteBadgeForSavedPmMatcher = hasTestTag(TEST_TAG_MODIFY_BADGE)
            .and(hasContentDescription(forEndsWith.readNumbersAsIndividualDigits(), substring = true))

        composeTestRule.waitForNode(
            matcher = deleteBadgeForSavedPmMatcher.and(isEnabled()),
            atLeastOneRootRequired = false,
        )
        composeTestRule.clickNode(
            matcher = deleteBadgeForSavedPmMatcher,
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun clickDeleteButton() {
        val removeButtonMatch = hasTestTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG)

        composeTestRule.waitForNode(
            matcher = removeButtonMatch.and(isEnabled()),
            atLeastOneRootRequired = false,
        )
        composeTestRule.clickNode(
            matcher = removeButtonMatch,
            scrollBehavior = ScrollBehavior.Required,
        )
    }

    fun clickDialogRemoveButton() {
        val dialogRemoveButtonMatcher = hasTestTag(TEST_TAG_DIALOG_CONFIRM_BUTTON)

        composeTestRule.waitForNode(
            matcher = dialogRemoveButtonMatcher.and(isEnabled()),
            atLeastOneRootRequired = false,
        )
        composeTestRule.clickNode(
            matcher = dialogRemoveButtonMatcher,
            scrollBehavior = ScrollBehavior.Never,
        )
    }

    fun onSavedPaymentMethod(endsWith: String): SemanticsNodeInteraction {
        val savedPaymentMethodMatcher = getSavedPaymentMethodMatcher(endsWith = endsWith)

        composeTestRule.waitForNode(
            matcher = savedPaymentMethodMatcher,
            atLeastOneRootRequired = false,
        )
        return composeTestRule.onNode(savedPaymentMethodMatcher)
    }

    fun clickSavedPaymentMethod(endsWith: String) {
        val savedPaymentMethodMatcher = getSavedPaymentMethodMatcher(endsWith = endsWith)

        composeTestRule.waitForNode(
            matcher = savedPaymentMethodMatcher.and(isEnabled()),
            atLeastOneRootRequired = false,
        )
        composeTestRule.clickNode(
            matcher = savedPaymentMethodMatcher,
            scrollBehavior = ScrollBehavior.Required,
        )

        composeTestRule.waitForNode(
            matcher = savedPaymentMethodMatcher.and(isSelected()),
            atLeastOneRootRequired = false,
        )
    }

    private fun getSavedPaymentMethodMatcher(endsWith: String): SemanticsMatcher {
        return hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG)
            .and(hasText(endsWith, substring = true))
    }

    private fun clickPrimaryButton(tag: String) {
        composeTestRule.waitForNode(
            matcher = hasTestTag(tag).and(isEnabled()),
            atLeastOneRootRequired = false,
        )
        composeTestRule.clickNode(
            matcher = hasTestTag(tag),
            scrollBehavior = ScrollBehavior.Required,
        )
        composeTestRule.waitForIdle()
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
