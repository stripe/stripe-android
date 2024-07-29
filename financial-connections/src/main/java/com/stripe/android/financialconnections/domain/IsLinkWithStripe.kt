package com.stripe.android.financialconnections.domain

import javax.inject.Inject

internal fun interface IsLinkWithStripe {
    suspend operator fun invoke(): Boolean
}

internal class RealIsLinkWithStripe @Inject constructor(
    private val getOrFetchSync: GetOrFetchSync,
) : IsLinkWithStripe {

    override suspend operator fun invoke(): Boolean {
        return getOrFetchSync().manifest.isLinkWithStripe ?: false
    }
}
