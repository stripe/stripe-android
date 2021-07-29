package com.stripe.android.paymentsheet.address

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository.Companion.DEFAULT_COUNTRY_CODE
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository.Companion.supportedCountries
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File

class AddressFieldElementRepositoryTest {

    private val addressFieldElementRepository = AddressFieldElementRepository(mock())

    @Test
    fun `Default country should always be in the supported country list`() {
        assertThat(supportedCountries).contains("ZZ")
    }

    @Test
    fun `Country that doesn't exist return the default country`() {
        addressFieldElementRepository.init(
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

        assertThat(addressFieldElementRepository.get("GG"))
            .isEqualTo(addressFieldElementRepository.get(DEFAULT_COUNTRY_CODE))
    }

    @Test
    fun `Correct supported country is returned`() {
        addressFieldElementRepository.init(
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

        assertThat(supportedCountries).doesNotContain("NB")

        assertThat(addressFieldElementRepository.get("GG"))
            .isEqualTo(addressFieldElementRepository.get(DEFAULT_COUNTRY_CODE))
    }

    @Test
    fun `Verify only supported countries have json file`() {
        val files = File("src/main/assets/addressinfo").listFiles()

        if (files?.isEmpty() == false) {
            files.forEach {
                assertThat(supportedCountries).contains(it.nameWithoutExtension)
            }
        }
    }

    @Test
    fun `Verify all supported countries deserialize`() {
        addressFieldElementRepository.init(
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
    }
}
