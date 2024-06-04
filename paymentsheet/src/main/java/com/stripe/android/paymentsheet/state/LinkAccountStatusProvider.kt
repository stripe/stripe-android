package com.stripe.android.paymentsheet.state

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.model.AccountStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal fun interface LinkAccountStatusProvider {
    suspend operator fun invoke(configuration: LinkConfiguration): AccountStatus
}

internal class DefaultLinkAccountStatusProvider @Inject constructor(
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
) : LinkAccountStatusProvider {

    override suspend fun invoke(configuration: LinkConfiguration): AccountStatus {
        return linkConfigurationCoordinator.getAccountStatusFlow(configuration).first()
    }
}
