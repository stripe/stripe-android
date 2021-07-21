package com.stripe.android.paymentsheet.address

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository.DEFAULT_COUNTRY_CODE
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository.supportedCountries
import com.stripe.android.paymentsheet.parseAddressesSchema
import org.junit.Test
import java.io.File

class AddressFieldElementRepositoryTest {

    @Test
    fun `Default country should always be in the supported country list`() {
        assertThat(supportedCountries).contains("ZZ")
    }

    @Test
    fun `Country that doesn't exist return the default country`() {
        AddressFieldElementRepository.init(
            listOf("ZZ").associateWith { countryCode ->
                "src/main/assets/addressinfo/$countryCode.json"
            }
                .mapValues { (_, assetFileName) ->
                    requireNotNull(
                        parseAddressesSchema(
                            File(assetFileName).inputStream()
                        )
                    )
                }
        )

        assertThat(AddressFieldElementRepository.get("GG"))
            .isEqualTo(AddressFieldElementRepository.get(DEFAULT_COUNTRY_CODE))
    }

    @Test
    fun `Correct supported country is returned`() {
        AddressFieldElementRepository.init(
            supportedCountries.associateWith { countryCode ->
                "src/main/assets/addressinfo/$countryCode.json"
            }
                .mapValues { (_, assetFileName) ->
                    requireNotNull(
                        parseAddressesSchema(
                            File(assetFileName).inputStream()
                        )
                    )
                }
        )

        assertThat(supportedCountries).doesNotContain("GG")

        assertThat(AddressFieldElementRepository.get("GG"))
            .isEqualTo(AddressFieldElementRepository.get(DEFAULT_COUNTRY_CODE))
    }

}
