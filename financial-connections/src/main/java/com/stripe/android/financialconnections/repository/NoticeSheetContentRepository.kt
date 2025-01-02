package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.di.ActivityRetainedScope
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent
import com.stripe.android.financialconnections.repository.NoticeSheetContentRepository.State
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@ActivityRetainedScope
internal class NoticeSheetContentRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : PersistingRepository<State>(
    savedStateHandle = savedStateHandle,
) {

    fun set(content: NoticeSheetContent) {
        set(State(content))
    }

    @Parcelize
    data class State(
        val content: NoticeSheetContent? = null,
    ) : Parcelable
}
