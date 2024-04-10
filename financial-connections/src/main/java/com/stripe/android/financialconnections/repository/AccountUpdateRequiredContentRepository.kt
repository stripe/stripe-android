package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.features.accountupdate.AccountUpdateRequiredState
import com.stripe.android.financialconnections.repository.AccountUpdateRequiredContentRepository.State
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AccountUpdateRequiredContentRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : PersistingRepository<State>(
    savedStateHandle = savedStateHandle,
) {

    fun set(payload: AccountUpdateRequiredState.Payload) {
        set(State(payload))
    }

    @Parcelize
    data class State(
        val payload: AccountUpdateRequiredState.Payload,
    ) : Parcelable
}
