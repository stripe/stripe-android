package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class BecsDebitBanksTest {

    @Test
    fun init_shouldCreateExpectedData() {
        val banks = BecsDebitBanks().banks
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
}
