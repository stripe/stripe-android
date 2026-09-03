package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LpmSerializerTest {

    @Test
    fun `Verify that async defaults to false and fields to empty`() {
        val serializedString =
            """
                [
                  {
                    "type": "unknown_lpm"
                  }
                ]
            """.trimIndent()

        val result = LpmSerializer.deserializeList(serializedString).getOrThrow().first()
        assertThat(result.fields).isEmpty()
    }
}
