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
    @SerialName("consent")
    val consent: ConsentPane? = null
) : Parcelable

@Serializable
@Parcelize
internal data class ConsentPane(
    @SerialName("above_cta")
    val aboveCta: String,
    @SerialName("below_cta")
    val belowCta: String,
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
internal data class DataAccessNoticeBullet(
    @SerialName("content")
    val content: String,
    @SerialName("icon")
    val icon: Image,
    @SerialName("title")
    val title: String
) : Parcelable

@Serializable
@Parcelize
internal data class DataAccessNoticeBody(
    @SerialName("bullets")
    val bullets: List<DataAccessNoticeBullet>
) : Parcelable

@Serializable
@Parcelize
internal data class ConsentPaneBullet(
    @SerialName("content")
    val content: String,
    @SerialName("icon")
    val icon: Image
) : Parcelable

@Serializable
@Parcelize
internal data class DataAccessNotice(
    @SerialName("body")
    val body: DataAccessNoticeBody,
    @SerialName("content")
    val content: String,
    @SerialName("title")
    val title: String,
    @SerialName("cta")
    val cta: String,
) : Parcelable

@Serializable
@Parcelize
internal data class ConsentPaneBody(
    @SerialName("bullets")
    val bullets: List<ConsentPaneBullet>
) : Parcelable
