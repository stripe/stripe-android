package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.address.autocomplete.model.Place
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.lang.NullPointerException

@RunWith(RobolectricTestRunner::class)
class AddressControllerTest {
    private val emailController = SimpleTextFieldController(
        EmailConfig()
    )
    private val ibanController =
        SimpleTextFieldController(
            IbanConfig()
        )
    private val sectionFieldElementFlow = MutableStateFlow(
        listOf(
            EmailElement(
                IdentifierSpec.Email,
                controller = emailController
            ),
            IbanElement(
                IdentifierSpec.Generic("sepa_debit[iban]"),
                ibanController
            )
        )
    )
    private val addressController = AddressController(
        sectionFieldElementFlow
    )

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify the first error field is the error published`() {
        runBlocking {
            // Both email and iban invalid, should show email error first
            val emailInvalidEmailCharacters = "98kl;;;@dkc9.com"
            emailController.onValueChange(emailInvalidEmailCharacters)
            val ibanInvalidCountryCode = "12334"
            ibanController.onValueChange(ibanInvalidCountryCode)

            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(addressController.error.first()?.errorMessage)
                .isEqualTo(R.string.email_is_invalid)

            emailController.onValueChange("joe@email.com")

            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(addressController.error.first()?.errorMessage)
                .isEqualTo(R.string.iban_invalid_start)
        }
    }

    @Test
    fun test() {
        val address = listOf(
            AddressComponent(name="990", types=listOf("street_number")),
            AddressComponent(name="Washington Avenue", types=listOf("route")),
            AddressComponent(name="Brooklyn", types=listOf("sublocality_level_1", "sublocality", "political")),
            AddressComponent(name="Kings County", types=listOf("administrative_area_level_2", "political")),
            AddressComponent(name="New York", types=listOf("administrative_area_level_1", "political")),
            AddressComponent(name="United States", types=listOf("country", "political")),
            AddressComponent(name="11225", types=listOf("postal_code")),
        ).toAddress()

        assertThat(address.line1).isEqualTo("990 Washington Avenue")
    }

    @Throws(NullPointerException::class)
    private fun List<AddressComponent>.toAddress(): Address {
        val filter: (Place.Type) -> String? = { type ->
            this.find { it.types.contains(type.value) }?.name
        }

        val line1 = listOfNotNull(filter(Place.Type.STREET_NUMBER), filter(Place.Type.ROUTE))
            .joinToString(" ")
            .ifBlank { null }
        val city = filter(Place.Type.LOCALITY) ?: filter(Place.Type.SUBLOCALITY)
        val state = filter(Place.Type.ADMINISTRATIVE_AREA_LEVEL_1)
        val country = filter(Place.Type.COUNTRY)
        val postalCode = filter(Place.Type.POSTAL_CODE)

        return Address.Builder()
            .setLine1(line1)
            .setCity(city)
            .setState(state)
            .setCountry(country)
            .setPostalCode(postalCode)
            .build()
    }
}
