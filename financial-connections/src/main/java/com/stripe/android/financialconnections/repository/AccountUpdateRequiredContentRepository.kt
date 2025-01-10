package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.di.ActivityRetainedScope
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.UpdateRequired
import com.stripe.android.financialconnections.repository.AccountUpdateRequiredContentRepository.State
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@ActivityRetainedScope
internal class AccountUpdateRequiredContentRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : PersistingRepository<State>(
    savedStateHandle = savedStateHandle,
) {

    fun set(payload: UpdateRequired) {
        set(State(payload))
    }

    @Parcelize
    data class State(
        val payload: UpdateRequired,
    ) : Parcelable
}
