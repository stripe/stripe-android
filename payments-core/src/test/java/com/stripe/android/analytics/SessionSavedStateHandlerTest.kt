package com.stripe.android.analytics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.testing.SessionTestRule
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class SessionSavedStateHandlerTest {
    @get:Rule
    val sessionRule = SessionTestRule()

    @Test
    fun `initial call to attachTo() should save session as owner & set session id`() {
        val viewModel = FakeViewModel()

        SessionSavedStateHandler.attachTo(viewModel, viewModel.savedStateHandle)

        val owner = viewModel.savedStateHandle.get<Session>(SessionSavedStateHandler.SESSION_KEY)

        assertThat(owner).isInstanceOf(Session.Owner::class.java)
        assertThat(UUID.fromString((owner as Session.Owner).id)).isEqualTo(AnalyticsRequestFactory.sessionId)
    }

    @Test
    fun `subsequent calls to attachTo() after initial session should save session as observer`() {
        val viewModel1 = FakeViewModel()
        val viewModel2 = FakeViewModel()
        val viewModel3 = FakeViewModel()
        val viewModel4 = FakeViewModel()

        SessionSavedStateHandler.attachTo(viewModel1, viewModel1.savedStateHandle)
        SessionSavedStateHandler.attachTo(viewModel2, viewModel2.savedStateHandle)
        SessionSavedStateHandler.attachTo(viewModel3, viewModel3.savedStateHandle)
        SessionSavedStateHandler.attachTo(viewModel4, viewModel4.savedStateHandle)

        assertThat(
            viewModel2.savedStateHandle.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Observer::class.java)

        assertThat(
            viewModel3.savedStateHandle.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Observer::class.java)

        assertThat(
            viewModel4.savedStateHandle.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Observer::class.java)
    }

    @Test
    fun `if attachTo() called with previous owner session data, should maintain owner session`() {
        val id = UUID.randomUUID().toString()

        val viewModel = FakeViewModel().apply {
            savedStateHandle.apply {
                set(SessionSavedStateHandler.SESSION_KEY, Session.Owner(id))
            }
        }

        SessionSavedStateHandler.attachTo(viewModel, viewModel.savedStateHandle)

        assertThat(
            viewModel.savedStateHandle.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isEqualTo(Session.Owner(id))

        assertThat(
            AnalyticsRequestFactory.sessionId
        ).isEqualTo(UUID.fromString(id))
    }

    @Test
    fun `if attachTo() called with previous observer session data, should maintain observer session`() {
        val viewModel = FakeViewModel().apply {
            savedStateHandle.apply {
                set(SessionSavedStateHandler.SESSION_KEY, Session.Observer)
            }
        }

        SessionSavedStateHandler.attachTo(viewModel, viewModel.savedStateHandle)

        assertThat(
            viewModel.savedStateHandle.get<Session>(SessionSavedStateHandler.SESSION_KEY)
        ).isEqualTo(Session.Observer)
    }

    @Test
    fun `if session restart called by owner, should regenerate session id`() {
        val viewModel = FakeViewModel()

        val restartSession = SessionSavedStateHandler.attachTo(viewModel, viewModel.savedStateHandle)

        val sessionId = AnalyticsRequestFactory.sessionId

        restartSession()

        val newSessionId = AnalyticsRequestFactory.sessionId

        assertThat(sessionId).isNotEqualTo(newSessionId)
    }

    @Test
    fun `if session restart called by observer, should not regenerate session id`() {
        val owner = FakeViewModel()

        SessionSavedStateHandler.attachTo(owner, owner.savedStateHandle)

        val observer = FakeViewModel()

        val restartSession = SessionSavedStateHandler.attachTo(observer, observer.savedStateHandle)

        val sessionId = AnalyticsRequestFactory.sessionId

        restartSession()

        val newSessionId = AnalyticsRequestFactory.sessionId

        assertThat(sessionId).isEqualTo(newSessionId)
    }

    @Test
    fun `if onCleared() called by session owner, should unlock session`() {
        val viewModelStore = ViewModelStore()
        val viewModel = FakeViewModel()

        viewModelStore.put("FakeViewModel", viewModel)
        SessionSavedStateHandler.attachTo(viewModel, viewModel.savedStateHandle)
        viewModelStore.clear()

        val newViewModel = FakeViewModel()

        SessionSavedStateHandler.attachTo(newViewModel, newViewModel.savedStateHandle)

        assertThat(
            newViewModel.savedStateHandle.get<Session.Owner>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Owner::class.java)
    }

    @Test
    fun `if onCleared() called, should not remove session data from handler`() {
        val viewModelStore = ViewModelStore()
        val viewModel = FakeViewModel()

        viewModelStore.put("FakeViewModel", viewModel)
        SessionSavedStateHandler.attachTo(viewModel, viewModel.savedStateHandle)
        viewModelStore.clear()

        assertThat(
            viewModel.savedStateHandle.get<Session.Owner>(SessionSavedStateHandler.SESSION_KEY)
        ).isInstanceOf(Session.Owner::class.java)
    }

    private class FakeViewModel : ViewModel() {
        val savedStateHandle = SavedStateHandle()
    }
}
