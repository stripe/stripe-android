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
    @SerialName("mobile")
    val mobile: String? = null,
    @SerialName("text")
    val text: TextUpdate? = null
) : Parcelable

@Serializable
@Parcelize
internal data class TextUpdate(
    @SerialName("consent_pane")
    val consent: ConsentPane? = null
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
    @SerialName("title")
    val title: String
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
    val content: String,
    @SerialName("icon")
    val icon: String,
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
) : Parcelable

@Serializable
@Parcelize
internal data class ConsentPaneBody(
    @SerialName("bullets")
    val bullets: List<Bullet>
) : Parcelable
