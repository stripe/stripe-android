package com.stripe.android.camera.framework.util

import androidx.annotation.CheckResult
import java.nio.ByteBuffer

/**
 * Convert a [ByteBuffer] to a [ByteArray].
 */
@CheckResult
internal fun ByteBuffer.toByteArray() = ByteArray(remaining()).also { this.get(it) }

/**
 * Convert a list of [ByteBuffer]s to a single [ByteArray].
 */
@CheckResult
internal fun List<ByteBuffer>.toByteArray(): ByteArray {
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
internal inline fun <T, reified U> Array<T>.mapArray(transform: (T) -> U) =
    Array(this.size) { transform(this[it]) }

/**
 * Map an array to a new [IntArray].
 */
@CheckResult
internal fun <T> Array<T>.mapToIntArray(transform: (T) -> Int) =
    IntArray(this.size) { transform(this[it]) }
