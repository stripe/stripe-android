package com.stripe.android.analytics

import android.app.Application
import androidx.annotation.RestrictTo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * This [ViewModel] is meant to restore the analytics session ID during process death cases.
 *
 * Upon initial creation, the [ViewModel] will attempt to assign itself as a session owner if a session
 * another [ViewModel] isn't already one, otherwise it will assign itself as a session observer. Irregardless of
 * what it holds, the session type is saved and restored after process death. If the [ViewModel] is a session
 * observer, no action is taken. If the [ViewModel] is a session owner, it will assign itself as the session
 * owner and restore the session id.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class SessionAndroidViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    init {
        SessionSavedStateHandler.startSession(savedStateHandle)
    }

    protected fun restartSession() {
        SessionSavedStateHandler.restartSession(savedStateHandle)
    }

    override fun onCleared() {
        SessionSavedStateHandler.clearSession(savedStateHandle)
    }
}
