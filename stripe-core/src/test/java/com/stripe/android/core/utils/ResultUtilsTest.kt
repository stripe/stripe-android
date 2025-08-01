package com.stripe.android.core.utils

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ResultUtilsTest {
    @Test
    fun testFlatMapCatching() {
        val success = Result.success(42)
        val failure = Result.failure<String>(RuntimeException())
        run {
            assertThat(success.flatMapCatching { Result.success(it.toString()) })
                .isEqualTo(Result.success("42"))
        }

        run {
            val error = RuntimeException()
            assertThat(success.flatMapCatching<Int, Any> { throw error })
                .isEqualTo(Result.failure<Any>(error))
        }

        run {
            assertThat(failure.flatMapCatching { Result.success(it) })
                .isEqualTo(failure)
        }
    }
}
