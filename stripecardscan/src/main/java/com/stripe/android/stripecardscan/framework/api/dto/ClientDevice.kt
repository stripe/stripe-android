package com.stripe.android.stripecardscan.framework.api.dto

import androidx.annotation.RestrictTo
import com.stripe.android.stripecardscan.framework.util.Device
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ClientDevice(
    @SerialName("device_id") val android_id: String?,
    @SerialName("vendor_id") val vendor_id: String?,
    @SerialName("device_type") val name: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("platform") val platform: String
) {
    companion object {
        internal fun fromDevice(device: Device) = ClientDevice(
            android_id = device.android_id,
            vendor_id = device.android_id,
            name = device.name,
            osVersion = device.osVersion.toString(),
            platform = device.platform
        )
    }
}
