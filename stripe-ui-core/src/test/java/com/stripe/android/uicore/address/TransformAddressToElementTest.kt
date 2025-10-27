package com.stripe.android.uicore.address

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.elements.AddressTextFilter
import com.stripe.android.uicore.elements.AdministrativeAreaConfig
import com.stripe.android.uicore.elements.AdministrativeAreaElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import com.stripe.android.core.R as CoreR

class TransformAddressToElementTest {
    private data class TestTextSpec(
        val apiPath: IdentifierSpec,
        val label: Int,
        val keyboardCapitalization: KeyboardCapitalization,
        val keyboardType: KeyboardType,
        val showOptionalLabel: Boolean
    )

    @Test
    fun `Read US schema`() = runBlocking {
        val simpleTextList = AddressSchemaRegistry.get("US")!!.transformToElementList("US")

        val addressLine1 = TestTextSpec(
            IdentifierSpec.Line1,
            CoreR.string.stripe_address_label_address_line1,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = false
        )

        val addressLine2 = TestTextSpec(
            IdentifierSpec.Line2,
            R.string.stripe_address_label_address_line2,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = true
        )

        val city = TestTextSpec(
            IdentifierSpec.City,
            CoreR.string.stripe_address_label_city,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = false
        )

        val zip = TestTextSpec(
            IdentifierSpec.PostalCode,
            CoreR.string.stripe_address_label_zip_code,
            KeyboardCapitalization.None,
            KeyboardType.NumberPassword,
            showOptionalLabel = false
        )

        assertThat(simpleTextList.size).isEqualTo(4)
        verifySimpleTextSpecInTextFieldController(
            simpleTextList[0] as SectionSingleFieldElement,
            addressLine1
        )
        verifySimpleTextSpecInTextFieldController(
            simpleTextList[1] as SectionSingleFieldElement,
            addressLine2
        )
        val cityZipRow = simpleTextList[2] as RowElement
        verifySimpleTextSpecInTextFieldController(
            cityZipRow.fields[0],
            city
        )
        verifySimpleTextSpecInTextFieldController(
            cityZipRow.fields[1],
            zip
        )

        // US has state dropdown
        val stateDropdownElement = simpleTextList[3] as AdministrativeAreaElement
        val stateDropdownController = stateDropdownElement.controller
        assertThat(stateDropdownController.displayItems).isEqualTo(
            AdministrativeAreaConfig.Country.US().administrativeAreas.map { it.second }
        )
        assertThat(stateDropdownController.label.first()).isEqualTo(
            resolvableString(CoreR.string.stripe_address_label_state)
        )
    }

    @Test
    fun `Make sure name schema is not found on fields not processed`() {
        AddressSchemaRegistry.all.forEach { (_, schema) ->
            val invalidNameType = schema.schemaElements().filter { addressSchema ->
                addressSchema.schema?.nameType != null
            }.filter {
                it.type == FieldType.AddressLine1 || it.type == FieldType.AddressLine2
            }

            invalidNameType.forEach { println(it.type?.name) }
            assertThat(invalidNameType).isEmpty()
        }
    }

    @Test
    fun `Make sure sorting code and dependent locality is never required`() {
        // Sorting code and dependent locality are not actually sent to the server.
        AddressSchemaRegistry.all.forEach { (_, schema) ->
            val invalidNameType = schema.schemaElements().filter { addressSchema ->
                addressSchema.required && (
                    addressSchema.type == FieldType.SortingCode ||
                        addressSchema.type == FieldType.DependentLocality
                    )
            }
            invalidNameType.forEach { println(it.type?.name) }
            assertThat(invalidNameType).isEmpty()
        }
    }

    @Test
    fun `Address text fields have AddressTextFilter configured`() = runBlocking {
        val usElements = AddressSchemaRegistry.get("US")!!.transformToElementList("US")

        // Check that address line 1 has AddressTextFilter
        val addressLine1Element = usElements[0] as SectionSingleFieldElement
        val addressLine1Controller = addressLine1Element.controller as SimpleTextFieldController
        val addressLine1Config = addressLine1Controller.textFieldConfig as SimpleTextFieldConfig

        assertThat(addressLine1Config.textFilter).isInstanceOf(AddressTextFilter::class.java)

        // Check that address line 2 has AddressTextFilter
        val addressLine2Element = usElements[1] as SectionSingleFieldElement
        val addressLine2Controller = addressLine2Element.controller as SimpleTextFieldController
        val addressLine2Config = addressLine2Controller.textFieldConfig as SimpleTextFieldConfig

        assertThat(addressLine2Config.textFilter).isInstanceOf(AddressTextFilter::class.java)

        // Check that city has AddressTextFilter
        val cityElement = (usElements[2] as RowElement).fields[0]
        val cityController = cityElement.controller as SimpleTextFieldController
        val cityConfig = cityController.textFieldConfig as SimpleTextFieldConfig

        assertThat(cityConfig.textFilter).isInstanceOf(AddressTextFilter::class.java)
    }

    private suspend fun verifySimpleTextSpecInTextFieldController(
        textElement: SectionSingleFieldElement,
        simpleTextSpec: TestTextSpec
    ) {
        val actualController = textElement.controller as TextFieldController
        assertThat(actualController.capitalization).isEqualTo(simpleTextSpec.keyboardCapitalization)
        assertThat(actualController.keyboardType).isEqualTo(simpleTextSpec.keyboardType)
        assertThat(actualController.label.first()).isEqualTo(
            resolvableString(simpleTextSpec.label)
        )
    }
}
