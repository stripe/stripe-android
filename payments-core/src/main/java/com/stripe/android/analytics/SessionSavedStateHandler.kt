package com.stripe.android.analytics

import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.networking.AnalyticsRequestFactory
import org.jetbrains.annotations.VisibleForTesting
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object SessionSavedStateHandler {
    @VisibleForTesting
    internal const val SESSION_KEY = "STRIPE_ANALYTICS_LOCAL_SESSION"

    private var sessionLocked: Boolean = false

    fun startSession(savedStateHandle: SavedStateHandle) {
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

    fun restartSession(savedStateHandle: SavedStateHandle) {
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

    fun clearSession(savedStateHandle: SavedStateHandle) {
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

    fun clear() {
        sessionLocked = false
    }
}
