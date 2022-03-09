package com.stripe.android.stripecardscan.framework.util

import androidx.annotation.CheckResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A class that counts and saves items.
 */
internal class ItemCounter<T>(initialValue: T) {
    private val storageMutex = Mutex()
    private val items = mutableMapOf<T, Int>()

    init { runBlocking { countItem(initialValue) } }

    /**
     * Increment the count for the given item. Return the new count for the given item.
     */
    suspend fun countItem(item: T): Int = storageMutex.withLock {
        1 + (items.put(item, 1 + (items[item] ?: 0)) ?: 0)
    }

    /**
     * Get the item that with the highest count.
     */
    @CheckResult
    fun getHighestCountItem(): Pair<Int, T> =
        items.maxByOrNull { it.value }?.let { it.value to it.key }!!

    /**
     * Reset all item counts.
     */
    suspend fun reset() = storageMutex.withLock {
        items.clear()
    }
}
