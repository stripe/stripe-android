package com.stripe.android.compose.elements

import com.google.common.truth.Truth
import org.junit.Test

class IdealBankConfigTest {

    private val idealBankConfig = IdealBankConfig()

    @Test
    fun `verify country display name converted to country code`() {
        Truth.assertThat(idealBankConfig.convertToPaymentMethodParam("ABN AMRO"))
            .isEqualTo("abn_amro")
    }

    @Test
    fun `verify country code converted to country display name`() {
        Truth.assertThat(idealBankConfig.convertToDisplay("abn_amro"))
            .isEqualTo("ABN AMRO")
    }
}