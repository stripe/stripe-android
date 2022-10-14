package com.stripe.android.financialconnections.model

import kotlinx.serialization.SerialName

@Suppress("MaximumLineLength", "MaxLineLength")
// TODO DELETE.
internal val sampleConsent: ConsentPane =
    ConsentPane(
        title = "Goldilocks works with **Stripe** to link your accounts",
        body = ConsentPaneBody(
            bullets = listOf(
                ConsentPaneBullet(
                    icon = Image(default = "https://www.cdn.stripe.com/12321312321.png"),
                    content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                ),
                ConsentPaneBullet(
                    icon = Image(default = "https://www.cdn.stripe.com/12321312321.png"),
                    content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                ),
                ConsentPaneBullet(
                    icon = Image(default = "https://www.cdn.stripe.com/12321312321.png"),
                    content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                ),
            )
        ),
        aboveCta = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
        belowCta = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
        cta = "Agree",
        dataAccessNotice = DataAccessNotice(
            title = "Goldilocks works with **Stripe** to link your accounts",
            body = DataAccessNoticeBody(
                bullets = listOf(
                    DataAccessNoticeBullet(
                        icon = Image(default = "https://www.cdn.stripe.com/12321312321.png"),
                        title = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                        content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                    ),
                    DataAccessNoticeBullet(
                        icon = Image(default = "https://www.cdn.stripe.com/12321312321.png"),
                        title = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                        content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                    ),
                    DataAccessNoticeBullet(
                        icon = Image(default = "https://www.cdn.stripe.com/12321312321.png"),
                        title = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                        content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                    ),
                )
            ),
            content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
            cta = "OK"
        )
    )

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
)

internal data class ConsentPaneBody(
    @SerialName("bullets")
    val bullets: List<ConsentPaneBullet>
)

internal data class DataAccessNotice(
    @SerialName("body")
    val body: DataAccessNoticeBody,
    @SerialName("content")
    val content: String,
    @SerialName("title")
    val title: String,
    @SerialName("cta")
    val cta: String,
)

internal data class ConsentPaneBullet(
    @SerialName("content")
    val content: String,
    @SerialName("icon")
    val icon: Image
)

internal data class DataAccessNoticeBody(
    @SerialName("bullets")
    val bullets: List<DataAccessNoticeBullet>
)

internal data class DataAccessNoticeBullet(
    @SerialName("content")
    val content: String,
    @SerialName("icon")
    val icon: Image,
    @SerialName("title")
    val title: String
)
