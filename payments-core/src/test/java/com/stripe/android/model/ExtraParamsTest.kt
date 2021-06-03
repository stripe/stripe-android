package com.stripe.android.model

import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ExtraParamsTest {

    @Test
    fun `verify roundtrip parceling`() {
        ParcelUtils.verifyParcelRoundtrip(
            ExtraParams(
                mapOf(
                    "hello" to "world",
                    "colors" to listOf("blue", "red", "yellow"),
                    "capitals" to mapOf(
                        "CA" to "Sacramento",
                        "MD" to "Annapolis"
                    )
                )
            )
        )
    }
}
