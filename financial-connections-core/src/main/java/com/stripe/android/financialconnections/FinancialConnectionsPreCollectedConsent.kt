package com.stripe.android.financialconnections

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Evidence of a user's consent to link a bank account, collected by the merchant prior to
 * presenting Financial Connections.
 *
 * @param consent the consent token returned by the pre-collection API (e.g. `fccons_123`).
 */
@Parcelize
data class FinancialConnectionsPreCollectedConsent(
    val consent: String,
) : Parcelable
