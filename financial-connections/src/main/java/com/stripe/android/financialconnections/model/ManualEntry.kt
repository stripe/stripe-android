package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.core.model.serializers.EnumIgnoreUnknownSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Poko
class ManualEntry internal constructor(
    val mode: ManualEntryMode
) : Parcelable

@Serializable(with = ManualEntryMode.Serializer::class)
enum class ManualEntryMode(val value: String) {
    @SerialName(value = "automatic")
    AUTOMATIC("automatic"),

    @SerialName(value = "custom")
    CUSTOM("custom"),

    @SerialName(value = "unknown")
    UNKNOWN("unknown");

    internal object Serializer :
        EnumIgnoreUnknownSerializer<ManualEntryMode>(
            entries.toTypedArray(),
            UNKNOWN
        )
}
