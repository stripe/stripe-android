package com.stripe.android.ui.core.elements

import android.app.Application
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SimpleTextFieldController
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CardBillingAddressElementTest {
    private val addressRepository = AddressRepository(
        ApplicationProvider.getApplicationContext<Application>().resources
    )
    val dropdownFieldController = DropdownFieldController(
        CountryConfig(emptySet())
    )
    val cardBillingElement = CardBillingAddressElement(
        IdentifierSpec.Generic("billing_element"),
        rawValuesMap = emptyMap(),
        addressRepository,
        emptySet(),
        dropdownFieldController,
        null,
        null
    )

    init {
        // We want to use fields that are easy to set in error
        addressRepository.add(
            "US",
            listOf(
                EmailElement(
                    IdentifierSpec.Email,
                    controller = SimpleTextFieldController(EmailConfig())
                )
            )
        )
        addressRepository.add(
            "JP",
            listOf(
                IbanElement(
                    IdentifierSpec.Generic("sepa_debit[iban]"),
                    SimpleTextFieldController(IbanConfig())
                )
            )
        )
    }

    @Test
    fun `Verify that when US is selected postal is not hidden`() {
        val hiddenIdFlowValues = mutableListOf<Set<IdentifierSpec>>()
        cardBillingElement.hiddenIdentifiers.asLiveData()
            .observeForever {
                hiddenIdFlowValues.add(it)
            }

        dropdownFieldController.onRawValueChange("US")
        verifyPostalShown(hiddenIdFlowValues[0])
    }

    @Test
    fun `Verify that when GB is selected postal is not hidden`() {
        val hiddenIdFlowValues = mutableListOf<Set<IdentifierSpec>>()
        cardBillingElement.hiddenIdentifiers.asLiveData()
            .observeForever {
                hiddenIdFlowValues.add(it)
            }

        dropdownFieldController.onRawValueChange("GB")
        verifyPostalShown(hiddenIdFlowValues[1])
    }

    @Test
    fun `Verify that when CA is selected postal is not hidden`() {
        val hiddenIdFlowValues = mutableListOf<Set<IdentifierSpec>>()
        cardBillingElement.hiddenIdentifiers.asLiveData()
            .observeForever {
                hiddenIdFlowValues.add(it)
            }

        dropdownFieldController.onRawValueChange("CA")
        verifyPostalShown(hiddenIdFlowValues[0])
    }

    @Test
    fun `Verify that when DE is selected postal IS hidden`() {
        val hiddenIdFlowValues = mutableListOf<Set<IdentifierSpec>>()
        cardBillingElement.hiddenIdentifiers.asLiveData()
            .observeForever {
                hiddenIdFlowValues.add(it)
            }

        dropdownFieldController.onRawValueChange("DE")
        verifyPostalHidden(hiddenIdFlowValues[1])
    }

    fun verifyPostalShown(hiddenIdentifiers: Set<IdentifierSpec>) {
        Truth.assertThat(hiddenIdentifiers).doesNotContain(IdentifierSpec.PostalCode)
        Truth.assertThat(hiddenIdentifiers).doesNotContain(IdentifierSpec.Country)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.Line1)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.Line2)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.State)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.City)
    }

    fun verifyPostalHidden(hiddenIdentifiers: Set<IdentifierSpec>) {
        Truth.assertThat(hiddenIdentifiers).doesNotContain(IdentifierSpec.Country)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.PostalCode)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.Line1)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.Line2)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.State)
        Truth.assertThat(hiddenIdentifiers).contains(IdentifierSpec.City)
    }
}
