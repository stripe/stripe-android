package com.stripe.android.uicore.address

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.address.AddressSchemaRepository.Companion.DEFAULT_COUNTRY_CODE
import com.stripe.android.uicore.address.AddressSchemaRepository.Companion.SUPPORTED_COUNTRIES
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.coroutines.coroutineContext

@RunWith(AndroidJUnit4::class)
class AddressRepositoryTest {
    @Test
    fun `Default country should always be in the supported country list`() {
        assertThat(SUPPORTED_COUNTRIES).contains("ZZ")
    }

    @Test
    fun `Country that doesn't exist return the default country`() = runTest {
        assertThat(SUPPORTED_COUNTRIES).doesNotContain("NB")

        val addressRepository = createAddressRepository()

        assertThat(addressRepository.get("NB"))
            .isEqualTo(addressRepository.get(DEFAULT_COUNTRY_CODE))
    }

    @Test
    fun `Correct supported country is returned`() = runTest {
        assertThat(SUPPORTED_COUNTRIES).contains("DE")

        val addressRepository = createAddressRepository()

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
                assertThat(SUPPORTED_COUNTRIES).contains(it.nameWithoutExtension)
            }
        }
    }

    @Test
    fun `Verify all supported countries deserialize`() = runTest {
        val addressRepository = createAddressRepository()

        SUPPORTED_COUNTRIES.forEach {
            if (it != "ZZ") {
                assertThat(addressRepository.get(it))
                    .isNotEqualTo(addressRepository.get(DEFAULT_COUNTRY_CODE))
            }
            assertThat(addressRepository.get(it))
                .isNotNull()
        }
    }
}

private suspend fun createAddressRepository(): AddressRepository {
    return AddressRepository(
        resources = ApplicationProvider.getApplicationContext<Application>().resources,
        workContext = coroutineContext,
    )
}
