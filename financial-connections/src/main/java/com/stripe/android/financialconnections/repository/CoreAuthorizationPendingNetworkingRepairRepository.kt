package com.stripe.android.financialconnections.repository

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository.State
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for storing the core authorization pending repair.
 */
@Singleton
internal class CoreAuthorizationPendingNetworkingRepairRepository @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val logger: Logger,
) : PersistingRepository<State>(
    savedStateHandle = savedStateHandle,
) {

    fun set(coreAuthorization: String) {
        logger.debug("core authorization set to $coreAuthorization")
        set(State(coreAuthorization))
    }

    @Parcelize
    data class State(
        val coreAuthorization: String? = null
    ) : Parcelable
}
