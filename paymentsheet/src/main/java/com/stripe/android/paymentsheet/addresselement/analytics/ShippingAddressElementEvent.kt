package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.payments.core.analytics.ErrorReporter

internal sealed class ShippingAddressElementEvent : AnalyticsEvent {
    abstract val additionalParams: Map<String, Any>

    class Shown(
        private val addressData: ShippingAddressElementAnalyticsData,
    ) : ShippingAddressElementEvent() {
        override val eventName: String = "elements.shipping_address.shown"
        override val additionalParams: Map<String, Any>
            get() = mapOf(FIELD_ADDRESS_DATA_BLOB to addressData.toAnalyticsParams())
    }

    class Canceled(
        private val addressData: ShippingAddressElementAnalyticsData,
    ) : ShippingAddressElementEvent() {
        override val eventName: String = "elements.shipping_address.canceled"
        override val additionalParams: Map<String, Any>
            get() = mapOf(FIELD_ADDRESS_DATA_BLOB to addressData.toAnalyticsParams())
    }

    class SaveStarted(
        private val addressData: ShippingAddressElementAnalyticsData,
    ) : ShippingAddressElementEvent() {
        override val eventName: String = "elements.shipping_address.save_started"
        override val additionalParams: Map<String, Any>
            get() = mapOf(FIELD_ADDRESS_DATA_BLOB to addressData.toAnalyticsParams())
    }

    class SaveFailed(
        private val addressData: ShippingAddressElementAnalyticsData,
        private val error: Throwable,
    ) : ShippingAddressElementEvent() {
        override val eventName: String = "elements.shipping_address.save_failed"
        override val additionalParams: Map<String, Any>
            get() = mapOf(FIELD_ADDRESS_DATA_BLOB to addressData.toAnalyticsParams()) +
                ErrorReporter.getAdditionalParamsFromError(error)
    }

    class SaveCompleted(
        private val addressData: ShippingAddressElementAnalyticsData,
    ) : ShippingAddressElementEvent() {
        override val eventName: String = "elements.shipping_address.save_completed"
        override val additionalParams: Map<String, Any>
            get() = mapOf(FIELD_ADDRESS_DATA_BLOB to addressData.toAnalyticsParams())
    }

    internal companion object {
        const val FIELD_ADDRESS_DATA_BLOB = "address_data_blob"
        const val FIELD_ADDRESS_COUNTRY_CODE = "address_country_code"
        const val FIELD_AUTO_COMPLETE_RESULT_SELECTED = "auto_complete_result_selected"
        const val FIELD_EDIT_DISTANCE = "edit_distance"
    }
}

internal data class ShippingAddressElementAnalyticsData(
    val country: String,
    val autocompleteResultSelected: Boolean? = null,
    val editDistance: Int? = null,
) {
    fun toAnalyticsParams(): Map<String, Any> = buildMap {
        put(ShippingAddressElementEvent.FIELD_ADDRESS_COUNTRY_CODE, country)
        autocompleteResultSelected?.let {
            put(ShippingAddressElementEvent.FIELD_AUTO_COMPLETE_RESULT_SELECTED, it)
        }
        editDistance?.let {
            put(ShippingAddressElementEvent.FIELD_EDIT_DISTANCE, it)
        }
    }
}
