package com.stripe.android.financialconnections.domain.prepane

import android.os.Parcelable
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.serializer.MarkdownToHtmlSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class OauthPrepane(
    @SerialName("body")
    val body: Body,
    @SerialName("cta")
    val cta: Cta,
    @SerialName("institution_icon")
    val institutionIcon: Image,
    //@SerialName("partner_notice")
    @Transient
    val partnerNotice: PartnerNotice = PartnerNotice(
        partnerIcon = Image("https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--reserve-primary-3x.png"),
        text = "LOLOLOLOLOLOLOLOLOL"
    ),
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String
) : Parcelable

@Serializable
@Parcelize
internal data class Body(
    @SerialName("entries")
    val entries: List<Entry>,
) : Parcelable
