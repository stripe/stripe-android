package com.stripe.android.test.core

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.test.core.ui.Selectors
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.BacsDebitBankAccountSpec
import com.stripe.android.ui.core.elements.BacsDebitConfirmSpec
import com.stripe.android.ui.core.elements.BoletoTaxIdSpec
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.DropdownSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.KlarnaCountrySpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec

internal class FieldPopulator(
    private val selectors: Selectors,
    private val testParameters: TestParameters,
    private val populateCustomLpmFields: () -> Unit,
    private val verifyCustomLpmFields: () -> Unit = {},
    private val values: Values,
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

        selectors.composeTestRule.waitForIdle()
        Espresso.onIdle()

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
        val state: String = "California",
        val cardNumber: String = "4242424242424242",
        val cardExpiration: String = "1230",
        val cardCvc: String = "321",
        val auBecsBsbNumber: String = "000000",
        val auBecsAccountNumber: String = "000123456",
        val bacsSortCode: String = "108800",
        val bacsAccountNumber: String = "00012345",
        val boletoTaxId: String = "00000000000",
    )

    private fun verifyPlatformLpmFields() {
        formSpec.items.forEach {
            when (it) {
                is BsbSpec -> {
                    selectors.getAuBsb()
                        .assertContentDescriptionEquals(values.auBecsBsbNumber)
                }
                is CardDetailsSectionSpec -> {
                    val accessibleCardNumber = values.cardNumber.toCharArray().joinToString(" ")
                    val accessibleCvc = values.cardCvc.toCharArray().joinToString(" ")

                    selectors.getCardNumber()
                        .assertContentDescriptionEquals(accessibleCardNumber)
                    selectors.getCardExpiration()
                        .assertContentDescriptionEquals(values.cardExpiration)
                    selectors.getCardCvc()
                        .assertContentDescriptionEquals(accessibleCvc)
                }
                is EmailSpec -> {
                    if (!defaultBillingAddress) {
                        selectors.getEmail()
                            .assertContentDescriptionEquals(values.email)
                    }
                }
                is NameSpec -> {
                    if (!defaultBillingAddress) {
                        selectors.getName(it.labelTranslationId.resourceId)
                            .assertContentDescriptionEquals(values.name)
                    }
                }
                is AddressSpec -> {
                    if (!defaultBillingAddress) {
                        selectors.getLine1()
                            .assertContentDescriptionEquals(values.line1)
                        selectors.getCity()
                            .assertContentDescriptionEquals(values.city)

                        validateZip()

                        if (usesStateDropdown()) {
                            selectors.getState()
                                .assertTextContains(values.state)
                        } else {
                            selectors.getState()
                                .assertContentDescriptionEquals(values.state)
                        }
                    }
                }
                is CountrySpec -> {}
                is SimpleTextSpec -> {}
                is AuBankAccountNumberSpec -> {
                    selectors.getAuAccountNumber()
                        .assertContentDescriptionEquals(values.auBecsAccountNumber)
                }
                is BacsDebitBankAccountSpec -> {
                    selectors.getBacsAccountNumber()
                        .assertContentDescriptionEquals(values.bacsAccountNumber)
                    selectors.getBacsSortCode()
                        .assertContentDescriptionEquals(values.bacsSortCode)
                }
                is BacsDebitConfirmSpec -> {
                    selectors.getBacsConfirmed()
                        .assertIsOn()
                }
                is DropdownSpec -> {}
                is IbanSpec -> {}
                is KlarnaCountrySpec -> {}
                is CardBillingSpec -> {
                    if (!defaultBillingAddress) {
                        validateZip()
                    }
                }
                else -> {}
            }
        }
    }

    private fun populatePlatformLpmFields() {
        formSpec.items.forEach {
            when (it) {
                is BsbSpec -> {
                    selectors.getAuBsb().apply {
                        performTextInput(values.auBecsBsbNumber)
                    }
                }
                is BoletoTaxIdSpec -> {
                    selectors.getBoletoTaxId().apply {
                        performScrollTo()
                        performTextInput(values.boletoTaxId)
                    }
                }
                is CardDetailsSectionSpec -> {
                    selectors.getCardNumber().performTextInput(values.cardNumber)
                    selectors.composeTestRule.waitForIdle()
                    selectors.getCardExpiration()
                        .performTextInput(values.cardExpiration)
                    selectors.getCardCvc().apply {
                        performTextInput(values.cardCvc)
                    }
                }
                is EmailSpec -> {
                    if (!defaultBillingAddress) {
                        selectors.getEmail().apply {
                            performClick()
                            performTextInput(values.email)
                        }
                    }
                }
                is NameSpec -> {
                    if (!defaultBillingAddress) {
                        selectors.getName(it.labelTranslationId.resourceId).apply {
                            performTextInput(values.name)
                        }
                    }
                }
                is AuBankAccountNumberSpec -> {
                    selectors.getAuAccountNumber().apply {
                        performTextInput(values.auBecsAccountNumber)
                    }
                }
                is BacsDebitBankAccountSpec -> {
                    selectors.getBacsAccountNumber()
                        .performTextInput(values.bacsAccountNumber)
                    selectors.getBacsSortCode()
                        .performTextInput(values.bacsSortCode)
                }
                is BacsDebitConfirmSpec -> {
                    selectors.getBacsConfirmed()
                        .performScrollTo()
                        .performClick()
                }
                is IbanSpec -> {}
                is KlarnaCountrySpec -> {}
                is CardBillingSpec -> {
                    if (!defaultBillingAddress) {
                        populateZip()
                    }
                }
                is AddressSpec -> {
                    if (!defaultBillingAddress) {
                        selectors
                            .getLine1()
                            .performScrollTo()
                            .performClick()
                            .performTextInput(values.line1)

                        selectors
                            .getCity()
                            .performScrollTo()
                            .performClick()
                            .performTextInput(values.city)

                        populateZip()

                        if (usesStateDropdown()) {
                            selectors
                                .getState()
                                .performScrollTo()
                                .performClick()

                            selectors.selectState(values.state)
                        } else {
                            selectors
                                .getState()
                                .performScrollTo()
                                .performClick()
                                .performTextInput(values.state)
                        }
                    }
                }
                is CountrySpec -> {}
                is SimpleTextSpec -> {}
                is DropdownSpec -> {}
                else -> {}
            }
        }
    }

    private fun populateZip() {
        if (usesZip()) {
            selectors.getZip().apply {
                performScrollTo()
                performTextInput(values.zip)
            }
        } else {
            selectors.getPostalCode().apply {
                performScrollTo()
                performTextInput(values.zip)
            }
        }
    }

    private fun validateZip() {
        if (usesZip()) {
            selectors.getZip()
                .assertContentDescriptionEquals(values.zip)
        } else {
            selectors.getPostalCode()
                .assertContentDescriptionEquals(values.zip)
        }
    }

    private val defaultBillingAddress: Boolean
        get() = testParameters.playgroundSettingsSnapshot[DefaultBillingAddressSettingsDefinition]

    private val merchantCountryCode: String
        get() = testParameters.playgroundSettingsSnapshot[CountrySettingsDefinition].value

    private fun usesStateDropdown(): Boolean {
        return merchantCountryCode in setOf("US", "GB")
    }

    private fun usesZip(): Boolean {
        return merchantCountryCode in setOf("US", "GB")
    }
}
