package com.stripe.android.paymentsheet.featuredinstitutions

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.core.networking.ApiRequest
import kotlinx.coroutines.CompletableDeferred

internal class FakeFeaturedInstitutionsRepository(
    var result: Result<FeaturedInstitutionList> = Result.success(FeaturedInstitutionList(emptyList())),
    private val responseGate: CompletableDeferred<Unit>? = null,
) : FeaturedInstitutionsRepository {
    private val _retrieveCalls = Turbine<RetrieveCall>()
    val retrieveCalls: ReceiveTurbine<RetrieveCall> = _retrieveCalls

    override suspend fun retrieveFeaturedInstitutions(
        requestOptions: ApiRequest.Options,
        country: String?,
    ): Result<FeaturedInstitutionList> {
        _retrieveCalls.add(RetrieveCall(requestOptions, country))
        responseGate?.await()
        return result
    }

    fun ensureAllEventsConsumed() {
        _retrieveCalls.ensureAllEventsConsumed()
    }

    data class RetrieveCall(
        val requestOptions: ApiRequest.Options,
        val country: String?,
    )
}
