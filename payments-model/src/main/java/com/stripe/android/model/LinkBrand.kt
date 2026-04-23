package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.serializers.EnumIgnoreUnknownSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable(with = LinkBrand.Serializer::class)
enum class LinkBrand(val value: String) {

    @SerialName("link")
    Link("link"),

    @SerialName("notlink")
    Notlink("notlink");

    fun brandName(): String = when (this) {
        Link -> "Link"
        Notlink -> "Notlink"
    }

    internal object Serializer :
        EnumIgnoreUnknownSerializer<LinkBrand>(entries.toTypedArray(), Link)
}
