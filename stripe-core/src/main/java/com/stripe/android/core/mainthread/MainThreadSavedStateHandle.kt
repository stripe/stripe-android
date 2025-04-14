package com.stripe.android.core.mainthread

import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.BuildConfig
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MainThreadSavedStateHandle @Inject constructor(private val savedStateHandle: SavedStateHandle) {
    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        return savedStateHandle.getStateFlow(key, initialValue)
    }

    operator fun <T> set(key: String, value: T?) {
        if (Looper.getMainLooper() != Looper.myLooper() && BuildConfig.DEBUG) {
            throw AssertionError("Updates must be made on the main thread.")
        }
        savedStateHandle[key] = value
    }

    operator fun <T> get(key: String): T? {
        return savedStateHandle[key]
    }
}
