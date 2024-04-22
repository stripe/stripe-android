package com.stripe.android.uicore.address

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.address.AddressSchemaRepository.Companion.DEFAULT_COUNTRY_CODE
import com.stripe.android.uicore.address.AddressSchemaRepository.Companion.SUPPORTED_COUNTRIES
import com.stripe.android.uicore.elements.IdentifierSpec
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
        val schemas = addressRepository.load()

        assertThat(schemas.get("NB"))
            .isEqualTo(schemas.get(DEFAULT_COUNTRY_CODE))
    }

    @Test
    fun `Correct supported country is returned`() = runTest {
        assertThat(SUPPORTED_COUNTRIES).contains("DE")

        val addressRepository = createAddressRepository()
        val schemas = addressRepository.load()

        val elements = schemas.get("DE")!!
        assertThat(elements[0].type?.identifierSpec).isEqualTo(IdentifierSpec.Line1)
        assertThat(elements[1].type?.identifierSpec).isEqualTo(IdentifierSpec.Line2)
        assertThat(elements[2].type?.identifierSpec).isEqualTo(IdentifierSpec.PostalCode)
        assertThat(elements[3].type?.identifierSpec).isEqualTo(IdentifierSpec.City)
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
        val schemas = addressRepository.load()

        SUPPORTED_COUNTRIES.forEach {
            assertThat(schemas.get(it))
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
