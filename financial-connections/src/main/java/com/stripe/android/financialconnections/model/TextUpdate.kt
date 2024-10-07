package com.stripe.android.financialconnections.model

import FinancialConnectionsGenericInfoScreen
import android.os.Parcelable
import com.stripe.android.financialconnections.model.serializer.EntrySerializer
import com.stripe.android.financialconnections.model.serializer.MarkdownToHtmlSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class TextUpdate(
    @SerialName("account_picker_pane")
    val accountPicker: AccountPickerPane? = null,
    @SerialName("consent_pane")
    val consent: ConsentPane? = null,
    @SerialName("link_login_pane")
    val linkLoginPane: LinkLoginPane? = null,
    @SerialName("networking_link_signup_pane")
    val networkingLinkSignupPane: NetworkingLinkSignupPane? = null,
    @SerialName("oauth_prepane")
    val oauthPrepane: OauthPrepane? = null,
    @SerialName("returning_networking_user_account_picker")
    val returningNetworkingUserAccountPicker: ReturningNetworkingUserAccountPicker? = null,
    @SerialName("success_pane")
    val successPane: SuccessPane? = null,
) : Parcelable

@Serializable
@Parcelize
internal data class AccountPickerPane(
    @SerialName("data_access_notice")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val dataAccessNotice: String,
) : Parcelable

@Serializable
@Parcelize
internal data class SuccessPane(
    @SerialName("caption")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val caption: String,
    @SerialName("sub_caption")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val subCaption: String,
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
    val dataAccessNotice: DataAccessNotice? = null,
    @SerialName("legal_details_notice")
    val legalDetailsNotice: LegalDetailsNotice,
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String
) : Parcelable

@Serializable
@Parcelize
internal data class OauthPrepane(
    @SerialName("body")
    val body: Body,
    @SerialName("cta")
    val cta: Cta,
    @SerialName("institution_icon")
    val institutionIcon: Image? = null,
    @SerialName("partner_notice")
    val partnerNotice: PartnerNotice? = null,
    @SerialName("data_access_notice")
    val dataAccessNotice: DataAccessNotice? = null,
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String,
    @SerialName("subtitle")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val subtitle: String
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
        internal const val TYPE_IMAGE = "image"
    }
}

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
    val skipCta: String,
    @SerialName("legal_details_notice")
    val legalDetailsNotice: LegalDetailsNotice? = null
) : Parcelable

@Serializable
@Parcelize
internal data class NetworkingLinkSignupBody(
    @SerialName("bullets")
    val bullets: List<Bullet>
) : Parcelable

@Serializable
@Parcelize
internal data class LinkLoginPane(
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String,
    @SerialName("body")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val body: String,
    @SerialName("above_cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val aboveCta: String,
    @SerialName("cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val cta: String,
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
    @SerialName("icon")
    val icon: Image? = null,
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String,
    @SerialName("subtitle")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val subtitle: String? = null,
    @SerialName("body")
    val body: DataAccessNoticeBody,
    @SerialName("connected_account_notice")
    val connectedAccountNotice: ConnectedAccessNotice? = null,
    @SerialName("disclaimer")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val disclaimer: String? = null,
    @SerialName("cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val cta: String,
) : Parcelable

@Serializable
@Parcelize
internal data class ConnectedAccessNotice(
    @SerialName("subtitle")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val subtitle: String,
    @SerialName("body")
    val body: DataAccessNoticeBody
) : Parcelable

@Serializable
@Parcelize
internal data class LegalDetailsNotice(
    @SerialName("icon")
    val icon: Image? = null,
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String,
    @SerialName("subtitle")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val subtitle: String? = null,
    @SerialName("body")
    val body: LegalDetailsBody,
    @SerialName("cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val cta: String,
    @SerialName("disclaimer")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val disclaimer: String? = null
) : Parcelable

@Serializable
@Parcelize
internal data class LegalDetailsBody(
    @SerialName("links")
    val links: List<ServerLink>
) : Parcelable

@Serializable
@Parcelize
internal data class ServerLink(
    @SerialName("title")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val title: String,
    @SerialName("content")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val content: String? = null
) : Parcelable

@Serializable
@Parcelize
internal data class ReturningNetworkingUserAccountPicker(
    @SerialName("title") val title: String,
    @SerialName("default_cta") val defaultCta: String,
    @SerialName("add_new_account") val addNewAccount: AddNewAccount,
    @SerialName("accounts") val accounts: List<NetworkedAccount>,
    @SerialName("above_cta")
    @Serializable(with = MarkdownToHtmlSerializer::class)
    val aboveCta: String? = null,
    @SerialName("multiple_account_types_selected_data_access_notice")
    val multipleAccountTypesSelectedDataAccessNotice: DataAccessNotice? = null
) : Parcelable

@Serializable
@Parcelize
internal data class NetworkedAccount(
    @SerialName("id") val id: String,
    @SerialName("allow_selection") val allowSelection: Boolean,
    @SerialName("caption") val caption: String? = null,
    @SerialName("selection_cta") val selectionCta: String? = null,
    @SerialName("icon") val icon: Image? = null,
    @SerialName("selection_cta_icon") val selectionCtaIcon: Image? = null,
    @SerialName("account_icon") val accountIcon: Image? = null,
    @SerialName("data_access_notice") val dataAccessNotice: DataAccessNotice? = null,
    @SerialName("drawer_on_selection") val drawerOnSelection: FinancialConnectionsGenericInfoScreen? = null
) : Parcelable

@Serializable
@Parcelize
internal data class AddNewAccount(
    @SerialName("body") val body: String,
    @SerialName("icon") val icon: Image? = null,
) : Parcelable
