package com.stripe.android.ui.core.elements

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LpmSerializerTest {
    private val lpmSerializer = LpmSerializer()

    /**
     * Sofort is a little unique as it has a specific list of valid
     * countries, and the api path must be sofort[country]
     */
    @Test
    fun `Verify sofort country spec parsed correctly`() {
        val serializedString = ApplicationProvider
            .getApplicationContext<Application>()
            .resources
            .assets
            .open("lpms.json")
            .bufferedReader().use { it.readText() }

        val result = lpmSerializer.deserializeList(serializedString)

        val countrySpec = result.first { it.type == "sofort" }
            .fields
            .first() as CountrySpec

        assertThat(countrySpec.apiPath.v1).isEqualTo("sofort[country]")
        assertThat(countrySpec.allowedCountryCodes).isEqualTo(
            setOf(
                "AT",
                "BE",
                "DE",
                "ES",
                "IT",
                "NL"
            )
        )
    }

    @Test
    fun `Verify a DropdownSpec in lpms_json parses correctly`() {
        val serializedString = ApplicationProvider
            .getApplicationContext<Application>()
            .resources
            .assets
            .open("lpms.json")
            .bufferedReader().use { it.readText() }

        val result = lpmSerializer.deserializeList(serializedString)

        val dropdownSpec = result.first { it.type == "eps" }
            .fields[1] as DropdownSpec

        assertThat(dropdownSpec.apiPath.v1).isEqualTo("eps[bank]")
        assertThat(dropdownSpec.labelTranslationId).isEqualTo(TranslationId.EpsBank)
        assertThat(dropdownSpec.items.size).isEqualTo(28)
        assertThat(dropdownSpec.items[0]).isEqualTo(
            DropdownItemSpec(
                displayText = "Ärzte- und Apothekerbank",
                apiValue = "arzte_und_apotheker_bank"
            )
        )
        assertThat(dropdownSpec.items[27]).isEqualTo(
            DropdownItemSpec(
                displayText = "Other",
                apiValue = null
            )
        )
    }

    @Test
    fun `Verify SimpleTextSpec parses correctly`() {
        val serializedString = """
              {
                "type": "new_lpm",
                "async": true,
                "fields": [
                  {
                    "type": "text",
                    "api_path": {
                      "v1": "something_bogus"
                    },
                    "label": ${R.string.ideal_bank},
                    "capitalization": "sentences",
                    "keyboard_type": "phone",
                    "show_optional_label": true
                  }
                ]
              }
        """.trimIndent()

        val result = lpmSerializer.deserialize(serializedString)
        result.onFailure {
            println(it.message)
        }
        assertThat(result.isSuccess).isTrue()
        result.onSuccess {
            val addressSpec = it.fields[0] as SimpleTextSpec
            assertThat(addressSpec.label).isEqualTo(R.string.ideal_bank)
            assertThat(addressSpec.capitalization).isEqualTo(Capitalization.Sentences)
            assertThat(addressSpec.keyboardType).isEqualTo(KeyboardType.Phone)
            assertThat(addressSpec.showOptionalLabel).isTrue()
        }
    }

    @Test
    fun `Verify SimpleTextSpec defaults are correct`() {
        val serializedString = """
              {
                "type": "new_lpm",
                "async": true,
                "fields": [
                  {
                    "type": "text",
                    "api_path": {
                      "v1": "something_bogus"
                    },
                    "label": ${R.string.ideal_bank}
                  }
                ]
              }
        """.trimIndent()

        val result = lpmSerializer.deserialize(serializedString)
        result.onFailure {
            println(it.message)
        }
        assertThat(result.isSuccess).isTrue()
        result.onSuccess {
            val addressSpec = it.fields[0] as SimpleTextSpec
            assertThat(addressSpec.label).isEqualTo(R.string.ideal_bank)
            assertThat(addressSpec.capitalization).isEqualTo(Capitalization.None)
            assertThat(addressSpec.keyboardType).isEqualTo(KeyboardType.Ascii)
            assertThat(addressSpec.showOptionalLabel).isFalse()
        }
    }

    @Test
    fun `Verify DropdownSpec parses correctly`() {
        val serializedString = """
              {
                "type": "new_lpm",
                "async": true,
                "fields": [
                  {
                    "type": "selector",
                    "api_path": {
                      "v1": "something_bogus"
                    },
                    "label_translation_id": "upe.labels.ideal.bank",
                    "items": [
                      {
                        "api_value": "123",
                        "display_text": "abc"
                      },
                      {
                      }
                    ]
                  }
                ]
              }
        """.trimIndent()

        val result = lpmSerializer.deserialize(serializedString)
        result.onFailure {
            println(it.message)
        }
        assertThat(result.isSuccess).isTrue()
        result.onSuccess {
            val dropdownSpec = it.fields[0] as DropdownSpec
            assertThat(dropdownSpec.apiPath.v1).isEqualTo("something_bogus")
            assertThat(dropdownSpec.labelTranslationId).isEqualTo(TranslationId.IdealBank)
            assertThat(dropdownSpec.items.size).isEqualTo(2)
            assertThat(dropdownSpec.items[0]).isEqualTo(
                DropdownItemSpec(
                    "123", "abc"
                )
            )
            assertThat(dropdownSpec.items[1]).isEqualTo(
                DropdownItemSpec(
                    null, "Other"
                )
            )
        }
    }

    @Test
    fun `Verify address spec country codes parsed correctly`() {
        val serializedString = """
              {
                "type": "new_lpm",
                "async": true,
                "fields": [
                  {
                    "type": "billing_address",
                    "allowed_country_codes": [
                      "AT", "BE"
                    ]
                  }
                ]
              }
        """.trimIndent()

        val result = lpmSerializer.deserialize(serializedString)
        result.onFailure {
            println(it.message)
        }
        assertThat(result.isSuccess).isTrue()
        result.onSuccess {
            val addressSpec = it.fields[0] as AddressSpec
            assertThat(addressSpec.allowedCountryCodes).isEqualTo(setOf("AT", "BE"))
        }
    }

    @Test
    fun `Verify all types api_path parsed correctly`() {
        val types = listOf(
            "billing_address", "affirm_header", "afterpay_header",
            "au_becs_bsb_number", "au_becs_account_number", "au_becs_mandate",
            "country", "email", "iban", "klarna_country",
            "klarna_header", "static_text", "name", "mandate", "sepa_mandate"
        )
        types.forEach { fieldType ->
            println("field type: $fieldType")
            val serializedString = """
              {
                "type": "new_lpm",
                "async": true,
                "fields": [
                  {
                    "type": "$fieldType",
                    "api_path": {
                      "v1": "something_bogus"
                    },
                    "stringResId": ${R.string.email}
                  }
                ]
              }
            """.trimIndent()

            val result = lpmSerializer.deserialize(serializedString)
            assertThat(result.isSuccess).isTrue()
            result.onSuccess { sharedDataSpec ->
                val fieldItemSpec = sharedDataSpec.fields[0]
                assertThat(fieldItemSpec.apiPath.v1).isEqualTo("something_bogus")
            }
        }
    }

    @Test
    fun `Verify all specs have default api_path parsed correctly`() {
        val types = mapOf(
            "billing_address" to "billing_details[address]",
            "affirm_header" to "affirm_header",
            "afterpay_header" to "afterpay_text",
            "au_becs_bsb_number" to "au_becs_debit[bsb_number]",
            "au_becs_account_number" to "au_becs_debit[account_number]",
            "au_becs_mandate" to "au_becs_mandate",
            "country" to "billing_details[address][country]",
            "email" to "billing_details[email]",
            "iban" to "sepa_debit[iban]",
            "klarna_country" to "billing_details[address][country]",
            "klarna_header" to "klarna_header_text",
            "name" to "billing_details[name]",
            "mandate" to "mandate",
            "sepa_mandate" to "sepa_mandate",
            "static_text" to "static_text",
        )
        // the types selector, and text do not have default api_paths and are tested separately

        types.forEach { (key, value) ->
            println("type: $key")

            // StringResId added for those types requiring it, expect it to be ignored otherwise
            val serializedString = """
              {
                "type": "new_lpm",
                "async": true,
                "fields": [
                  {
                    "type": "$key",
                    "stringResId": ${R.string.email}
                  }
                ]
              }
            """.trimIndent()

            val result = lpmSerializer.deserialize(serializedString)
            assertThat(result.isSuccess).isTrue()
            result.onSuccess {
                val fieldSpec = it.fields[0]
                assertThat(fieldSpec.apiPath.v1).isEqualTo(value)
            }
        }
    }

    @Test
    fun `Verify that unknown field in Json spec deserializes - ignoring the field`() {
        val serializedString =
            """
                {
                    "type": "au_becs_debit",
                    "fields": [
                      {
                        "type": "unknown_field",
                        "unknown_value": {
                          "some_stuff": "some_value"
                        }
                      }
                    ]
                  }
            """.trimIndent()

        val result = lpmSerializer.deserialize(serializedString)
        assertThat(result.isSuccess).isTrue()
        result.onSuccess {
            assertThat(it.fields).isEqualTo(
                listOf(EmptyFormSpec)
            )
        }
    }

    @Test
    fun `Verify that async defaults to false and fields to empty`() {
        val serializedString =
            """
                {
                    "type": "unknown_lpm"
                }
            """.trimIndent()

        val result = lpmSerializer.deserialize(serializedString)
        assertThat(result.isSuccess).isTrue()
        result.onSuccess {
            assertThat(it.async).isFalse()
            assertThat(it.fields).isEqualTo(listOf(EmptyFormSpec))
        }
    }

    @Test
    fun `Deserialize each field type`() {
        val lpms = lpmSerializer.deserializeList(JSON_ALL_FIELDS)
        assertThat(lpms.first().fields.size).isEqualTo(17)

        // Empty would mean a field is not recognized/ignored.
        assertThat(lpms.filterIsInstance<EmptyFormSpec>()).isEmpty()
    }

    companion object {
        val JSON_ALL_FIELDS = """
                [
                  {
                    "type": "llamaBucks",
                    "async": false,
                    "fields": [
                      {
                        "type": "name",
                        "api_path": {
                          "v1": "billing_details[name]"
                        }
                      },
                      {
                        "type": "afterpay_header"
                      },
                      {
                        "type": "email",
                        "api_path": {
                          "v1": "billing_details[email]"
                        }
                      },
                      {
                        "type": "billing_address",
                        "api_path": {
                          "v1": "billing_details[address]"
                        }
                      },
                      {
                        "type": "affirm_header"
                      },
                      {
                        "type": "klarna_header"
                      },
                      {
                        "type": "klarna_country",
                        "api_path": {
                          "v1": "billing_details[address][country]"
                        }
                      },
                      {
                        "type": "selector",
                        "label_translation_id": "upe.labels.ideal.bank",
                        "items": [
                          {
                            "display_text": "ABN Amro",
                            "api_value": "abn_amro"
                          }
                        ],
                        "api_path": {
                          "v1": "ideal[bank]"
                        }
                      },
                      {
                        "type": "billing_address",
                        "allowed_country_codes": [
                          "AT",
                          "BE",
                          "DE",
                          "ES",
                          "IT",
                          "NL"
                        ],
                        "display_fields": [
                          "country"
                        ]
                      },
                      {
                        "type": "iban",
                        "api_path": {
                          "v1": "sepa_debit[iban]"
                        }
                      },
                      {
                        "type": "sepa_mandate"
                      },
                      {
                        "type": "name",
                        "api_path": {
                          "v1": "billing_details[name]"
                        },
                        "label_translation_id": "upe.labels.name.onAccount"
                      },
                      {
                        "type": "au_becs_bsb_number",
                        "api_path": {
                          "v1": "au_becs_debit[bsb_number]"
                        }
                      },
                      {
                        "type": "au_becs_account_number",
                        "api_path": {
                          "v1": "au_becs_debit[account_number]"
                        }
                      },
                      {
                        "type": "au_becs_mandate"
                      },
                      {
                        "type": "card_details"
                      },
                      {
                        "type": "card_billing"
                      }
                    ]
                  }
               ]
        """.trimIndent()
    }
}
