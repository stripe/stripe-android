package com.stripe.android.paymentsheet.injection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import org.junit.Test

class AddressElementViewModelModuleTest {
    private val module = AddressElementViewModelModule()

    @Test
    fun `provideInlinePlacesClient returns null when config is null`() {
        val placesClient = module.provideInlinePlacesClient(
            args = AddressElementActivityContract.Args(
                publishableKey = "pk_123",
                config = null,
            ),
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            addressLauncherEventReporter = FakeAddressLauncherEventReporter(),
        )

        assertThat(placesClient).isNull()
    }

    @Test
    fun `provideInlinePlacesClient returns Stripe-hosted client when config is present`() {
        val placesClient = module.provideInlinePlacesClient(
            args = AddressElementActivityContract.Args(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration(billingAddress = null),
            ),
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            addressLauncherEventReporter = FakeAddressLauncherEventReporter(),
        )

        assertThat(placesClient).isNotNull()
    }
}
