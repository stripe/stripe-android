package com.stripe.android.paymentsheet.addresselement.analytics

import com.stripe.android.core.networking.AnalyticsEvent

internal sealed class AddressLauncherEvent : AnalyticsEvent {
    abstract val additionalParams: Map<String, String>

    class Show(
        val country: String
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_show"
        override val additionalParams: Map<String, String>
            get() {
                return mapOf(
                    FIELD_ADDRESS_DATA_BLOB to mapOf(
                        FIELD_ADDRESS_COUNTRY_CODE to country
                    ).toString()
                )
            }
    }

    class Completed(
        val country: String,
        private val autocompleteResultSelected: Boolean,
        private val editDistance: Int?
    ) : AddressLauncherEvent() {
        override val eventName: String = "mc_address_completed"
        override val additionalParams: Map<String, String>
            get() {
                val data = mutableMapOf<String, Any>(
                    FIELD_ADDRESS_COUNTRY_CODE to country,
                    FIELD_AUTO_COMPLETE_RESULT_SELECTED to autocompleteResultSelected
                )
                editDistance?.let {
                    data[FIELD_EDIT_DISTANCE] = it
                }
                return mapOf(
                    FIELD_ADDRESS_DATA_BLOB to data.toString()
                )
            }
    }

    internal companion object {
        const val FIELD_ADDRESS_DATA_BLOB = "address_data_blob"
        const val FIELD_ADDRESS_COUNTRY_CODE = "address_country_code"
        const val FIELD_AUTO_COMPLETE_RESULT_SELECTED = "auto_complete_result_selected"
        const val FIELD_EDIT_DISTANCE = "edit_distance"
    }
}
