package com.stripe.android.stripecardscan.framework.util

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ItemCounterTest {

    @Test
    fun `countItem returns 1 on first insertion`() = runTest {
        val counter = ItemCounter<String>()

        val count = counter.countItem("A")

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `countItem increments count for repeated items`() = runTest {
        val counter = ItemCounter<String>()

        counter.countItem("A")
        val count = counter.countItem("A")

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `countItem tracks multiple distinct items independently`() = runTest {
        val counter = ItemCounter<String>()

        counter.countItem("A")
        counter.countItem("A")
        counter.countItem("B")

        assertThat(counter.getHighestCountItem().item).isEqualTo("A")
        assertThat(counter.getHighestCountItem().count).isEqualTo(2)
    }

    @Test
    fun `getHighestCountItem returns item with maximum count`() = runTest {
        val counter = ItemCounter<String>()

        counter.countItem("A")
        counter.countItem("B")
        counter.countItem("B")
        counter.countItem("B")
        counter.countItem("A")

        val highest = counter.getHighestCountItem()
        assertThat(highest.item).isEqualTo("B")
        assertThat(highest.count).isEqualTo(3)
    }

    @Test(expected = NoSuchElementException::class)
    fun `getHighestCountItem throws NoSuchElementException when empty`() {
        val counter = ItemCounter<String>()

        counter.getHighestCountItem()
    }

    @Test
    fun `getHighestCountItemOrNull returns null when empty`() {
        val counter = ItemCounter<String>()

        assertThat(counter.getHighestCountItemOrNull()).isNull()
    }

    @Test
    fun `getHighestCountItemOrNull returns correct CountedItem`() = runTest {
        val counter = ItemCounter<Int>()

        counter.countItem(42)
        counter.countItem(42)

        val result = counter.getHighestCountItemOrNull()
        assertThat(result).isNotNull()
        assertThat(result!!.item).isEqualTo(42)
        assertThat(result.count).isEqualTo(2)
    }

    @Test
    fun `reset clears all items`() = runTest {
        val counter = ItemCounter<String>()

        counter.countItem("A")
        counter.countItem("B")
        counter.reset()

        assertThat(counter.getHighestCountItemOrNull()).isNull()
    }

    @Test
    fun `reset followed by countItem starts fresh`() = runTest {
        val counter = ItemCounter<String>()

        counter.countItem("A")
        counter.countItem("A")
        counter.reset()
        val count = counter.countItem("A")

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `initialValue constructor counts the initial item`() {
        val counter = ItemCounter(initialValue = "X")

        val result = counter.getHighestCountItem()
        assertThat(result.item).isEqualTo("X")
        assertThat(result.count).isEqualTo(1)
    }
}
