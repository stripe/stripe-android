package com.stripe.android.model

import com.stripe.android.view.FpxBank
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class FpxBankStatuses internal constructor(
    private val statuses: Map<String, Boolean> = emptyMap()
) : StripeModel {
    /**
     * Defaults to `true` if statuses aren't available.
     */
    @JvmSynthetic
    internal fun isOnline(bank: FpxBank): Boolean {
        return statuses[bank.id] ?: true
    }
}
