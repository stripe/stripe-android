package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkScreenTest {
    @Test
    fun `Parameter is URL encoded`() {
        assertThat(LinkScreen.CardEdit("abc <>*/ ").route)
            .isEqualTo("CardEdit?id=abc+%3C%3E*%2F+")
    }
}
