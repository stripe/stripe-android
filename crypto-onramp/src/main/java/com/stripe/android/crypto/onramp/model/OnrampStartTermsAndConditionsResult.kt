package com.stripe.android.crypto.onramp.model

import com.stripe.android.link.LinkAppearance

internal sealed interface OnrampStartTermsAndConditionsResult {
    class PresentationRequired internal constructor(
        val terms: PartnerTerms.Required,
        val appearance: LinkAppearance?,
    ) : OnrampStartTermsAndConditionsResult

    data object NotRequired : OnrampStartTermsAndConditionsResult

    class Failed internal constructor(
        val error: Throwable
    ) : OnrampStartTermsAndConditionsResult
}
