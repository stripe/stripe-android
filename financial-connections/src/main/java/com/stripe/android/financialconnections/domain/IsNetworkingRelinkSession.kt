package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository
import javax.inject.Inject

internal fun interface IsNetworkingRelinkSession {
    operator fun invoke(): Boolean
}

internal class RealIsNetworkingRelinkSession @Inject constructor(
    private val pendingRepairRepository: CoreAuthorizationPendingNetworkingRepairRepository,
) : IsNetworkingRelinkSession {

    override fun invoke(): Boolean {
        return pendingRepairRepository.get() != null
    }
}
