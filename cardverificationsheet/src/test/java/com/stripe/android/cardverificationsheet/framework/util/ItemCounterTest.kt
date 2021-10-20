package com.stripe.android.cardverificationsheet.framework.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ItemCounterTest {

    @Test
    @ExperimentalCoroutinesApi
    fun countTotalItems() = runBlockingTest {
        val itemCounter = ItemTotalCounter<String>()

        assertEquals(1, itemCounter.countItem("a"))
        assertEquals(1, itemCounter.countItem("b"))
        assertEquals(2, itemCounter.countItem("a"))

        assertEquals("a", itemCounter.getHighestCountItem()?.second)
        assertEquals(2, itemCounter.getHighestCountItem()?.first)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun resetTotal() = runBlockingTest {
        val itemCounter = ItemTotalCounter<String>()

        assertEquals(1, itemCounter.countItem("a"))
        assertEquals(1, itemCounter.countItem("b"))
        assertEquals(2, itemCounter.countItem("a"))

        itemCounter.reset()

        assertNull(itemCounter.getHighestCountItem())

        assertEquals(1, itemCounter.countItem("a"))
        assertEquals(1, itemCounter.countItem("b"))
        assertEquals(2, itemCounter.countItem("a"))
    }

    @Test
    @ExperimentalCoroutinesApi
    fun countMinTotalItems() = runBlockingTest {
        val itemCounter = ItemTotalCounter<String>()

        assertEquals(1, itemCounter.countItem("a"))
        assertEquals(1, itemCounter.countItem("b"))
        assertEquals(2, itemCounter.countItem("a"))

        assertNull(itemCounter.getHighestCountItem(minCount = 3))
    }

    @Test
    @ExperimentalCoroutinesApi
    fun countRecentItems() = runBlockingTest {
        val itemCounter = ItemRecencyCounter<String>(3)

        assertEquals(1, itemCounter.countItem("a")) // counter = [a]
        assertEquals(1, itemCounter.countItem("b")) // counter = [b, a]
        assertEquals(2, itemCounter.countItem("a")) // counter = [a, b, a]
        assertEquals(2, itemCounter.countItem("a")) // counter = [a, a, b]
        assertEquals(3, itemCounter.countItem("a")) // counter = [a, a, a]
        assertEquals(3, itemCounter.countItem("a")) // counter = [a, a, a]
        assertEquals(1, itemCounter.countItem("b")) // counter = [b, a, a]
        assertEquals(2, itemCounter.countItem("a")) // counter = [a, b, a]

        assertEquals("a", itemCounter.getHighestCountItem()?.second)
        assertEquals(2, itemCounter.getHighestCountItem()?.first)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun resetRecentItems() = runBlockingTest {
        val itemCounter = ItemRecencyCounter<String>(3)

        assertEquals(1, itemCounter.countItem("a")) // counter = [a]
        assertEquals(1, itemCounter.countItem("b")) // counter = [b, a]
        assertEquals(2, itemCounter.countItem("a")) // counter = [a, b, a]

        itemCounter.reset()

        assertNull(itemCounter.getHighestCountItem())

        assertEquals(1, itemCounter.countItem("a")) // counter = [a]
        assertEquals(1, itemCounter.countItem("b")) // counter = [b, a]
        assertEquals(2, itemCounter.countItem("a")) // counter = [a, b, a]

        assertEquals("a", itemCounter.getHighestCountItem()?.second)
        assertEquals(2, itemCounter.getHighestCountItem()?.first)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun countMinRecentItems() = runBlockingTest {
        val itemCounter = ItemRecencyCounter<String>(3)

        assertEquals(1, itemCounter.countItem("a"))
        assertEquals(1, itemCounter.countItem("b"))
        assertEquals(2, itemCounter.countItem("a"))

        assertNull(itemCounter.getHighestCountItem(minCount = 3))
    }
}
