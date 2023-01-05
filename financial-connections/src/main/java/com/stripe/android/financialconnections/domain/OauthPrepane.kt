package com.stripe.android.financialconnections.domain

import android.os.Parcelable
import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.serializer.EntrySerializer
import com.stripe.android.financialconnections.model.serializer.MarkdownToHtmlSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
internal data class Display(
    @SerialName("text")
    val text: Text
) : Parcelable

@Serializable
@Parcelize
internal data class Text(
    @SerialName("oauth_prepane")
    val oauthPrepane: OauthPrepane,
) : Parcelable

@Serializable
@Parcelize
internal data class OauthPrepane(
    @SerialName("body")
    val body: Body,
    @SerialName("cta")
    val cta: Cta,
    @SerialName("institution_icon")
    val institutionIcon: Image,
    @SerialName("partner_notice")
    val partnerNotice: PartnerNotice? = null,
    @SerialName("data_access_notice")
    val dataAccessNotice: DataAccessNotice? = null,
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String
) : Parcelable

@Serializable
@Parcelize
internal data class Cta(
    @SerialName("icon")
    val icon: Image? = null,
    @SerialName("text")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val text: String
) : Parcelable

@Serializable
@Parcelize
internal data class PartnerNotice(
    @SerialName("partner_icon")
    val partnerIcon: Image,
    @SerialName("text")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val text: String
) : Parcelable

@Serializable
@Parcelize
internal data class Body(
    @SerialName("entries")
    val entries: List<Entry>,
) : Parcelable

@Serializable(with = EntrySerializer::class)
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
