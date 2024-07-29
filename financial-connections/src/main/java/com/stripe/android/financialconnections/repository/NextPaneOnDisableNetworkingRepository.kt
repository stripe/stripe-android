package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NextPaneOnDisableNetworkingRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : PersistingRepository<NextPaneOnDisableNetworkingRepository.State>(
    savedStateHandle = savedStateHandle,
) {

    fun set(nextPane: String?) {
        set(State(nextPane))
    }

    @Parcelize
    data class State(
        val nextPane: String?,
    ) : Parcelable
}
