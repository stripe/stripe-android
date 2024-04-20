package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
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
    val configuration: FinancialConnectionsSheet.Configuration,
    @Named(APPLICATION_ID) private val applicationId: String,
) {

    suspend operator fun invoke(
        fetchCondition: FetchCondition = FetchCondition.IfMissing
    ): SynchronizeSessionResponse {
        return repository.getOrSynchronizeFinancialConnectionsSession(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            applicationId = applicationId,
            fetchCondition = fetchCondition::check,
        )
    }

    sealed interface FetchCondition {
        fun check(response: SynchronizeSessionResponse): Boolean

        /**
         * Session won't be fetched if it's already cached.
         */
        data object IfMissing : FetchCondition {
            override fun check(response: SynchronizeSessionResponse): Boolean {
                return false
            }
        }

        /**
         * Session will always be fetched, even if a cached version exists.
         */
        data object Always : FetchCondition {
            override fun check(response: SynchronizeSessionResponse): Boolean {
                return true
            }
        }

        /**
         * Session will be fetched only if there's no active auth session on the cached manifest.
         */
        data object MissingActiveAuthSession : FetchCondition {
            override fun check(response: SynchronizeSessionResponse): Boolean {
                return response.manifest.activeAuthSession == null
            }
        }
    }
}
