package com.stripe.android.uicore.utils

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    val result = remember { mutableStateOf(produceInitialValue()) }
    LaunchedEffect(key) {
        DefaultProduceStateScope(result, coroutineContext).producer()
    }
    return result
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun <T> StateFlow<T>.collectAsState(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> = produceState(
    produceInitialValue = remember { { value } },
    key = this
) {
    if (context == EmptyCoroutineContext) {
        collect { value = it }
    } else {
        withContext(context) {
            collect { value = it }
        }
    }
}
