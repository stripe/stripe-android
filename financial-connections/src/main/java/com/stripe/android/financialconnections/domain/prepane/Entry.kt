package com.stripe.android.financialconnections.domain.prepane

import android.os.Parcelable
import com.stripe.android.financialconnections.model.serializer.BodySerializer
import com.stripe.android.financialconnections.model.serializer.MarkdownToHtmlSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable(with = BodySerializer::class)
internal sealed class Entry : Parcelable {
    @Serializable
    @Parcelize
    internal data class Text(
        @Serializable(with = MarkdownToHtmlSerializer::class)
        val content: String
    ) : Entry(), Parcelable

    @Serializable
    @Parcelize
    internal data class Image(
        val content: com.stripe.android.financialconnections.model.Image
    ) : Entry(), Parcelable

    companion object {
        internal const val TYPE_TEXT = "text"
        internal const val TYPE_IMAGE = "image.account"
    }
}
