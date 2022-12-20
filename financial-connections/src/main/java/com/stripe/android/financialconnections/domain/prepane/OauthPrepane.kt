package com.stripe.android.financialconnections.domain.prepane

import android.os.Parcelable
import com.stripe.android.financialconnections.model.Image
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class OauthPrepane(
    @SerialName("body")
    val body: List<Body>,
    @SerialName("cta")
    val cta: Cta,
    @SerialName("institution_icon")
    val institutionIcon: Image,
    @SerialName("partner_notice")
    val partnerNotice: PartnerNotice,
    @SerialName("title")
    val title: String
) : Parcelable
