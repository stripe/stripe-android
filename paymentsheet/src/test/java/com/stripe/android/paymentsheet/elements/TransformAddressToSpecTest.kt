package com.stripe.android.paymentsheet.elements

import com.stripe.android.paymentsheet.specifications.SectionFieldSpec
import kotlinx.serialization.decodeFromString
import org.junit.Test
import java.io.File


class TransformAddressToSpec {
    private val supportedCountries = arrayOf(
        "AE", "AT", "AU", "BE", "BG", "BR", "CA", "CH", "CI", "CR", "CY", "CZ", "DE", "DK", "DO",
        "EE", "ES", "FI", "FR", "GB", "GI", "GR", "GT", "HK", "HU", "ID", "IE", "IN", "IT",
        "JP", "LI", "LT", "LU", "LV", "MT", "MX", "MY", "NL", "NO", "NZ", "PE", "PH", "PL",
        "PT", "RO", "SE", "SG", "SI", "SK", "SN", "TH", "TT", "US", "UY"
    )

    @Test
    fun `Read json state schema`() {
        val str = """
            {
              "isoID": "AL",
              "key": "AL",
              "name": "Alabama",
              "latinName": null
            }
        """.trimIndent()
        println(
            format.decodeFromString<StateSchema>(
                str
            )
        )
    }

    @Test
    fun `Read json field schema`() {
        val str = """
            {
                  "nameType": "state",
                  "list": [
                    {
                      "isoID": "AL",
                      "key": "AL",
                      "name": "Alabama",
                      "latinName": null
                    },
                    {
                      "isoID": "AK",
                      "key": "AK",
                      "name": "Alaska",
                      "latinName": null
                    }
                  ]
            }
        """.trimIndent()
        println(
            format.decodeFromString<FieldSchema>(
                str
            )
        )
    }

    @Test
    fun `Read address schema`() {
        val str = """
              {
                "type": "administrativeArea",
                "required": true,
                "schema": {
                  "nameType": "state",
                  "list": [
                    {
                      "isoID": "AL",
                      "key": "AL",
                      "name": "Alabama",
                      "latinName": null
                    }
                  ]
                }
              }
        """.trimIndent()

        println(
            format.decodeFromString<AddressSchema>(
                str
            )
        )
    }

    @Test
    fun `Read AddressSchema list file and output spec json`() {
        for (countryCode in supportedCountries) {
            val file = File(
                "/Users/michelleb/stripe/stripe-android/paymentsheet/src/main/assets/addressinfo/US.json"
            )

            if (file.exists()) {

                val addressSchema = parseAddressesSchema(file.inputStream())
//                addressSchema
//                    ?.forEach {
//                        println(it.type?.name + " " + it.required)
//                    }
                addressSchema?.let {
                    val elementList = it.transformToSpecFieldList()
                    elementList.forEach { it ->
                        val spec = it as? SectionFieldSpec.SimpleText
                        println(spec?.identifier?.value + " " + spec?.isRequired)
                    }
                }

                break
            }
        }
    }
}
