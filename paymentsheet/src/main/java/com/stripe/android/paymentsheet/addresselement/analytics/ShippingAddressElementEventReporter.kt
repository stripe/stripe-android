package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory

internal interface ShippingAddressElementEventReporter {
    fun onShown(addressData: ShippingAddressElementAnalyticsData)

    fun onCanceled(addressData: ShippingAddressElementAnalyticsData)

    fun onSaveStarted(addressData: ShippingAddressElementAnalyticsData)

    fun onSaveFailed(addressData: ShippingAddressElementAnalyticsData, error: Throwable)

    fun onSaveCompleted(addressData: ShippingAddressElementAnalyticsData)
}

internal class DefaultShippingAddressElementEventReporter(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val checkoutSessionId: String,
) : ShippingAddressElementEventReporter {
    override fun onShown(addressData: ShippingAddressElementAnalyticsData) {
        fireEvent(ShippingAddressElementEvent.Shown(addressData))
    }

    override fun onCanceled(addressData: ShippingAddressElementAnalyticsData) {
        fireEvent(ShippingAddressElementEvent.Canceled(addressData))
    }

    override fun onSaveStarted(addressData: ShippingAddressElementAnalyticsData) {
        fireEvent(ShippingAddressElementEvent.SaveStarted(addressData))
    }

    override fun onSaveFailed(addressData: ShippingAddressElementAnalyticsData, error: Throwable) {
        fireEvent(ShippingAddressElementEvent.SaveFailed(addressData, error))
    }

    override fun onSaveCompleted(addressData: ShippingAddressElementAnalyticsData) {
        fireEvent(ShippingAddressElementEvent.SaveCompleted(addressData))
    }

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

internal object NoOpShippingAddressElementEventReporter : ShippingAddressElementEventReporter {
    override fun onShown(addressData: ShippingAddressElementAnalyticsData) = Unit

    override fun onCanceled(addressData: ShippingAddressElementAnalyticsData) = Unit

    override fun onSaveStarted(addressData: ShippingAddressElementAnalyticsData) = Unit

    override fun onSaveFailed(addressData: ShippingAddressElementAnalyticsData, error: Throwable) = Unit

    override fun onSaveCompleted(addressData: ShippingAddressElementAnalyticsData) = Unit
}
