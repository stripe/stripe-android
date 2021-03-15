package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.view.FpxBank
import kotlin.test.Test

class FpxBankStatusesTest {

    @Test
    fun isOnline_withEmptyInstance_shouldAlwaysReturnTrue() {
        assertThat(FpxBank.get("hsbc")?.let { FpxBankStatuses().isOnline(it) })
            .isTrue()
    }
}
