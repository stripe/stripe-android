package com.stripe.android.financialconnections.utils

import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse

@Suppress("TestFunctionName")
internal fun GetOrFetchSync(
    action: (RefetchCondition) -> SynchronizeSessionResponse,
): GetOrFetchSync {
    return object : GetOrFetchSync {
        override suspend fun invoke(refetchCondition: RefetchCondition): SynchronizeSessionResponse {
            return action(refetchCondition)
        }
    }
}
