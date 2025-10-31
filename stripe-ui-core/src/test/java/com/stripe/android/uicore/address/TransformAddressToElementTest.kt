package com.stripe.android.uicore.address

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import com.stripe.android.uicore.elements.AdministrativeAreaConfig
import com.stripe.android.uicore.elements.AdministrativeAreaElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionSingleFieldElement
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

    @Test
    fun `Simple text elements in address schemas filter out emojis`() = runBlocking {
        val simpleTextList = AddressSchemaRegistry.get("US")!!.transformToElementList("US")

        // Check address line 1
        val addressLine1Element = simpleTextList[0] as SectionSingleFieldElement
        val addressLine1Controller = addressLine1Element.controller as SimpleTextFieldController
        assertThat(addressLine1Controller.textFieldConfig.filter("123 Main St 🏠")).isEqualTo("123 Main St ")

        // Check address line 2
        val addressLine2Element = simpleTextList[1] as SectionSingleFieldElement
        val addressLine2Controller = addressLine2Element.controller as SimpleTextFieldController
        assertThat(addressLine2Controller.textFieldConfig.filter("Apt 5 😊")).isEqualTo("Apt 5 ")

        // Check city (inside row)
        val cityZipRow = simpleTextList[2] as RowElement
        val cityElement = cityZipRow.fields[0]
        val cityController = cityElement.controller as SimpleTextFieldController
        assertThat(cityController.textFieldConfig.filter("New York 🗽")).isEqualTo("New York ")
    }

    @Test
    fun `Address elements in different countries filter emojis`() = runBlocking {
        // Test Canada
        val canadaSchema = AddressSchemaRegistry.get("CA")!!.transformToElementList("CA")
        val canadaAddressLine1 = canadaSchema[0] as SectionSingleFieldElement
        val canadaController = canadaAddressLine1.controller as SimpleTextFieldController
        assertThat(canadaController.textFieldConfig.filter("Main Street 🍁")).isEqualTo("Main Street ")

        // Test UK
        val ukSchema = AddressSchemaRegistry.get("GB")!!.transformToElementList("GB")
        val ukAddressLine1 = ukSchema[0] as SectionSingleFieldElement
        val ukController = ukAddressLine1.controller as SimpleTextFieldController
        assertThat(ukController.textFieldConfig.filter("High Street 🏰")).isEqualTo("High Street ")
    }

    @Test
    fun `Address elements preserve numbers while filtering emojis`() = runBlocking {
        val simpleTextList = AddressSchemaRegistry.get("US")!!.transformToElementList("US")

        val addressLine1Element = simpleTextList[0] as SectionSingleFieldElement
        val addressLine1Controller = addressLine1Element.controller as SimpleTextFieldController

        // Numbers should be preserved
        assertThat(addressLine1Controller.textFieldConfig.filter("123 Main St")).isEqualTo("123 Main St")
        assertThat(addressLine1Controller.textFieldConfig.filter("456 Oak Ave #789")).isEqualTo("456 Oak Ave #789")

        // But emojis should be removed
        assertThat(addressLine1Controller.textFieldConfig.filter("123 😀 Main St")).isEqualTo("123  Main St")
    }
}
