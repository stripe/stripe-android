package com.stripe.android.paymentsheet.address

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository.Companion.supportedCountries
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec
import org.junit.Test
import java.io.File
import java.security.InvalidParameterException

class TransformAddressToSpecTest {

    @Test
    fun `Read US Json`() {
        val addressSchema = readFile("src/main/assets/addressinfo/US.json")!!
        val simpleTextList = addressSchema.transformToSpecFieldList()

        val addressLine1 = SectionFieldSpec.SimpleText(
            IdentifierSpec("line1"),
            R.string.address_label_address,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = false
        )

        val addressLine2 = SectionFieldSpec.SimpleText(
            IdentifierSpec("line2"),
            R.string.address_label_address_line2,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = true
        )

        val city = SectionFieldSpec.SimpleText(
            IdentifierSpec("city"),
            R.string.address_label_city,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = false
        )

        val state = SectionFieldSpec.SimpleText(
            IdentifierSpec("state"),
            R.string.address_label_state,
            KeyboardCapitalization.Words,
            KeyboardType.Text,
            showOptionalLabel = false
        )

        val zip = SectionFieldSpec.SimpleText(
            IdentifierSpec("postal_code"),
            R.string.address_label_zip_code,
            KeyboardCapitalization.None,
            KeyboardType.Number,
            showOptionalLabel = false
        )

        assertThat(simpleTextList.size).isEqualTo(5)
        assertThat(simpleTextList[0]).isEqualTo(addressLine1)
        assertThat(simpleTextList[1]).isEqualTo(addressLine2)
        assertThat(simpleTextList[2]).isEqualTo(city)
        assertThat(simpleTextList[3]).isEqualTo(zip)
        assertThat(simpleTextList[4]).isEqualTo(state)
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

    private fun readFile(filename: String): List<AddressSchema>? {
        val file = File(filename)

        if (file.exists()) {
            return parseAddressesSchema(file.inputStream())
        } else {
            throw InvalidParameterException("Error could not find the test files.")
        }
    }
}
