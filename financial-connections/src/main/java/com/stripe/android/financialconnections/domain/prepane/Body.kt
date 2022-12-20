package com.stripe.android.financialconnections.domain.prepane

import android.os.Parcelable
import com.stripe.android.financialconnections.model.serializer.BodySerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable(with = BodySerializer::class)
internal sealed class Body : Parcelable {
    @Serializable
    @Parcelize
    internal data class Text(
        val type: String,
        val content: String
    ) : Body(), Parcelable

    @Serializable
    @Parcelize
    internal data class Image(
        val type: String,
        val content: com.stripe.android.financialconnections.model.Image
    ) : Body(), Parcelable

    companion object {
        internal const val TYPE_TEXT = "text"
        internal const val TYPE_IMAGE = "image.account"
    }
}
