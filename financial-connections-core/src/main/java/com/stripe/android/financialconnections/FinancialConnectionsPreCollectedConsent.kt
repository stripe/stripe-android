package com.stripe.android.financialconnections

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Evidence of a user's consent to link a bank account, collected by the merchant prior to
 * presenting Financial Connections.
 *
 * @param consent the consent token returned by the pre-collection API (e.g. `fccons_123`).
 * @param collectedAt the Unix timestamp, in whole seconds, when the customer affirmatively accepted
 * the complete Stripe-issued consent text. Capture this timestamp once at acceptance and preserve it
 * across retries or sheet reopenings; do not substitute the time the consent was presented.
 *
 * Stripe evaluates this evidence and may still require the customer to accept consent in Financial
 * Connections. The SDK forwards both values exactly as provided and does not validate their format,
 * age, or plausibility.
 */
@Parcelize
@Poko
class FinancialConnectionsPreCollectedConsent(
    val consent: String,
    val collectedAt: Long,
) : Parcelable
