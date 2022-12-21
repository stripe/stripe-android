package com.stripe.android.financialconnections.domain.prepane

import android.os.Parcelable
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.model.serializer.MarkdownToHtmlSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class PartnerNotice(
    @SerialName("partner_icon")
    val partnerIcon: Image,
    @SerialName("text")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val text: String
) : Parcelable
