package com.stripe.android.core.mainthread

import android.os.Looper
import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MainThreadOnlyMutableStateFlow<T>(initialValue: T) : StateFlow<T> {
    private val source: MutableStateFlow<T> = MutableStateFlow(initialValue)

    override var value: T
        get() = source.value
        set(value) {
            assertIsMainThread()
            source.value = value
        }
    override val replayCache: List<T>
        get() = source.replayCache

    fun asStateFlow(): StateFlow<T> = source.asStateFlow()

    fun compareAndSet(expect: T, update: T): Boolean {
        assertIsMainThread()
        return source.compareAndSet(expect, update)
    }

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        source.collect(collector)
    }

    private fun assertIsMainThread() {
        if (Looper.getMainLooper() != Looper.myLooper() && BuildConfig.DEBUG) {
            throw AssertionError("Updates must be made on the main thread.")
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <T> MainThreadOnlyMutableStateFlow<T>.update(function: (T) -> T) {
    while (true) {
        val prevValue = value
        val nextValue = function(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}
