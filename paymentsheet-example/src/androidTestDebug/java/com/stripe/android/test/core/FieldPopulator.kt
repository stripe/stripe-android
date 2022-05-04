package com.stripe.android.test.core

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.stripe.android.test.core.ui.Selectors
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.BankDropdownSpec
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
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
    private val populateCustomLpmFields: () -> Unit,
    private val verifyCustomLpmFields: () -> Unit = {}
) {
    private val formSpec = testParameters.paymentMethod.formSpec

    fun verifyFields() {
        // Need to verify the value of save for future usage
        if (testParameters.saveForFutureUseCheckboxVisible) {
            selectors.saveForFutureCheckbox.assertExists()
            if (testParameters.saveCheckboxValue) {
                selectors.saveForFutureCheckbox.assertIsOn()
            } else {
                selectors.saveForFutureCheckbox.assertIsOff()
            }
        } else {
            selectors.saveForFutureCheckbox.assertDoesNotExist()
        }

        verifyPlatformLpmFields()
        verifyCustomLpmFields()
    }

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

    data class Values(
        val name: String = "Jenny Rosen",
        val email: String = "jrosen@email.com",
        val line1: String = "123 Main Street",
        val city: String = "San Francisco",
        val zip: String = "12345",
        val state: String = "CA",
        val cardNumber: String = "4242424242424242",
        val cardExpiration: String = "1230",
        val cardCvc: String = "321"
    )

    private fun verifyPlatformLpmFields(values: Values = Values()) {
        formSpec.items.forEach {
            when (it) {
                is CardDetailsSectionSpec -> {
                    selectors.getCardNumber()
                        .assertContentDescriptionEquals(values.cardNumber)
                    selectors.getCardExpiration()
                        .assertContentDescriptionEquals(values.cardExpiration)
                    selectors.getCardCvc()
                        .assertContentDescriptionEquals(
                            values.cardCvc.replace("\\d".toRegex(), "$0 ")
                        )
                }
                is SectionSpec -> {
                    if (!expectFieldToBeHidden(testParameters.saveCheckboxValue, it)) {
                        it.fields.forEach { sectionField ->
                            when (sectionField) {
                                is EmailSpec -> {
                                    if (testParameters.billing == Billing.Off) {
                                        selectors.getEmail()
                                            .assertContentDescriptionEquals(values.email)
                                    }
                                }
                                SimpleTextSpec.NAME -> {
                                    if (testParameters.billing == Billing.Off) {
                                        selectors.getName()
                                            .assertContentDescriptionEquals(values.name)
                                    }
                                }
                                is AddressSpec -> {
                                    if (testParameters.billing == Billing.Off) {
                                        // TODO: This will not work when other countries are selected or defaulted
                                        selectors.getLine1()
                                            .assertContentDescriptionEquals(values.line1)
                                        selectors.getCity()
                                            .assertContentDescriptionEquals(values.city)
                                        selectors.getZip()
                                            .assertContentDescriptionEquals(values.zip)
                                        selectors.getState()
                                            .assertContentDescriptionEquals(values.state)
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
                                            .assertContentDescriptionEquals(values.zip)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun populatePlatformLpmFields(values: Values = Values()) {
        formSpec.items.forEach {
            when (it) {
                is CardDetailsSectionSpec -> {
                    selectors.getCardNumber().performTextInput(values.cardNumber)
                    selectors.composeTestRule.waitForIdle()
                    selectors.getCardExpiration()
                        .performTextInput(values.cardExpiration)
                    selectors.getCardCvc().apply {
                        performTextInput(values.cardCvc)

                    }
                }
                is SectionSpec -> {
                    if (!expectFieldToBeHidden(testParameters.saveCheckboxValue, it)) {
                        it.fields.forEach { sectionField ->
                            when (sectionField) {
                                is EmailSpec -> {
                                    if (testParameters.billing == Billing.Off) {
                                        selectors.getEmail().apply {
                                            performClick()
                                            performTextInput(values.email)
                                        }
                                    }
                                }
                                SimpleTextSpec.NAME -> {
                                    if (testParameters.billing == Billing.Off) {
                                        selectors.getName().apply {
                                            performTextInput(values.name)

                                        }
                                    }
                                }
                                is AddressSpec -> {
                                    if (testParameters.billing == Billing.Off) {
                                        // TODO: This will not work when other countries are selected or defaulted
                                        selectors.getLine1().apply {
                                            performClick()
                                            performTextInput(values.line1)
                                        }
                                        selectors.getCity()
                                            .performTextInput(values.city)
                                        selectors.getZip()
                                            .performTextInput(values.zip)
                                        selectors.getState().apply {
                                            performTextInput(values.state)

                                        }
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
                                        selectors.getZip().apply {
                                            performTextInput(values.zip)
                                        }
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
                saveForFutureUseHidesIdentifier.api_path.v1
            }
            ?.firstOrNull { saveForFutureUseHidesIdentifier ->
                saveForFutureUseHidesIdentifier == section.api_path.v1
            } != null)

    }

}
