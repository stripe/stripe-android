package com.stripe.android.cardverificationsheet.framework.util

import androidx.annotation.CheckResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList

internal interface ItemCounter<T> {
    suspend fun countItem(item: T): Int

    fun getHighestCountItem(minCount: Int = 1): Pair<Int, T>?

    suspend fun reset()
}

/**
 * A class that counts and saves items.
 */
internal class ItemTotalCounter<T>(firstValue: T? = null) : ItemCounter<T> {
    private val storageMutex = Mutex()
    private val items = mutableMapOf<T, Int>()

    init { if (firstValue != null) runBlocking { countItem(firstValue) } }

    /**
     * Increment the count for the given item. Return the new count for the given item.
     */
    override suspend fun countItem(item: T): Int = storageMutex.withLock {
        1 + (items.put(item, 1 + (items[item] ?: 0)) ?: 0)
    }

    /**
     * Get the item that with the highest count.
     *
     * @param minCount the minimum times an item must have been counted.
     */
    @CheckResult
    override fun getHighestCountItem(minCount: Int): Pair<Int, T>? =
        items
            .maxByOrNull { it.value }
            ?.let { if (items[it.key] ?: 0 >= minCount) it.value to it.key else null }

    /**
     * Reset all item counts.
     */
    override suspend fun reset() = storageMutex.withLock {
        items.clear()
    }
}

/**
 * A class that keeps track of [maxItemsToTrack] recent items.
 */
internal class ItemRecencyCounter<T>(
    private val maxItemsToTrack: Int,
    firstValue: T? = null
) : ItemCounter<T> {
    private val storageMutex = Mutex()
    private val items = LinkedList<T>()

    init { if (firstValue != null) runBlocking { countItem(firstValue) } }

    /**
     * Increment the count for the given item. Return the new count for the given item.
     */
    override suspend fun countItem(item: T): Int = storageMutex.withLock {
        items.addFirst(item)

        while (items.size > maxItemsToTrack) {
            items.removeLast()
        }

        items.count { it == item }
    }

    /**
     * Get the item that with the highest count.
     *
     * @param minCount the minimum times an item must have been counted.
     */
    @CheckResult
    override fun getHighestCountItem(minCount: Int): Pair<Int, T>? =
        items
            .groupingBy { it }
            .eachCount()
            .filter { it.value >= minCount }
            .maxByOrNull { it.value }
            ?.let { it.value to it.key }

    /**
     * Reset all item counts.
     */
    override suspend fun reset() = storageMutex.withLock {
        items.clear()
    }
}
