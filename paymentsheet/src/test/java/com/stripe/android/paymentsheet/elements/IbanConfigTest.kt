package com.stripe.android.paymentsheet.elements

import androidx.compose.ui.text.AnnotatedString
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IbanConfigTest {
    private val ibanConfig = IbanConfig()

    @Test
    fun `visualTransformation formats entered value`() {
        assertThat(ibanConfig.visualTransformation.filter(AnnotatedString("12345678901234")).text)
            .isEqualTo(AnnotatedString("1234 5678 9012 34"))
    }

    @Test
    fun `verify that only letters and numbers are allowed in the field`() {
        assertThat(ibanConfig.filter("123^@IBan[\uD83E\uDD57."))
            .isEqualTo("123IBAN")
    }
}
