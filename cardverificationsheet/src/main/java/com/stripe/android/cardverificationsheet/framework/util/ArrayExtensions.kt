package com.stripe.android.cardverificationsheet.framework.util

import androidx.annotation.CheckResult
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Update an array in place with a modifier function.
 */
fun <T> Array<T>.updateEach(operation: (original: T) -> T) {
    for (i in this.indices) {
        this[i] = operation(this[i])
    }
}

/**
 * Update a [FloatArray] in place with a modifier function.
 */
fun FloatArray.updateEach(operation: (original: Float) -> Float) {
    for (i in this.indices) {
        this[i] = operation(this[i])
    }
}

/**
 * Filter an array to only those values specified in an index array.
 */
@CheckResult
inline fun <reified T> Array<T>.filterByIndexes(indexesToKeep: IntArray) =
    Array(indexesToKeep.size) { this[indexesToKeep[it]] }

/**
 * Filter an array to only those values specified in an index array.
 */
@CheckResult
fun FloatArray.filterByIndexes(indexesToKeep: IntArray) =
    FloatArray(indexesToKeep.size) { this[indexesToKeep[it]] }

/**
 * Flatten an array of arrays into a single array of sequential values.
 */
@CheckResult
fun Array<FloatArray>.flatten() = if (this.isNotEmpty()) {
    this.reshape(this.size * this[0].size)[0]
} else {
    floatArrayOf()
}

/**
 * Transpose an array of float arrays.
 */
@CheckResult
fun Array<FloatArray>.transpose() = if (this.isNotEmpty()) {
    val oldRows = this.size
    val oldColumns = this[0].size
    Array(oldColumns) { newRow -> FloatArray(oldRows) { newColumn -> this[newColumn][newRow] } }
} else {
    this
}

/**
 * Reshape a two-dimensional array. Assume all rows of the original array are the same length, and
 * that the array is evenly divisible by the new columns.
 */
@CheckResult
fun Array<FloatArray>.reshape(newColumns: Int): Array<FloatArray> {
    val oldRows = this.size
    val oldColumns = if (this.isNotEmpty()) this[0].size else 0
    val linearSize = oldRows * oldColumns
    val newRows = linearSize / newColumns + if (linearSize % newColumns != 0) 1 else 0

    var oldRow = 0
    var oldColumn = 0
    return Array(newRows) {
        FloatArray(newColumns) {
            val value = this[oldRow][oldColumn]
            if (++oldColumn == oldColumns) {
                oldColumn = 0
                oldRow++
            }
            value
        }
    }
}

/**
 * Clamp the value between min and max
 */
@CheckResult
fun clamp(value: Float, minimum: Float, maximum: Float): Float =
    max(minimum, min(maximum, value))

/**
 * Return a list of indexes that pass the filter.
 */
@CheckResult
fun FloatArray.filteredIndexes(predicate: (Float) -> Boolean): IntArray {
    val filteredIndexes = ArrayList<Int>()
    for (index in this.indices) {
        if (predicate(this[index])) {
            filteredIndexes.add(index)
        }
    }
    return filteredIndexes.toIntArray()
}

/**
 * Divide a [ByteArray] into an array of byte arrays of a given size. If the original array is not
 * evenly divisible by the [chunkSize], the last ByteArray may be smaller than the chunk size.
 */
@CheckResult
fun ByteArray.chunk(chunkSize: Int): Array<ByteArray> =
    Array(this.size / chunkSize + if (this.size % chunkSize == 0) 0 else 1) {
        copyOfRange(it * chunkSize, min((it + 1) * chunkSize, this.size))
    }

/**
 * Find the index of the maximum value in the array.
 */
@CheckResult
fun FloatArray.indexOfMax(): Int? {
    if (isEmpty()) {
        return null
    }

    var maxIndex = 0
    var maxValue = this[maxIndex]
    for (index in this.indices) {
        if (this[index] > maxValue) {
            maxIndex = index
            maxValue = this[index]
        }
    }

    return maxIndex
}

/**
 * Convert a [ByteBuffer] to a [ByteArray].
 */
@CheckResult
fun ByteBuffer.toByteArray() = ByteArray(remaining()).also { this.get(it) }

/**
 * Convert a list of [ByteBuffer]s to a single [ByteArray].
 */
@CheckResult
fun List<ByteBuffer>.toByteArray(): ByteArray {
    val totalSize = this.sumOf { it.remaining() }
    var offset = 0
    return ByteArray(totalSize).apply {
        // This should be using this@toByteArray.forEach, but doing so seems to require API 24. It's
        // unclear why this won't use the kotlin.collections version of `forEach`, but it's not
        // during compile.
        for (it in this@toByteArray) {
            val size = it.remaining()
            it.get(this, offset, size)
            offset += size
        }
    }
}

/**
 * Map an array to a new [Array].
 */
@CheckResult
inline fun <T, reified U> Array<T>.mapArray(transform: (T) -> U) =
    Array(this.size) { transform(this[it]) }

/**
 * Map an array to a new [IntArray].
 */
@CheckResult
fun <T> Array<T>.mapToIntArray(transform: (T) -> Int) = IntArray(this.size) { transform(this[it]) }
