package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.repository.SuccessContentRepository.State
import com.stripe.android.financialconnections.ui.TextResource
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SuccessContentRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : PersistingRepository<State>(
    savedStateHandle = savedStateHandle,
) {

    fun set(
        message: TextResource?,
        heading: TextResource? = null
    ) {
        set(
            State(
                message = message,
                heading = heading
            )
        )
    }

    @Parcelize
    data class State(
        val message: TextResource?,
        val heading: TextResource?
    ) : Parcelable
}
