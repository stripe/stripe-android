package com.stripe.android.uicore.address

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.R
import com.stripe.android.uicore.address.AddressSchemaRepository.Companion.SUPPORTED_COUNTRIES
import com.stripe.android.uicore.elements.AdministrativeAreaConfig
import com.stripe.android.uicore.elements.AdministrativeAreaElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.TextFieldController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.security.InvalidParameterException
import com.stripe.android.core.R as CoreR

class TransformAddressToElementTest {
    private data class TestTextSpec(
        val apiPath: IdentifierSpec,
        val label: Int,
        val keyboardCapitalization: KeyboardCapitalization,
        val keyboardType: androidx.compose.ui.text.input.KeyboardType,
        val showOptionalLabel: Boolean
    )

    @Test
    fun `Read US Json`() = runBlocking {
        val addressSchema = readFile("src/main/assets/addressinfo/US.json")!!
        val simpleTextList = addressSchema.transformToElementList("US")

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
            CoreR.string.stripe_address_label_state
        )
    }

    private suspend fun verifySimpleTextSpecInTextFieldController(
        textElement: SectionSingleFieldElement,
        simpleTextSpec: TestTextSpec
    ) {
        val actualController = textElement.controller as TextFieldController
        assertThat(actualController.capitalization).isEqualTo(simpleTextSpec.keyboardCapitalization)
        assertThat(actualController.keyboardType).isEqualTo(simpleTextSpec.keyboardType)
        assertThat(actualController.label.first()).isEqualTo(
            simpleTextSpec.label
        )
    }

    @Test
    fun `Make sure name schema is not found on fields not processed`() {
        SUPPORTED_COUNTRIES.forEach { countryCode ->
            val schemaList = readFile("src/main/assets/addressinfo/$countryCode.json")
            val invalidNameType = schemaList?.filter { addressSchema ->
                addressSchema.schema?.nameType != null
            }
                ?.filter {
                    it.type == FieldType.AddressLine1 &&
                        it.type == FieldType.AddressLine2 &&
                        it.type == FieldType.Locality
                }
            invalidNameType?.forEach { println(it.type?.name) }
            assertThat(invalidNameType).isEmpty()
        }
    }

    @Test
    fun `Make sure sorting code and dependent locality is never required`() {
        // Sorting code and dependent locality are not actually sent to the server.
        SUPPORTED_COUNTRIES.forEach { countryCode ->
            val schemaList = readFile("src/main/assets/addressinfo/$countryCode.json")
            val invalidNameType = schemaList?.filter { addressSchema ->
                addressSchema.required &&
                    (
                        addressSchema.type == FieldType.SortingCode ||
                            addressSchema.type == FieldType.DependentLocality
                        )
            }
            invalidNameType?.forEach { println(it.type?.name) }
            assertThat(invalidNameType).isEmpty()
        }
    }

    @Test
    fun `Make sure all country code json files are serializable`() {
        SUPPORTED_COUNTRIES.forEach { countryCode ->
            val schemaList = readFile("src/main/assets/addressinfo/$countryCode.json")
            schemaList?.filter { addressSchema ->
                addressSchema.schema?.nameType != null
            }
                ?.filter {
                    it.type == FieldType.AddressLine1 &&
                        it.type == FieldType.AddressLine2 &&
                        it.type == FieldType.Locality
                }
                ?.forEach { println(it.type?.name) }
        }
    }

    private fun readFile(filename: String): List<CountryAddressSchema>? {
        val file = File(filename)

        if (file.exists()) {
            return parseAddressesSchema(file.inputStream())
        } else {
            throw InvalidParameterException("Error could not find the test files.")
        }
    }
}
