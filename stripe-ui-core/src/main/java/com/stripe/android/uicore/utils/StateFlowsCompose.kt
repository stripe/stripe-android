package com.stripe.android.uicore.utils

import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.stripe.android.uicore.BuildConfig
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private class DefaultProduceStateScope<T>(
    state: MutableState<T>,
    override val coroutineContext: CoroutineContext
) : ProduceStateScope<T>, MutableState<T> by state {
    override suspend fun awaitDispose(onDispose: () -> Unit): Nothing {
        try {
            suspendCancellableCoroutine<Nothing> { }
        } finally {
            onDispose()
        }
    }
}

@Composable
private fun <T> produceState(
    produceInitialValue: () -> T,
    key: Any?,
    producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember(key) { mutableStateOf(produceInitialValue()) }
    LaunchedEffect(key) {
        DefaultProduceStateScope(result, coroutineContext).producer()
    }
    return result
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun <T> StateFlow<T>.collectAsState(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> {
    val exception = remember(this) {
        if (BuildConfig.DEBUG) {
            // Need the stack trace of where it was originally instantiated to better debug.
            // Because compose can re-enter composable functions from anywhere,
            // it can be hard to figure out what StateFlow is being emitted off the main thread.
            AssertionError()
        } else {
            // Exceptions are expensive to create. Only do so when it might be used.
            null
        }
    }
    return produceState(
        produceInitialValue = remember(this) { { value } },
        key = this
    ) {
        if (context == EmptyCoroutineContext) {
            collect {
                if (Looper.getMainLooper() != Looper.myLooper() && BuildConfig.DEBUG) {
                    throw AssertionError("Updates must be made on the main thread.", exception)
                }
                value = it
            }
        } else {
            withContext(context) {
                collect { value = it }
            }
        }
    }
}
