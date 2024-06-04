package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.repository.FinancialConnectionsErrorRepository.State
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FinancialConnectionsErrorRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : PersistingRepository<State>(
    savedStateHandle = savedStateHandle,
) {

    fun set(error: Throwable) {
        set(State(error))
    }

    @Parcelize
    data class State(
        val error: Throwable,
    ) : Parcelable
}
