package com.stripe.android.financialconnections.model

import kotlinx.serialization.SerialName

@Suppress("MaximumLineLength", "MaxLineLength")
// TODO DELETE.
internal val sampleConsent = ConsentScreen(
    title = "Goldilocks works with **Stripe** to link your accounts",
    body = listOf(
        Body(
            iconUrl = "https://www.cdn.stripe.com/12321312321.png",
            text = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
        ),
        Body(
            iconUrl = "https://www.cdn.stripe.com/12321312321.png",
            text = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
        ),
        Body(
            iconUrl = "https://www.cdn.stripe.com/12321312321.png",
            text = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
        ),
    ),
    footerText = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
    buttonTitle = "Agree",
    dataDialog = DataDialog(
        title = "Goldilocks works with **Stripe** to link your accounts",
        body = listOf(
            Body(
                iconUrl = "https://www.cdn.stripe.com/12321312321.png",
                text = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                subtext = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
            ),
            Body(
                iconUrl = "https://www.cdn.stripe.com/12321312321.png",
                text = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                subtext = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
            ),
            Body(
                iconUrl = "https://www.cdn.stripe.com/12321312321.png",
                text = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                subtext = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
            ),
        ),
        footerText = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
        buttonTitle = "Agree",
    )
)

internal data class ConsentScreen(
    @SerialName("title") val title: String,
    @SerialName("body") val body: List<Body>,
    @SerialName("footer_text") val footerText: String,
    @SerialName("button_title") val buttonTitle: String,
    @SerialName("data_dialog") val dataDialog: DataDialog
)

internal data class DataDialog(
    @SerialName("title") val title: String,
    @SerialName("body") val body: List<Body>,
    @SerialName("footer_text") val footerText: String,
    @SerialName("button_title") val buttonTitle: String,
)

internal data class Body(
    @SerialName("icon_url") val iconUrl: String,
    @SerialName("text") val text: String,
    @SerialName("subtext") val subtext: String? = null
)
