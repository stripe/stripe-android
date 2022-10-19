@file:Suppress("MaximumLineLength", "MaxLineLength")

package com.stripe.android.financialconnections.model

// TODO@carlosmuvi DELETE.
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
