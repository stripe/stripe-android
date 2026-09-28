package com.stripe.android.stripecardscan.framework.util

import androidx.annotation.CheckResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class CountedItem<T>(val count: Int, val item: T)

/**
 * A class that counts and saves items.
 */
internal class ItemCounter<T>(initialValue: T? = null) {
    private val storageMutex = Mutex()
    private val items = mutableMapOf<T, Int>()

    init { initialValue?.let { runBlocking { countItem(it) } } }

    /**
     * Increment the count for the given item. Return the new count for the given item.
     */
    suspend fun countItem(item: T): Int = storageMutex.withLock {
        1 + (items.put(item, 1 + (items[item] ?: 0)) ?: 0)
    }

    /**
     * Get the item with the highest count, or null if empty.
     */
    @CheckResult
    fun getHighestCountItemOrNull(): CountedItem<T>? =
        items.maxByOrNull { it.value }?.let { CountedItem(it.value, it.key) }

    /**
     * Get the item with the highest count.
     * @throws NoSuchElementException if the counter is empty.
     */
    @CheckResult
    fun getHighestCountItem(): CountedItem<T> =
        getHighestCountItemOrNull() ?: throw NoSuchElementException("ItemCounter is empty")

    /**
     * Reset all item counts.
     */
    suspend fun reset() = storageMutex.withLock {
        items.clear()
    }
}
