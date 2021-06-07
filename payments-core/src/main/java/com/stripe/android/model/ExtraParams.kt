package com.stripe.android.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
internal data class ExtraParams(
    val value: Map<String, Any>? = emptyMap()
) : Parcelable {
    internal companion object : Parceler<ExtraParams> {
        override fun ExtraParams.write(parcel: Parcel, flags: Int) {
            parcel.writeString(
                StripeJsonUtils.mapToJsonObject(value)?.toString()
            )
        }

        override fun create(parcel: Parcel): ExtraParams {
            val json = parcel.readString()?.let {
                JSONObject(it)
            }
            return ExtraParams(
                StripeJsonUtils.jsonObjectToMap(json)
                    .orEmpty()
                    .toList()
                    .fold(emptyMap()) { acc, (key, value) ->
                        // remove null values
                        acc.plus(
                            value?.let { mapOf(key to it) }.orEmpty()
                        )
                    }
            )
        }
    }
}
