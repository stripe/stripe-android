package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.financialconnections.model.serializer.MarkdownToHtmlSerializer
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
    val visual: VisualUpdate,
) : Parcelable

@Serializable
@Parcelize
internal data class TextUpdate(
    @SerialName("consent_pane")
    val consent: ConsentPane? = null,
    @SerialName("networking_link_signup_pane")
    val networkingLinkSignupPane: NetworkingLinkSignupPane? = null
) : Parcelable

@Serializable
@Parcelize
internal data class VisualUpdate(
    // Indicates whether the logo should be removed from most panes
    @SerialName("reduced_branding")
    val reducedBranding: Boolean,
    @SerialName("reduced_manual_entry_prominence_for_errors")
    val reducedManualEntryProminenceForErrors: Boolean,
    @SerialName("merchant_logo")
    val merchantLogos: List<String>
) : Parcelable

@Serializable
@Parcelize
internal data class ConsentPane(
    @SerialName("above_cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val aboveCta: String,
    @SerialName("below_cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val belowCta: String? = null,
    @SerialName("body")
    val body: ConsentPaneBody,
    @SerialName("cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val cta: String,
    @SerialName("data_access_notice")
    val dataAccessNotice: DataAccessNotice,
    @SerialName("legal_details_notice")
    val legalDetailsNotice: LegalDetailsNotice,
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String
) : Parcelable

@Serializable
@Parcelize
internal data class NetworkingLinkSignupPane(
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String,
    @SerialName("body")
    val body: NetworkingLinkSignupBody,
    @SerialName("above_cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val aboveCta: String,
    @SerialName("cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val cta: String,
    @SerialName("skip_cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val skipCta: String
) : Parcelable

@Serializable
@Parcelize
internal data class NetworkingLinkSignupBody(
    @SerialName("bullets")
    val bullets: List<Bullet>
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
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val content: String? = null,
    @SerialName("icon")
    val icon: Image? = null,
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String? = null
) : Parcelable

@Serializable
@Parcelize
internal data class DataAccessNotice(
    @SerialName("body")
    val body: DataAccessNoticeBody,
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String,
    @SerialName("subtitle")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val subtitle: String? = null,
    @SerialName("cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val cta: String,
    @SerialName("learn_more")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val learnMore: String,
    @SerialName("connected_account_notice")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val connectedAccountNotice: String? = null,
) : Parcelable

@Serializable
@Parcelize
internal data class LegalDetailsNotice(
    @SerialName("body")
    val body: LegalDetailsBody,
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String,
    @SerialName("cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val cta: String,
    @SerialName("learn_more")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val learnMore: String,

) : Parcelable

@Serializable
@Parcelize
internal data class LegalDetailsBody(
    @SerialName("bullets")
    val bullets: List<Bullet>
) : Parcelable
