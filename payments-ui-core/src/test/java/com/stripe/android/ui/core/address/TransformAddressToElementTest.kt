package com.stripe.android.ui.core.address

import androidx.compose.ui.text.input.KeyboardCapitalization
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository.Companion.supportedCountries
import com.stripe.android.ui.core.elements.Capitalization
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.KeyboardType
import com.stripe.android.ui.core.elements.RowElement
import com.stripe.android.ui.core.elements.SectionSingleFieldElement
import com.stripe.android.ui.core.elements.SimpleTextSpec
import com.stripe.android.ui.core.elements.TextFieldController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.security.InvalidParameterException

class TransformAddressToElementTest {

    @Test
    fun `Read US Json`() = runBlocking {
        val addressSchema = readFile("src/main/assets/addressinfo/US.json")!!
        val simpleTextList = addressSchema.transformToElementList()

        val addressLine1 = SimpleTextSpec(
            IdentifierSpec.Line1,
            R.string.address_label_address_line1,
            Capitalization.words,
            KeyboardType.text,
            showOptionalLabel = false
        )

        val addressLine2 = SimpleTextSpec(
            IdentifierSpec.Line2,
            R.string.address_label_address_line2,
            Capitalization.words,
            KeyboardType.text,
            showOptionalLabel = true
        )

        val city = SimpleTextSpec(
            IdentifierSpec.City,
            R.string.address_label_city,
            Capitalization.words,
            KeyboardType.text,
            showOptionalLabel = false
        )

        val state = SimpleTextSpec(
            IdentifierSpec.State,
            R.string.address_label_state,
            Capitalization.words,
            KeyboardType.text,
            showOptionalLabel = false
        )

        val zip = SimpleTextSpec(
            IdentifierSpec.PostalCode,
            R.string.address_label_zip_code,
            Capitalization.none,
            KeyboardType.number_password,
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
        verifySimpleTextSpecInTextFieldController(
            simpleTextList[3] as SectionSingleFieldElement,
            state
        )
    }

    private suspend fun verifySimpleTextSpecInTextFieldController(
        textElement: SectionSingleFieldElement,
        simpleTextSpec: SimpleTextSpec
    ) {
        val actualController = textElement.controller as TextFieldController
        assertThat(actualController.capitalization).isEqualTo(
            when (simpleTextSpec.capitalization) {
                Capitalization.none -> KeyboardCapitalization.None
                Capitalization.characters -> KeyboardCapitalization.Characters
                Capitalization.words -> KeyboardCapitalization.Words
                Capitalization.sentences -> KeyboardCapitalization.Sentences
            }
        )
        assertThat(actualController.keyboardType).isEqualTo(
            when (simpleTextSpec.keyboardType) {
                KeyboardType.text -> androidx.compose.ui.text.input.KeyboardType.Text
                KeyboardType.ascii -> androidx.compose.ui.text.input.KeyboardType.Ascii
                KeyboardType.number -> androidx.compose.ui.text.input.KeyboardType.Number
                KeyboardType.phone -> androidx.compose.ui.text.input.KeyboardType.Phone
                KeyboardType.uri -> androidx.compose.ui.text.input.KeyboardType.Uri
                KeyboardType.email -> androidx.compose.ui.text.input.KeyboardType.Email
                KeyboardType.password -> androidx.compose.ui.text.input.KeyboardType.Password
                KeyboardType.number_password -> androidx.compose.ui.text.input.KeyboardType.NumberPassword
            }
        )
        assertThat(actualController.label.first()).isEqualTo(
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
