package com.stripe.android.test.core

import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.BankDropdownSpec
import com.stripe.android.ui.core.elements.BsbSpec
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

        // TODO: This is not working as expected
        Truth.assertThat(testParameters.saveForFutureUseCheckboxVisible == SaveForFutureCheckbox.exists())
            .isTrue()
        if (SaveForFutureCheckbox.exists()) {
            if (!testParameters.saveCheckboxValue) {
                SaveForFutureCheckbox.click()
            }
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
                                        selectors.getName()
                                            .performTextInput("Jenny Rosen")
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
                                BsbSpec -> {}
                                IbanSpec -> {}
                                is KlarnaCountrySpec -> {}
                            }
                        }
                    }
                }
                else -> {}
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
