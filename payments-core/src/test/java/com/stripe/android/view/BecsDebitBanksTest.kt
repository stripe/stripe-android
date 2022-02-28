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
            .hasSize(121)
        assertThat(banks)
            .contains(
                BecsDebitBanks.Bank(
                    prefix = "21",
                    name = "JP Morgan Chase Bank"
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
