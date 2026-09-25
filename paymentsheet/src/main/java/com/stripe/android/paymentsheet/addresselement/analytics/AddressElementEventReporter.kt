package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory

internal interface AddressElementEventReporter {
    fun onShown(country: String?)

    fun onCanceled(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    )

    fun onSaveStarted(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    )

    fun onSaveFailed(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
        error: Throwable,
    )

    fun onSaveCompleted(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    )
}

internal class StandaloneAddressElementEventReporter(
    private val addressLauncherEventReporter: AddressLauncherEventReporter,
) : AddressElementEventReporter {
    override fun onShown(country: String?) {
        addressLauncherEventReporter.onShow(country.orEmpty())
    }

    override fun onCanceled(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) = Unit

    override fun onSaveStarted(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) = Unit

    override fun onSaveFailed(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
        error: Throwable,
    ) = Unit

    override fun onSaveCompleted(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) {
        val savedCountry = country ?: return
        addressLauncherEventReporter.onCompleted(
            country = savedCountry,
            autocompleteResultSelected = autocompleteResultSelected,
            editDistance = editDistance,
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

    override fun onCanceled(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) {
        fireEvent(
            ShippingAddressElementEvent.Canceled(
                analyticsData(country, autocompleteResultSelected, editDistance)
            )
        )
    }

    override fun onSaveStarted(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) {
        fireEvent(
            ShippingAddressElementEvent.SaveStarted(
                analyticsData(country, autocompleteResultSelected, editDistance)
            )
        )
    }

    override fun onSaveFailed(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
        error: Throwable,
    ) {
        fireEvent(
            ShippingAddressElementEvent.SaveFailed(
                analyticsData(country, autocompleteResultSelected, editDistance),
                error,
            )
        )
    }

    override fun onSaveCompleted(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) {
        fireEvent(
            ShippingAddressElementEvent.SaveCompleted(
                analyticsData(country, autocompleteResultSelected, editDistance)
            )
        )
    }

    private fun analyticsData(
        country: String?,
        autocompleteResultSelected: Boolean,
        editDistance: Int?,
    ) = ShippingAddressElementAnalyticsData(
        country = country.orEmpty(),
        autocompleteResultSelected = autocompleteResultSelected,
        editDistance = editDistance,
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
