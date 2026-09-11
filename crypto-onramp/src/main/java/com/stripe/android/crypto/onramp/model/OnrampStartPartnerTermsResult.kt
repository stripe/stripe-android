package com.stripe.android.crypto.onramp.model

import com.stripe.android.link.LinkAppearance

internal sealed interface OnrampStartPartnerTermsResult {
    class PresentationRequired internal constructor(
        val terms: PartnerTerms.Required,
        val appearance: LinkAppearance?,
    ) : OnrampStartPartnerTermsResult

    data object NotRequired : OnrampStartPartnerTermsResult

    class Failed internal constructor(
        val error: Throwable
    ) : OnrampStartPartnerTermsResult
}
