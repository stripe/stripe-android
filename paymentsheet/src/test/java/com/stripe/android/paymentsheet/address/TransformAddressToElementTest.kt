package com.stripe.android.paymentsheet.address

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository.Companion.supportedCountries
import com.stripe.android.paymentsheet.elements.SectionSingleFieldElement
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec
import org.junit.Test
import java.io.File
import java.security.InvalidParameterException

class TransformAddressToElementTest {

    @Test
    fun `Read US Json`() {
        val addressSchema = readFile("src/main/assets/addressinfo/US.json")!!
        val simpleTextList = addressSchema.transformToElementList()

        val addressLine1 = SimpleTextSpec(
            IdentifierSpec.Line1,
            R.string.address_label_address_line1,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = false
        )

        val addressLine2 = SimpleTextSpec(
            IdentifierSpec.Line2,
            R.string.address_label_address_line2,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = true
        )

        val city = SimpleTextSpec(
            IdentifierSpec.City,
            R.string.address_label_city,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = false
        )

        val state = SimpleTextSpec(
            IdentifierSpec.State,
            R.string.address_label_state,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = false
        )

        val zip = SimpleTextSpec(
            IdentifierSpec.PostalCode,
            R.string.address_label_zip_code,
            KeyboardCapitalization.None,
            KeyboardType.Number,
            showOptionalLabel = false
        )

        assertThat(simpleTextList.size).isEqualTo(5)
        verifySimpleTextSpecInTextFieldController(simpleTextList[0], addressLine1)
        verifySimpleTextSpecInTextFieldController(simpleTextList[1], addressLine2)
        verifySimpleTextSpecInTextFieldController(simpleTextList[2], city)
        verifySimpleTextSpecInTextFieldController(simpleTextList[3], zip)
        verifySimpleTextSpecInTextFieldController(simpleTextList[4], state)
    }

    private fun verifySimpleTextSpecInTextFieldController(
        textElement: SectionSingleFieldElement,
        simpleTextSpec: SimpleTextSpec
    ) {
        val actualController = textElement.controller as SimpleTextFieldController
        assertThat(actualController.capitalization).isEqualTo(
            simpleTextSpec.capitalization
        )
        assertThat(actualController.keyboardType).isEqualTo(
            simpleTextSpec.keyboardType
        )
        assertThat(actualController.label).isEqualTo(
            simpleTextSpec.label
        )
    }

    @Test
    fun `Make sure name schema is not found on fields not processed`() {
        supportedCountries.forEach { countryCode ->
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
    fun `Make sure all country code json files are serializable`() {
        supportedCountries.forEach { countryCode ->
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
