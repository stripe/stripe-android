package com.stripe.android.test.core

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddress
import com.stripe.android.paymentsheet.example.playground.settings.DefaultBillingAddressSettingsDefinition
import com.stripe.android.test.core.ui.Selectors
import com.stripe.android.ui.core.elements.TranslationId
import com.stripe.android.ui.core.elements.formatExpirationDateForAccessibility
import com.stripe.android.uicore.utils.asIndividualDigits
import com.stripe.android.core.R as CoreR

internal class FieldPopulator(
    private val selectors: Selectors,
    private val testParameters: TestParameters,
    private val populateCustomLpmFields: FieldPopulator.() -> Unit,
    private val verifyCustomLpmFields: FieldPopulator.() -> Unit,
    private val values: Values,
) {
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
        selectors.formElement.waitFor()

        populateCustomLpmFields()

        selectors.composeTestRule.waitForIdle()
        Espresso.onIdle()

        Espresso.closeSoftKeyboard()

        Espresso.onIdle()

        if (testParameters.playgroundSettingsSnapshot.configurationData.integrationType.isPaymentFlow()) {
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
        val phoneNumber: String = "4021234567",
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
        if (!defaultBillingAddress) {
            selectors.getEmail()
                .ifExistsAssertContentDescriptionEquals(values.email)

            selectors.getName(selectors.getResourceString(TranslationId.AddressName.resourceId))
                .ifExistsAssertContentDescriptionEquals(values.name)

            selectors.getLine1()
                .ifExistsAssertContentDescriptionEquals(values.line1)
            selectors.getCity()
                .ifExistsAssertContentDescriptionEquals(values.city)
            validateZip()
            if (usesStateDropdown()) {
                selectors.getState()
                    .assertTextContains(values.state)
            } else {
                selectors.getState()
                    .ifExistsAssertContentDescriptionEquals(values.state)
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
                .ifExistsAssertContentDescriptionEquals(values.zip.asIndividualDigits())
        } else {
            selectors.getPostalCode()
                .ifExistsAssertContentDescriptionEquals(values.zip.asIndividualDigits())
        }
    }

    private val defaultBillingAddress: Boolean
        get() = testParameters.playgroundSettingsSnapshot[DefaultBillingAddressSettingsDefinition] != DefaultBillingAddress.Off

    private val merchantCountryCode: String
        get() = testParameters.playgroundSettingsSnapshot[CountrySettingsDefinition].value

    private fun usesStateDropdown(): Boolean {
        return merchantCountryCode in setOf("US", "GB")
    }

    private fun usesZip(): Boolean {
        return merchantCountryCode in setOf("US", "GB")
    }

    fun populateCardDetails() {
        selectors.getCardNumber().performTextInput(values.cardNumber)
        selectors.composeTestRule.waitForIdle()
        selectors.getCardExpiration()
            .performTextInput(values.cardExpiration)
        selectors.getCardCvc().apply {
            performTextInput(values.cardCvc)
        }

        if (!defaultBillingAddress) {
            populateZip()
        }
    }

    fun populateName(
        labelText: String = selectors.getResourceString(TranslationId.AddressName.resourceId)
    ) {
        selectors.getName(labelText).apply {
            performTextInput(values.name)
        }
    }

    fun populateEmail() {
        selectors.getEmail().apply {
            performClick()
            performTextInput(values.email)
        }
    }

    fun populateAddress() {
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

    fun populatePhoneNumber(
        labelText: String = selectors.getResourceString(CoreR.string.stripe_address_label_phone_number)
    ) {
        selectors.getPhoneNumber(labelText)
            .performTextInput(values.phoneNumber)
    }

    fun populateBoletoTaxId() {
        selectors.getBoletoTaxId().apply {
            performScrollTo()
            performTextInput(values.boletoTaxId)
        }
    }

    fun populateAuBecs() {
        selectors.getAuBsb().apply {
            performTextInput(values.auBecsBsbNumber)
        }
        selectors.getAuAccountNumber().apply {
            performTextInput(values.auBecsAccountNumber)
        }
    }

    fun populateBacs() {
        selectors.getBacsSortCode()
            .performScrollTo()
            .performTextInput(values.bacsSortCode)

        selectors.composeTestRule.waitForIdle()

        selectors.getBacsAccountNumber()
            .performScrollTo()
            .performTextInput(values.bacsAccountNumber)

        selectors.composeTestRule.waitForIdle()

        Espresso.closeSoftKeyboard()
        Espresso.onIdle()

        selectors.getBacsConfirmed()
            .performScrollTo()
            .performClick()
    }

    fun verifyAuBecs() {
        selectors.getAuBsb()
            .ifExistsAssertContentDescriptionEquals(values.auBecsBsbNumber)
        selectors.getAuAccountNumber()
            .ifExistsAssertContentDescriptionEquals(values.auBecsAccountNumber)
    }

    fun verifyCard() {
        val accessibleCardNumber = values.cardNumber.toCharArray().joinToString(" ")
        val accessibleCvc = values.cardCvc.toCharArray().joinToString(" ")
        val accessibleExpiryDate = formatExpirationDateForAccessibility(values.cardExpiration)
            .resolve(InstrumentationRegistry.getInstrumentation().targetContext)

        selectors.getCardNumber()
            .ifExistsAssertContentDescriptionEquals(accessibleCardNumber)
        selectors.getCardExpiration()
            .ifExistsAssertContentDescriptionEquals(accessibleExpiryDate)
        selectors.getCardCvc()
            .ifExistsAssertContentDescriptionEquals(accessibleCvc)
    }

    fun verifyMandateFieldExists() {
        selectors.mandateText.assertIsDisplayed()
    }

    fun verifyMandateFieldDoesNotExists() {
        selectors.mandateText.assertDoesNotExist()
    }

    private fun SemanticsNodeInteraction.ifExistsAssertContentDescriptionEquals(contentDescription: String) {
        ifExistsAssert { assertContentDescriptionEquals(contentDescription) }
    }

    private fun SemanticsNodeInteraction.ifExistsAssert(assertion: SemanticsNodeInteraction.() -> Unit) {
        val performAssert = try {
            assertExists()
            true
        } catch (_: AssertionError) {
            // Expected
            false
        }
        if (performAssert) {
            assertion()
        }
    }
}
