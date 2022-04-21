package com.stripe.android.test.core

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.stripe.android.test.core.ui.Selectors
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.BankDropdownSpec
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.KlarnaCountrySpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec

class FieldPopulator(
    private val selectors: Selectors,
    private val testParameters: TestParameters,
    private val populateCustomLpmFields: () -> Unit
) {
    private val formSpec = testParameters.paymentMethod.formSpec

    fun populateFields() {
        populatePlatformLpmFields()
        populateCustomLpmFields()

        Espresso.closeSoftKeyboard()

        if (testParameters.saveForFutureUseCheckboxVisible) {
            selectors.saveForFutureCheckbox.assertExists()
            if (testParameters.saveCheckboxValue) {
                if (!isSaveForFutureUseSelected()) {
                    selectors.saveForFutureCheckbox.performClick()
                }
            } else {
                if (isSaveForFutureUseSelected()) {
                    selectors.saveForFutureCheckbox.performClick()
                }
            }
        } else {
            selectors.saveForFutureCheckbox.assertDoesNotExist()
        }
    }

    private fun isSaveForFutureUseSelected(): Boolean {
        return try {
            selectors.saveForFutureCheckbox.assertIsToggleable()
            selectors.saveForFutureCheckbox.assertIsOn()
            true
        } catch (e: AssertionError) {
            false
        }
    }

    private fun populatePlatformLpmFields() {
        formSpec.items.forEach {
            when (it) {
                is SectionSpec -> {
                    if (!expectFieldToBeHidden(testParameters.saveCheckboxValue, it)) {
                        it.fields.forEach { sectionField ->
                            when (sectionField) {
                                is EmailSpec -> {
                                    if (testParameters.billing == Billing.Off) {
                                        selectors.getEmail()
                                            .performTextInput("jrosen@email.com")
                                    }
                                }
                                SimpleTextSpec.NAME -> {
                                    if (testParameters.billing == Billing.Off) {
                                        selectors.getName().apply {
                                            performTextInput("Jenny Rosen")
                                            performImeAction()
                                        }
                                    }
                                }
                                is AddressSpec -> {
                                    if (testParameters.billing == Billing.Off) {
                                        // TODO: This will not work when other countries are selected or defaulted
                                        selectors.getLine1()
                                            .performTextInput("123 Main Street")
                                        selectors.getCity()
                                            .performTextInput("Albany")
                                        selectors.getZip()
                                            .performTextInput("12345")
                                        selectors.getState()
                                            .performTextInput("NY")
                                    }
                                }
                                is CountrySpec -> {}
                                is SimpleTextSpec -> {}
                                AuBankAccountNumberSpec -> {}
                                is BankDropdownSpec -> {}
                                IbanSpec -> {}
                                is KlarnaCountrySpec -> {}
                                is CardBillingSpec -> {
                                    if (testParameters.billing == Billing.Off) {
                                        // TODO: This will not work when other countries are selected or defaulted
                                        selectors.getZip()
                                            .performTextInput("12345")
                                    }
                                }
                                CardDetailsSpec -> {
                                    selectors.getCardNumber().performTextInput("4242424242424242")
                                    selectors.composeTestRule.waitForIdle()
                                    selectors.getCardExpiration().performTextInput("1230")
                                    selectors.getCardCvc().apply{
                                        performTextInput("123")
                                        // TODO(MLB): Need to perform the right way so screenshot doesn't have the cursor
                                        performImeAction()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun expectFieldToBeHidden(
        saveCheckboxValue: Boolean,
        section: SectionSpec
    ): Boolean {
        val saveForFutureUseSpec = formSpec.items
            .mapNotNull { it as? SaveForFutureUseSpec }
            .firstOrNull()
        return (!saveCheckboxValue
            && saveForFutureUseSpec?.identifierRequiredForFutureUse
            ?.map { saveForFutureUseHidesIdentifier ->
                saveForFutureUseHidesIdentifier.identifier.value
            }
            ?.firstOrNull { saveForFutureUseHidesIdentifier ->
                saveForFutureUseHidesIdentifier == section.identifier.value
            } != null)

    }
}
