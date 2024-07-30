package com.stripe.android.stripe3ds2.views

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import org.mockito.kotlin.mock
import kotlin.test.Test

class BrandTest {
    private val errorReporter = mock<ErrorReporter>()

    @Test
    fun `'visa' should return expected brand`() {
        assertThat(lookup("visa"))
            .isEqualTo(Brand.Visa)
    }

    @Test
    fun `'visa' with whitespace should return expected brand`() {
        assertThat(lookup("  visa  "))
            .isEqualTo(Brand.Visa)
    }

    @Test
    fun `'visa' with different case should return expected brand`() {
        assertThat(lookup("  VISA  "))
            .isEqualTo(Brand.Visa)
    }

    @Test
    fun `unsupported directory server name should return Unknown`() {
        assertThat(lookup("jcb"))
            .isEqualTo(Brand.Unknown)
    }

    private fun lookup(
        directoryServerName: String
    ) = Brand.lookup(directoryServerName, errorReporter)
}
