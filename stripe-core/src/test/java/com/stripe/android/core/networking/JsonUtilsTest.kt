package com.stripe.android.core.networking

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class JsonUtilsTest {
    @Test
    fun `toSnakeCase should return expected value`() {
        listOf(
            "foo" to "foo",
            "fooBar" to "foo_bar",
            "FooBar" to "foo_bar",
            "FOOBar" to "f_o_o_bar",
            "fooBAR" to "foo_b_a_r",
            "fooBarBaz" to "foo_bar_baz",
            "foo123Bar" to "foo123_bar",
        ).forEach { (input, expected) ->
            assertThat(input.toSnakeCase()).isEqualTo(expected)
        }
    }
}
