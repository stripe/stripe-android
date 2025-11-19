package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata.Value.Nested
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata.Value.SimpleBoolean
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata.Value.SimpleString
import com.stripe.android.utils.filterNotNullValues
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AnalyticsMetadata(
    private val values: Map<String, Value?>,
) : Parcelable {
    fun asMapOfAny(): Map<String, Any> {
        return values.asMapOfAny()
    }

    private fun Map<String, Value?>.asMapOfAny(): Map<String, Any> {
        return mapValues {
            when (val value = it.value) {
                is Nested -> value.value.asMapOfAny()
                is SimpleBoolean -> value.value
                is SimpleString -> value.value
                null -> null
            }
        }.filterNotNullValues()
    }

    sealed class Value : Parcelable {
        @Parcelize
        data class Nested(val value: Map<String, Value?>) : Value()

        @Parcelize
        data class SimpleString(val value: String?) : Value()

        @Parcelize
        data class SimpleBoolean(val value: Boolean?) : Value()
    }
}
