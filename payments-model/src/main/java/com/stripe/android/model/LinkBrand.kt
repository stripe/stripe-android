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

    @SerialName("onelink")
    Onelink("onelink");

    fun brandName(): String = when (this) {
        Link -> "Link"
        Onelink -> "Onelink"
    }

    fun baseUrl(): String = when (this) {
        Link -> "https://link.com"
        Onelink -> "https://onelink.com"
    }

    fun termsUrl(): String = "${baseUrl()}/terms"

    fun privacyUrl(): String = "${baseUrl()}/privacy"

    fun achAuthorizationTermsUrl(): String = "${baseUrl()}/terms/ach-authorization"

    internal object Serializer :
        EnumIgnoreUnknownSerializer<LinkBrand>(entries.toTypedArray(), Link)
}
