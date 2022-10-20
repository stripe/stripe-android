@file:Suppress("MaximumLineLength", "MaxLineLength")

package com.stripe.android.financialconnections.model

internal val sampleConsent: ConsentPane =
    ConsentPane(
        title = "Goldilocks works with **Stripe** to link your accounts",
        body = ConsentPaneBody(
            bullets = listOf(
                Bullet(
                    icon = "https://www.cdn.stripe.com/12321312321.png",
                    content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                    title = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                ),
                Bullet(
                    icon = "https://www.cdn.stripe.com/12321312321.png",
                    content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                    title = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                ),
                Bullet(
                    icon = "https://www.cdn.stripe.com/12321312321.png",
                    content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                    title = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
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
                    Bullet(
                        icon = "https://www.cdn.stripe.com/12321312321.png",
                        title = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                        content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                    ),
                    Bullet(
                        icon = "https://www.cdn.stripe.com/12321312321.png",
                        title = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                        content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                    ),
                    Bullet(
                        icon = "https://www.cdn.stripe.com/12321312321.png",
                        title = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
                        content = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them."
                    ),
                )
            ),
            learnMore = "Stripe will allow **Goldilocks** to access only the [data requested](stripe://data-access-notice). We never share your login details with them.",
            cta = "OK"
        )
    )
