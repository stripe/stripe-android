package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.FakeStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.StripeHostedPlacesClientProxy
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

class AddressElementViewModelModuleTest {
    private val module = AddressElementViewModelModule()

    @Test
    fun `providePrimaryButtonAction returns standalone success`() = runTest {
        val address = AddressDetails()

        val result = module.providePrimaryButtonAction(
            args = AddressElementActivityContract.Args.Standalone(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration(),
            ),
        )(address)

        assertThat(result.getOrNull()).isEqualTo(
            AddressElementActivityContract.Result.StandaloneSucceeded(address)
        )
    }

    @Test
    fun `providePrimaryButtonAction returns checkout shipping success`() = runTest {
        val address = AddressDetails()

        val result = module.providePrimaryButtonAction(
            args = AddressElementActivityContract.Args.CheckoutShipping(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration(),
            ),
        )(address)

        assertThat(result.getOrNull()).isEqualTo(
            AddressElementActivityContract.Result.CheckoutShippingSucceeded(address)
        )
    }

    @Test
    fun `provideInlinePlacesClient returns hosted client by default when google client is available`() {
        val googlePlacesClient = mock<PlacesClientProxy>()
        val placesClient = module.provideInlinePlacesClient(
            args = AddressElementActivityContract.Args.Standalone(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration(),
            ),
            stripeAutocompleteRepository = FakeStripeAutocompleteRepository(),
            googlePlacesClient = googlePlacesClient,
            addressLauncherEventReporter = FakeAddressLauncherEventReporter(),
        )

        assertThat(placesClient).isInstanceOf(StripeHostedPlacesClientProxy::class.java)
        assertThat(placesClient).isNotSameInstanceAs(googlePlacesClient)
    }

    @Test
    fun `provideGooglePlacesClient returns null without google api key`() {
        val placesClient = module.provideGooglePlacesClient(
            context = mock<Context>(),
            args = AddressElementActivityContract.Args.Standalone(
                publishableKey = "pk_123",
                config = AddressLauncher.Configuration(billingAddress = null),
            ),
        )

        assertThat(placesClient).isNull()
    }
}
