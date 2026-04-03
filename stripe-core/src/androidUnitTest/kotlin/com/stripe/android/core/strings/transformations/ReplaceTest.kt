package com.stripe.android.core.strings.transformations

import org.junit.Test
import kotlin.test.assertEquals

class ReplaceTest {
    @Test
    fun `provided substring should be replaced with provided replacement in a given text`() {
        val replacer = Replace(original = "card", replacement = "flag")

        assertEquals(replacer.transform("this is a card!"), "this is a flag!")
    }
}
