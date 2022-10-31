package com.stripe.android.ui.core.address

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.address.AddressRepository.Companion.DEFAULT_COUNTRY_CODE
import com.stripe.android.ui.core.address.AddressRepository.Companion.supportedCountries
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.RowElement
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AddressRepositoryTest {

    private val addressRepository = AddressRepository(
        ApplicationProvider.getApplicationContext<Application>().resources
    )

    @Test
    fun `Default country should always be in the supported country list`() {
        assertThat(supportedCountries).contains("ZZ")
    }

    @Test
    fun `Country that doesn't exist return the default country`() {
        assertThat(supportedCountries).doesNotContain("NB")

        assertThat(addressRepository.get("NB"))
            .isEqualTo(addressRepository.get(DEFAULT_COUNTRY_CODE))
    }

    @Test
    fun `Correct supported country is returned`() {
        assertThat(supportedCountries).contains("DE")

        val elements = addressRepository.get("DE")!!
        assertThat(elements[0].identifier).isEqualTo(IdentifierSpec.Line1)
        assertThat(elements[1].identifier).isEqualTo(IdentifierSpec.Line2)
        assertThat((elements[2] as RowElement).fields[0].identifier).isEqualTo(IdentifierSpec.PostalCode)
        assertThat((elements[2] as RowElement).fields[1].identifier).isEqualTo(IdentifierSpec.City)
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
        supportedCountries.forEach {
            if (it != "ZZ") {
                assertThat(addressRepository.get(it))
                    .isNotEqualTo(addressRepository.get(DEFAULT_COUNTRY_CODE))
            }
            assertThat(addressRepository.get(it))
                .isNotNull()
        }
    }
}
