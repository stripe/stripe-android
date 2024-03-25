package com.stripe.android.financialconnections.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update

internal fun <K, V> Flow<Map<K, V>>.get(keyFlow: Flow<K>): Flow<V> {
    return combineTransform(keyFlow) { map, key ->
        val result = map[key]
        if (result != null) {
            emit(result)
        }
    }.distinctUntilChanged()
}

internal fun <K, V> MutableStateFlow<Map<K, V>>.updateWithNewEntry(entry: Pair<K, V>) {
    update { it + mapOf(entry) }
}

internal fun <K, V> MutableStateFlow<Map<K, V>>.updateWithNewEntry(key: K, transform: (V) -> V) {
    update { map ->
        val oldValue = map[key]
        if (oldValue != null) {
            val newValue = transform(oldValue)
            map + mapOf(key to newValue)
        } else {
            map
        }
    }
}
