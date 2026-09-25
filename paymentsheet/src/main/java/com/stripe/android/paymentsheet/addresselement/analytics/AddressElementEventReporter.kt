package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.editDistance

internal interface AddressElementEventReporter {
    fun onShown(snapshot: AddressElementAnalyticsSnapshot)

    fun onCanceled(snapshot: AddressElementAnalyticsSnapshot)

    fun onSaveStarted(snapshot: AddressElementAnalyticsSnapshot)

    fun onSaveFailed(snapshot: AddressElementAnalyticsSnapshot, error: Throwable)

    fun onSaveCompleted(snapshot: AddressElementAnalyticsSnapshot)
}

internal data class AddressElementAnalyticsSnapshot(
    val address: AddressDetails,
    val initialAddress: AddressDetails?,
    val autocompleteSelectedAddress: AddressDetails?,
)

internal class StandaloneAddressElementEventReporter(
    private val addressLauncherEventReporter: AddressLauncherEventReporter,
) : AddressElementEventReporter {
    override fun onShown(snapshot: AddressElementAnalyticsSnapshot) {
        addressLauncherEventReporter.onShow(snapshot.initialAddress?.address?.country.orEmpty())
    }

    override fun onCanceled(snapshot: AddressElementAnalyticsSnapshot) = Unit

    override fun onSaveStarted(snapshot: AddressElementAnalyticsSnapshot) = Unit

    override fun onSaveFailed(snapshot: AddressElementAnalyticsSnapshot, error: Throwable) = Unit

    override fun onSaveCompleted(snapshot: AddressElementAnalyticsSnapshot) {
        val country = snapshot.address.address?.country ?: return
        addressLauncherEventReporter.onCompleted(
            country = country,
            autocompleteResultSelected = snapshot.initialAddress?.address?.line1 != null,
            editDistance = snapshot.address.editDistance(snapshot.initialAddress),
        )
    }
}

internal class CheckoutShippingAddressElementEventReporter(
    private val addressLauncherEventReporter: AddressLauncherEventReporter,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val checkoutSessionId: String,
) : AddressElementEventReporter {
    override fun onShown(snapshot: AddressElementAnalyticsSnapshot) {
        addressLauncherEventReporter.updateAutocompleteCountry(
            snapshot.initialAddress?.address?.country.orEmpty()
        )
        fireEvent(
            ShippingAddressElementEvent.Shown(
                ShippingAddressElementAnalyticsData(
                    country = snapshot.address.address?.country.orEmpty(),
                    autocompleteResultSelected = null,
                    editDistance = null,
                )
            )
        )
    }

    override fun onCanceled(snapshot: AddressElementAnalyticsSnapshot) {
        fireEvent(ShippingAddressElementEvent.Canceled(snapshot.toAnalyticsData()))
    }

    override fun onSaveStarted(snapshot: AddressElementAnalyticsSnapshot) {
        fireEvent(ShippingAddressElementEvent.SaveStarted(snapshot.toAnalyticsData()))
    }

    override fun onSaveFailed(snapshot: AddressElementAnalyticsSnapshot, error: Throwable) {
        fireEvent(ShippingAddressElementEvent.SaveFailed(snapshot.toAnalyticsData(), error))
    }

    override fun onSaveCompleted(snapshot: AddressElementAnalyticsSnapshot) {
        fireEvent(ShippingAddressElementEvent.SaveCompleted(snapshot.toAnalyticsData()))
    }

    private fun AddressElementAnalyticsSnapshot.toAnalyticsData() = ShippingAddressElementAnalyticsData(
        country = address.address?.country.orEmpty(),
        autocompleteResultSelected = autocompleteSelectedAddress != null,
        editDistance = autocompleteSelectedAddress?.let { address.editDistance(it) },
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
