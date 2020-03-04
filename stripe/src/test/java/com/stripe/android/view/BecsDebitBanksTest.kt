package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BecsDebitBanksTest {

    @Test
    fun init_shouldCreateExpectedData() {
        val banks = BecsDebitBanks(
            ApplicationProvider.getApplicationContext<Context>()
        ).banks
        assertThat(banks)
            .hasSize(117)
        assertThat(banks)
            .contains(
                BecsDebitBanks.Bank(
                    prefix = "21",
                    code = "CMB",
                    name = "JP Morgan Chase Bank"
                )
            )
    }
}
