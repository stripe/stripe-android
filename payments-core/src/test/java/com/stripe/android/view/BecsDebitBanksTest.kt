package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class BecsDebitBanksTest {

    @Test
    fun init_shouldCreateExpectedData() {
        val banks = BecsDebitBanks(
            ApplicationProvider.getApplicationContext<Context>()
        ).banks
        assertThat(banks)
            .hasSize(146)
        assertThat(banks)
            .contains(
                BecsDebitBanks.Bank(
                    prefix = "369",
                    name = "BNK Banking Corporation Ltd"
                )
            )
    }

    @Test
    fun shouldIncludeTestBank_shouldConditionallyAddTestBank() {
        val testBank = BecsDebitBanks(
            context = ApplicationProvider.getApplicationContext(),
            shouldIncludeTestBank = true
        ).byPrefix("STRIPE")
        assertThat(testBank)
            .isNull()
    }
}
