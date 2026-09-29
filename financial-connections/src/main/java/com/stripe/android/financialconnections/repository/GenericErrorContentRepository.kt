package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.di.ActivityRetainedScope
import com.stripe.android.financialconnections.model.GenericErrorPane
import com.stripe.android.financialconnections.repository.GenericErrorContentRepository.State
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Carries the server-driven [GenericErrorPane] to the pane that renders it. Navigation arguments
 * are strings only, so rich payloads travel through a repository instead.
 */
@ActivityRetainedScope
internal class GenericErrorContentRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : PersistingRepository<State>(
    savedStateHandle = savedStateHandle,
) {

    fun set(pane: GenericErrorPane) {
        set(State(pane))
    }

    @Parcelize
    data class State(
        val pane: GenericErrorPane? = null,
    ) : Parcelable
}
