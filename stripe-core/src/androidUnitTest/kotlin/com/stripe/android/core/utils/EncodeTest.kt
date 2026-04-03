package com.stripe.android.core.utils

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class EncodeTest {

    @Test
    fun urlEncode_whenValueContainsSpaces_encodesWithPlus() {
        assertThat(urlEncode("a b c")).isEqualTo("a+b+c")
    }

    @Test
    fun urlEncode_whenValueContainsReservedCharacters_percentEncodesThem() {
        assertThat(urlEncode("card[number]+")).isEqualTo("card%5Bnumber%5D%2B")
    }

    @Test
    fun urlEncode_whenValueContainsUtf8Characters_encodesUtf8Bytes() {
        assertThat(urlEncode("café")).isEqualTo("caf%C3%A9")
    }
}
