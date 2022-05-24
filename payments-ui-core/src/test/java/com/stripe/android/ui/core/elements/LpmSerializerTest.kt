package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LpmSerializerTest {
    private val lpmSerializer = LpmSerializer()

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
        // TODO: Test this with LPM Repository too
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
            assertThat(it.fields).isEmpty()
        }
    }

    @Test
    fun deserializeFields() {
        val lpms = lpmSerializer.deserializeList(JSON_ALL_FIELDS)
        assertThat(lpms.first().fields.size).isEqualTo(17)

        // Empty would mean a field is not recognized.
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
                        "label": "upe.labels.ideal.bank",
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
                        "valid_country_codes": [
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
                        "label": "upe.labels.name.onAccount"
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
