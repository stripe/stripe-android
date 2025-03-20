package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject
import javax.inject.Named

/**
 * Retrieves the current cached [SynchronizeSessionResponse] instance, or fetches
 * it from backend if no cached version available.
 */
internal class GetOrFetchSync @Inject constructor(
    val repository: FinancialConnectionsManifestRepository,
    val configuration: FinancialConnectionsSheetConfiguration,
    @Named(APPLICATION_ID) private val applicationId: String,
) {

    suspend operator fun invoke(
        refetchCondition: RefetchCondition = RefetchCondition.None,
        supportsAppVerification: Boolean = false
    ): SynchronizeSessionResponse {
        return repository.getOrSynchronizeFinancialConnectionsSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            applicationId = applicationId,
            reFetchCondition = refetchCondition::shouldReFetch,
            supportsAppVerification = supportsAppVerification
        )
    }

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
