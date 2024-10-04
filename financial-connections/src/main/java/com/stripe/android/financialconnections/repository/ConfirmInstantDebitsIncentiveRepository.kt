package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ConfirmInstantDebitsIncentiveRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : PersistingRepository<ConfirmInstantDebitsIncentiveRepository.State>(
    savedStateHandle = savedStateHandle,
) {

    fun set(shouldConfirm: Boolean) {
        set(State(shouldConfirm))
    }

    @Parcelize
    data class State(
        val shouldConfirm: Boolean,
    ) : Parcelable
}
