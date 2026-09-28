package com.stripe.android.stripecardscan.framework.util

import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArrayExtensionsTest {

    @Test
    @SmallTest
    fun reshape() {
        val matrix = generateTestMatrix(1_000, 10_000)
        val reshaped = matrix.reshape(100)

        assertEquals(100_000, reshaped.size)
        assertEquals(100, reshaped[0].size)
        assertEquals(matrix[3][5], reshaped[30][5])
    }

    @Test
    @SmallTest
    fun updateEach_array() {
        val array = (0 until 1_000_000).toList().toTypedArray()
        array.updateEach { it + 1 }

        Assert.assertArrayEquals((1..1_000_000).toList().toTypedArray(), array)
    }

    @Test
    @SmallTest
    fun updateEach_floatArray() {
        val array = generateTestFloatArray(1_000_000)
        val originalValue = array[100]
        array.updateEach { it + 0.1F }

        assertEquals(originalValue + 0.1F, array[100])
    }

    @Test
    @SmallTest
    fun filterByIndexes_FloatArray() {
        val array = generateTestFloatArray(1_000_000)
        val selectedIndexes = arrayOf(100).toIntArray() +
            IntArray(10_000) { Random.nextInt(array.size) }
        val originalValue = array[100]

        val filteredArray = array.filterByIndexes(selectedIndexes)

        assertEquals(originalValue, filteredArray[0])
    }

    @Test
    @SmallTest
    fun filterByIndexes_TypedArray() {
        val array = generateTestArray(1_000_000) { Random.nextInt() }
        val selectedIndexes = arrayOf(100).toIntArray() +
            IntArray(10_000) { Random.nextInt(array.size) }
        val originalValue = array[100]

        val filteredArray = array.filterByIndexes(selectedIndexes)

        assertEquals(originalValue, filteredArray[0])
    }

    @Test
    @SmallTest
    fun flatten() {
        val matrix = generateTestMatrix(1_000, 10_000)
        val flattened = matrix.flatten()

        assertEquals(matrix[3][5], flattened[3005])
    }

    @Test
    @SmallTest
    fun transpose() {
        val matrix = generateTestMatrix(1_000, 10_000)
        val transposed = matrix.transpose()

        assertEquals(matrix[1][1], transposed[1][1])
        assertEquals(matrix[3][5], transposed[5][3])
    }

    @Test
    @SmallTest
    fun filteredIndexes() {
        val array = generateTestFloatArray(10_000)
        val filteredIndexes = array.filteredIndexes { it < 0.5F }

        assertTrue { array.any { it >= 0.5F } }
        assertTrue { filteredIndexes.map { array[it] }.all { it < 0.5F } }
    }

    @Test
    @SmallTest
    fun indexOfMax() {
        val array = generateTestFloatArray(10_000)
        val maxIndex = array
            .mapIndexed { index, value -> Pair(index, value) }
            .maxByOrNull { it.second }?.first

        assertEquals(maxIndex, array.indexOfMax())
        assertNull(FloatArray(0).indexOfMax())
    }

    /**
     * Generate a test matrix.
     */
    private fun generateTestMatrix(width: Int, height: Int) =
        Array(height) { generateTestFloatArray(width) }

    /**
     * Generate a test FloatArray.
     */
    private fun generateTestFloatArray(length: Int) = FloatArray(length) { Random.nextFloat() }

    /**
     * Generate a test array.
     */
    private inline fun <reified T> generateTestArray(length: Int, noinline generator: (Int) -> T) =
        Array(length, generator)
}
