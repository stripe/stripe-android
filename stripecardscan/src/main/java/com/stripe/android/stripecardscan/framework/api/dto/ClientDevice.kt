package com.stripe.android.stripecardscan.framework.api.dto

import androidx.annotation.RestrictTo
import com.stripe.android.stripecardscan.framework.util.Device
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ClientDevice(
    @SerialName("device_id") val android_id: String?,
    @SerialName("vendor_id") val vendor_id: String?,
    @SerialName("device_type") val name: String,
// TODO: these should probably be reported as part of scanstats, but are not yet supported
//    @SerialName("boot_count") val bootCount: Int,
//    @SerialName("locale") val locale: String?,
//    @SerialName("carrier") val carrier: String?,
//    @SerialName("network_operator") val networkOperator: String?,
//    @SerialName("phone_type") val phoneType: Int?,
//    @SerialName("phone_count") val phoneCount: Int,
    @SerialName("os_version") val osVersion: String,
    @SerialName("platform") val platform: String
) {
    companion object {
        internal fun fromDevice(device: Device) = ClientDevice(
            android_id = device.android_id,
            vendor_id = device.android_id,
            name = device.name,
// TODO: these should probably be reported as part of scanstats, but are not yet supported
//            bootCount = device.bootCount,
//            locale = device.locale,
//            carrier = device.carrier,
//            networkOperator = device.networkOperator,
//            phoneType = device.phoneType,
//            phoneCount = device.phoneCount,
            osVersion = device.osVersion.toString(),
            platform = device.platform
        )
    }
}
