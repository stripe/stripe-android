package com.stripe.android.stripe3ds2.security

import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DirectoryServerTest {
    @Test
    fun `lookup should return expected DirectoryServer`() {
        assertThat(DirectoryServer.lookup("A000000003"))
            .isEqualTo(DirectoryServer.Visa)

        assertThat(DirectoryServer.lookup("A000000152"))
            .isEqualTo(DirectoryServer.Discover)

        assertThat(DirectoryServer.lookup("A000000324"))
            .isEqualTo(DirectoryServer.Discover)

        assertThat(DirectoryServer.lookup("A000000042"))
            .isEqualTo(DirectoryServer.CartesBancaires)

        assertFailsWith<SDKRuntimeException> {
            DirectoryServer.lookup("invalid")
        }
    }
}
