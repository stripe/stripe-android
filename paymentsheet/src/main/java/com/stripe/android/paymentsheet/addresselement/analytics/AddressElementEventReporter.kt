package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.editDistance

internal interface AddressElementEventReporter {
    fun onShown(country: String?)

    fun onSaveStarted(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    )

    fun onSaveFailed(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
        error: Throwable,
    )

    fun onSaveCompleted(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    )
}

internal class StandaloneAddressElementEventReporter(
    private val addressLauncherEventReporter: AddressLauncherEventReporter,
) : AddressElementEventReporter {
    override fun onShown(country: String?) {
        addressLauncherEventReporter.onShow(country.orEmpty())
    }

    override fun onSaveStarted(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) = Unit

    override fun onSaveFailed(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
        error: Throwable,
    ) = Unit

    override fun onSaveCompleted(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) {
        val savedCountry = addressDetails.address?.country ?: return
        addressLauncherEventReporter.onCompleted(
            country = savedCountry,
            autocompleteResultSelected = autocompleteAddressDetails != null,
            editDistance = autocompleteAddressDetails?.let { addressDetails.editDistance(it) },
        )
    }
}

internal class CheckoutShippingAddressElementEventReporter(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val checkoutSessionId: String,
) : AddressElementEventReporter {
    override fun onShown(country: String?) {
        fireEvent(
            ShippingAddressElementEvent.Shown(
                ShippingAddressElementAnalyticsData(
                    country = country.orEmpty(),
                    autocompleteResultSelected = null,
                    editDistance = null,
                )
            )
        )
    }

    override fun onSaveStarted(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) {
        fireEvent(
            ShippingAddressElementEvent.SaveStarted(
                analyticsData(addressDetails, autocompleteAddressDetails)
            )
        )
    }

    override fun onSaveFailed(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
        error: Throwable,
    ) {
        fireEvent(
            ShippingAddressElementEvent.SaveFailed(
                analyticsData(addressDetails, autocompleteAddressDetails),
                error,
            )
        )
    }

    override fun onSaveCompleted(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) {
        fireEvent(
            ShippingAddressElementEvent.SaveCompleted(
                analyticsData(addressDetails, autocompleteAddressDetails)
            )
        )
    }

    private fun analyticsData(
        addressDetails: AddressDetails,
        autocompleteAddressDetails: AddressDetails?,
    ) = ShippingAddressElementAnalyticsData(
        country = addressDetails.address?.country.orEmpty(),
        autocompleteResultSelected = autocompleteAddressDetails != null,
        editDistance = autocompleteAddressDetails?.let { addressDetails.editDistance(it) },
    )

    private fun fireEvent(event: ShippingAddressElementEvent) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = event,
                additionalParams = mapOf(FIELD_CHECKOUT_SESSION_ID to checkoutSessionId) +
                    event.additionalParams,
            )
        )
    }

    private companion object {
        const val FIELD_CHECKOUT_SESSION_ID = "checkout_session_id"
    }
}
