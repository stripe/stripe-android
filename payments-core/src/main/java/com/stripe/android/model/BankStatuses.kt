package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import com.stripe.android.view.Bank
import kotlinx.parcelize.Parcelize
import org.jetbrains.annotations.TestOnly

@Parcelize
internal data class BankStatuses internal constructor(
    private val statuses: Map<String, Boolean> = emptyMap()
) : StripeModel {
    @TestOnly
    internal fun size() = statuses.size

    /**
     * Defaults to `true` if statuses aren't available.
     */
    @JvmSynthetic
    internal fun isOnline(bank: Bank): Boolean {
        return statuses[bank.id] ?: true
    }
}
