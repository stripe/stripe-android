package com.stripe.android.financialconnections.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class SynchronizeSessionResponse(
    @SerialName("manifest")
    val manifest: FinancialConnectionsSessionManifest,
    @SerialName("text")
    val text: TextUpdate? = null,
    @SerialName("visual")
    val visual: VisualUpdate? = null,
) : Parcelable

@Serializable
@Parcelize
internal data class TextUpdate(
    @SerialName("consent_pane")
    val consent: ConsentPane? = null
) : Parcelable

@Serializable
@Parcelize
internal data class VisualUpdate(
    // Indicates whether the logo should be removed from most panes
    @SerialName("reduced_branding")
    val reducedBranding: Boolean
) : Parcelable

@Serializable
@Parcelize
internal data class ConsentPane(
    @SerialName("above_cta")
    val aboveCta: String,
    @SerialName("below_cta")
    val belowCta: String? = null,
    @SerialName("body")
    val body: ConsentPaneBody,
    @SerialName("cta")
    val cta: String,
    @SerialName("data_access_notice")
    val dataAccessNotice: DataAccessNotice,
    @SerialName("legal_details_notice")
    val legalDetailsNotice: LegalDetailsNotice,
    @SerialName("title")
    val title: String
) : Parcelable

@Serializable
@Parcelize
internal data class ConsentPaneBody(
    @SerialName("bullets")
    val bullets: List<Bullet>
) : Parcelable

@Serializable
@Parcelize
internal data class DataAccessNoticeBody(
    @SerialName("bullets")
    val bullets: List<Bullet>
) : Parcelable

@Serializable
@Parcelize
internal data class Bullet(
    @SerialName("content")
    val content: String? = null,
    @SerialName("icon")
    val icon: Image? = null,
    @SerialName("title")
    val title: String? = null
) : Parcelable

@Serializable
@Parcelize
internal data class DataAccessNotice(
    @SerialName("body")
    val body: DataAccessNoticeBody,
    @SerialName("title")
    val title: String,
    @SerialName("cta")
    val cta: String,
    @SerialName("learn_more")
    val learnMore: String,
    @SerialName("connected_account_notice")
    val connectedAccountNotice: String? = null,
) : Parcelable

@Serializable
@Parcelize
internal data class LegalDetailsNotice(
    @SerialName("body")
    val body: LegalDetailsBody,
    @SerialName("title")
    val title: String,
    @SerialName("cta")
    val cta: String,
    @SerialName("learn_more")
    val learnMore: String,

) : Parcelable

@Serializable
@Parcelize
internal data class LegalDetailsBody(
    @SerialName("bullets")
    val bullets: List<Bullet>
) : Parcelable
