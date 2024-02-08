package com.stripe.android.analytics

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import java.util.UUID

class SessionSavedStateHandlerTest {
    @After
    fun teardown() {
        SessionSavedStateHandler.clear()
    }

    @Test
    fun `initial call to startSession() should save session as owner`() {
        val savedStateHandle = SavedStateHandle()

        SessionSavedStateHandler.startSession(savedStateHandle)

        assertThat(
            savedStateHandle.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Owner::class.java)
    }

    @Test
    fun `subsequent calls to startSession() after initial session should save session as observer`() {
        val savedStateHandle1 = SavedStateHandle()
        val savedStateHandle2 = SavedStateHandle()
        val savedStateHandle3 = SavedStateHandle()

        SessionSavedStateHandler.startSession(SavedStateHandle())
        SessionSavedStateHandler.startSession(savedStateHandle1)
        SessionSavedStateHandler.startSession(savedStateHandle2)
        SessionSavedStateHandler.startSession(savedStateHandle3)

        assertThat(
            savedStateHandle1.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Observer::class.java)

        assertThat(
            savedStateHandle2.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Observer::class.java)

        assertThat(
            savedStateHandle3.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Observer::class.java)
    }

    @Test
    fun `if startSession() called with previous owner session data, should maintain observer session`() {
        val id = UUID.randomUUID().toString()
        val savedStateHandle = SavedStateHandle().apply {
            set(SessionSavedStateHandler.SESSION_KEY, Session.Owner(id))
        }

        SessionSavedStateHandler.startSession(savedStateHandle)

        assertThat(
            savedStateHandle.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isEqualTo(Session.Owner(id))
    }

    @Test
    fun `if startSession() called with previous owner session data, should maintain owner session`() {
        val savedStateHandle = SavedStateHandle().apply {
            set(SessionSavedStateHandler.SESSION_KEY, Session.Observer)
        }

        SessionSavedStateHandler.startSession(savedStateHandle)

        assertThat(
            savedStateHandle.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Observer::class.java)
    }

    @Test
    fun `if restartSession() called by owner, should regenerate session id`() {
        val savedStateHandle = SavedStateHandle()

        SessionSavedStateHandler.startSession(savedStateHandle)

        val currentSessionOwner = savedStateHandle.get<Session.Owner>(SessionSavedStateHandler.SESSION_KEY)

        SessionSavedStateHandler.restartSession(savedStateHandle)

        val newSessionOwner = savedStateHandle.get<Session.Owner>(SessionSavedStateHandler.SESSION_KEY)

        assertThat(currentSessionOwner).isNotEqualTo(newSessionOwner)
    }

    @Test
    fun `if restartSession() called by observer, should not regenerate session id`() {
        val savedStateHandle = SavedStateHandle()

        SessionSavedStateHandler.startSession(savedStateHandle)

        val currentSessionOwner = savedStateHandle.get<Session.Owner>(SessionSavedStateHandler.SESSION_KEY)

        SessionSavedStateHandler.restartSession(savedStateHandle)

        val newSessionOwner = savedStateHandle.get<Session.Owner>(SessionSavedStateHandler.SESSION_KEY)

        assertThat(currentSessionOwner).isNotEqualTo(newSessionOwner)
    }

    @Test
    fun `if clearSession() called by owner, should unlock session`() {
        val savedStateHandle = SavedStateHandle()

        SessionSavedStateHandler.startSession(savedStateHandle)
        SessionSavedStateHandler.clearSession(savedStateHandle)

        val newSavedStateHandle = SavedStateHandle()

        SessionSavedStateHandler.startSession(newSavedStateHandle)

        assertThat(
            newSavedStateHandle.get<Session.Owner>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Owner::class.java)
    }
}
