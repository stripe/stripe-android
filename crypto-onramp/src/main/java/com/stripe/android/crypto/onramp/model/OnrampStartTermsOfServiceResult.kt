package com.stripe.android.crypto.onramp.model

import com.stripe.android.link.LinkAppearance

internal sealed interface OnrampStartTermsOfServiceResult {
    class PresentationRequired internal constructor(
        val terms: PartnerTerms.Required,
        val appearance: LinkAppearance?,
    ) : OnrampStartTermsOfServiceResult

    data object NotRequired : OnrampStartTermsOfServiceResult

    class Failed internal constructor(
        val error: Throwable
    ) : OnrampStartTermsOfServiceResult
}
