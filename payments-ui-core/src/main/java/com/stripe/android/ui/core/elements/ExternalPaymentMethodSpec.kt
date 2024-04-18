package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ExternalPaymentMethodSpec(
    @SerialName("type")
    val type: String,

    @SerialName("light_image_url")
    val lightImageUrl: String,

    @SerialName("dark_image_url")
    val darkImageUrl: String? = null,

    // TODO: this name might be wrong
    @SerialName("localized_label")
    val localizedLabel: String? = null,
) : Parcelable

