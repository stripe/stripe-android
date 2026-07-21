package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import org.junit.Test
import org.mockito.kotlin.mock

class AddressElementViewModelModuleTest {
    private val module = AddressElementViewModelModule()

    @Test
    fun `provideInlinePlacesClient returns hosted client without google api key when hosted autocomplete is enabled`() {
        val placesClient = module.provideInlinePlacesClient(
            context = mock<Context>(),
            args = AddressElementActivityContract.Args(
                publishableKey = "pk_123",
                stripeAccountId = null,
                config = AddressLauncher.Configuration(
                    billingAddress = null,
                    useStripeHostedAutocomplete = true,
                ),
            ),
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
        )

        assertThat(placesClient).isNotNull()
    }

    @Test
    fun `provideGooglePlacesClient returns null without google api key even when hosted autocomplete is enabled`() {
        val placesClient = module.provideGooglePlacesClient(
            context = mock<Context>(),
            args = AddressElementActivityContract.Args(
                publishableKey = "pk_123",
                stripeAccountId = null,
                config = AddressLauncher.Configuration(
                    billingAddress = null,
                    useStripeHostedAutocomplete = true,
                ),
            ),
        )

        assertThat(placesClient).isNull()
    }
}
