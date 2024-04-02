package com.stripe.android.financialconnections.repository

import com.stripe.android.financialconnections.model.DataAccessNotice
import com.stripe.android.financialconnections.model.LegalDetailsNotice
import com.stripe.android.financialconnections.repository.StaticSheetContentRepository.State
import javax.inject.Inject

internal interface StaticSheetContentRepository {
    suspend fun get(): State
    fun update(reducer: State.() -> State)

    data class State(
        val content: StaticSheetContent? = null,
    )
}

internal sealed interface StaticSheetContent {

    data class Legal(
        val legalDetails: LegalDetailsNotice,
    ) : StaticSheetContent

    data class DataAccess(
        val dataAccess: DataAccessNotice,
    ) : StaticSheetContent
}

internal class RealStaticSheetContentRepository @Inject constructor() : StaticSheetContentRepository {

    private var state = State()

    override suspend fun get() = state

    override fun update(reducer: State.() -> State) {
        state = reducer(state)
    }
}
