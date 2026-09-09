package com.stripe.android.utils

import androidx.lifecycle.SavedStateHandle

// Rebuilds a handle from its saved provider state, as SavedStateRegistry does after process death.
@Suppress("RestrictedApi")
internal fun SavedStateHandle.simulateProcessDeath(): SavedStateHandle =
    SavedStateHandle.createHandle(savedStateProvider().saveState(), null)
