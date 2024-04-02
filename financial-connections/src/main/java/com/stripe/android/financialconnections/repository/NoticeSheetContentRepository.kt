package com.stripe.android.financialconnections.repository

import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent
import com.stripe.android.financialconnections.repository.NoticeSheetContentRepository.State
import javax.inject.Inject

internal interface NoticeSheetContentRepository {
    suspend fun get(): State
    fun update(reducer: State.() -> State)

    data class State(
        val content: NoticeSheetContent? = null,
    )
}

internal class RealNoticeSheetContentRepository @Inject constructor() : NoticeSheetContentRepository {

    private var state = State()

    override suspend fun get() = state

    override fun update(reducer: State.() -> State) {
        state = reducer(state)
    }
}
