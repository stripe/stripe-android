package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject
import javax.inject.Named

/**
 * Retrieves the current cached [SynchronizeSessionResponse] instance, or fetches
 * it from backend if no cached version available.
 */
internal interface GetOrFetchSync {

    suspend operator fun invoke(
        refetchCondition: RefetchCondition = RefetchCondition.None,
    ): SynchronizeSessionResponse

    sealed interface RefetchCondition {
        fun shouldReFetch(response: SynchronizeSessionResponse): Boolean

        /**
         * Session won't be re-fetched if it's already cached.
         */
        data object None : RefetchCondition {
            override fun shouldReFetch(response: SynchronizeSessionResponse): Boolean {
                return false
            }
        }

        /**
         * Session will always be fetched, even if a cached version exists.
         */
        data object Always : RefetchCondition {
            override fun shouldReFetch(response: SynchronizeSessionResponse): Boolean {
                return true
            }
        }

        /**
         * Session will be fetched only if there's no active auth session on the cached manifest.
         */
        data object IfMissingActiveAuthSession : RefetchCondition {
            override fun shouldReFetch(response: SynchronizeSessionResponse): Boolean {
                return response.manifest.activeAuthSession == null
            }
        }
    }
}

internal class RealGetOrFetchSync @Inject constructor(
    private val repository: FinancialConnectionsManifestRepository,
    private val configuration: FinancialConnectionsSheet.Configuration,
    @Named(APPLICATION_ID) private val applicationId: String,
) : GetOrFetchSync {

    override suspend operator fun invoke(
        refetchCondition: RefetchCondition,
    ): SynchronizeSessionResponse {
        return repository.getOrSynchronizeFinancialConnectionsSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            applicationId = applicationId,
            reFetchCondition = refetchCondition::shouldReFetch,
        )
    }
}
