package com.stripe.android.analytics

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.stripe.android.core.networking.AnalyticsRequestFactory
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object SessionSavedStateHandler {
    @VisibleForTesting
    internal const val SESSION_KEY = "STRIPE_ANALYTICS_LOCAL_SESSION"

    private var sessionLocked: Boolean = false

    /**
     * Attaches a [Session] to a [ViewModel]'s lifecycle and its provided [SavedStateHandle]. This should be called
     * in the first line of the `init` call in the [ViewModel].
     *
     * @param viewModel used to attach a session to its lifecycle.
     * @param savedStateHandle used to store & restore [Session] data.
     *
     * @return a function for manually restarting the session if needed.
     */
    fun attachTo(viewModel: ViewModel, savedStateHandle: SavedStateHandle): () -> Unit {
        startSession(savedStateHandle)

        viewModel.addCloseable {
            clearSession(savedStateHandle)
        }

        return {
            restartSession(savedStateHandle)
        }
    }

    private fun startSession(savedStateHandle: SavedStateHandle) {
        savedStateHandle.get<Session>(SESSION_KEY)?.let { session ->
            session.also { storedSession ->
                when (storedSession) {
                    is Session.Owner -> {
                        AnalyticsRequestFactory.setSessionId(UUID.fromString(storedSession.id))
                        sessionLocked = true
                    }
                    is Session.Observer -> Unit
                }
            }
        } ?: run {
            val session = if (!sessionLocked) {
                sessionLocked = true

                val id = UUID.randomUUID()

                AnalyticsRequestFactory.setSessionId(id)

                Session.Owner(id.toString())
            } else {
                Session.Observer
            }

            savedStateHandle[SESSION_KEY] = session
        }
    }

    private fun restartSession(savedStateHandle: SavedStateHandle) {
        savedStateHandle.get<Session>(SESSION_KEY)?.let { session ->
            session.also { storedSession ->
                when (storedSession) {
                    is Session.Owner -> {
                        val id = UUID.randomUUID()

                        AnalyticsRequestFactory.setSessionId(id)

                        savedStateHandle[SESSION_KEY] = Session.Owner(id.toString())
                    }
                    is Session.Observer -> Unit
                }
            }
        }
    }

    private fun clearSession(savedStateHandle: SavedStateHandle) {
        savedStateHandle.get<Session>(SESSION_KEY)?.let { session ->
            session.also { storedSession ->
                when (storedSession) {
                    is Session.Owner -> {
                        sessionLocked = false
                    }
                    is Session.Observer -> Unit
                }
            }
        }
    }

    @VisibleForTesting
    fun clear() {
        sessionLocked = false
    }
}
