package com.stripe.android.common.taptoadd

import com.google.common.truth.Truth.assertThat
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import org.junit.Test

internal class DefaultTapToAddFatalErrorCheckerTest {
    private val checker = DefaultTapToAddFatalErrorChecker()

    @Test
    fun `isFatal is true for TAP_TO_PAY_NFC_DISABLED`() {
        assertFatalTerminalError(TerminalErrorCode.TAP_TO_PAY_NFC_DISABLED)
    }

    @Test
    fun `isFatal is true for TAP_TO_PAY_DEVICE_TAMPERED`() {
        assertFatalTerminalError(TerminalErrorCode.TAP_TO_PAY_DEVICE_TAMPERED)
    }

    @Test
    fun `isFatal is true for TAP_TO_PAY_UNSUPPORTED_DEVICE`() {
        assertFatalTerminalError(TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE)
    }

    @Test
    fun `isFatal is true for TAP_TO_PAY_UNSUPPORTED_ANDROID_VERSION`() {
        assertFatalTerminalError(TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_ANDROID_VERSION)
    }

    @Test
    fun `isFatal is true for TAP_TO_PAY_UNSUPPORTED_PROCESSOR`() {
        assertFatalTerminalError(TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_PROCESSOR)
    }

    @Test
    fun `isFatal is true for TAP_TO_PAY_LIBRARY_NOT_INCLUDED`() {
        assertFatalTerminalError(TerminalErrorCode.TAP_TO_PAY_LIBRARY_NOT_INCLUDED)
    }

    @Test
    fun `isFatal is true for TAP_TO_PAY_DEBUG_NOT_SUPPORTED`() {
        assertFatalTerminalError(TerminalErrorCode.TAP_TO_PAY_DEBUG_NOT_SUPPORTED)
    }

    @Test
    fun `isFatal is true for TAP_TO_PAY_INSECURE_ENVIRONMENT`() {
        assertFatalTerminalError(TerminalErrorCode.TAP_TO_PAY_INSECURE_ENVIRONMENT)
    }

    @Test
    fun `isFatal is false for other terminal errors`() {
        val error = TerminalException(
            errorCode = TerminalErrorCode.CANCEL_FAILED,
            errorMessage = "Cancel failed",
        )

        assertThat(checker.isFatal(error)).isFalse()
    }

    @Test
    fun `isFatal is false for non-terminal throwables`() {
        assertThat(checker.isFatal(IllegalStateException("failed"))).isFalse()
    }

    private fun assertFatalTerminalError(errorCode: TerminalErrorCode) {
        val error = TerminalException(
            errorCode = errorCode,
            errorMessage = errorCode.name,
        )

        assertThat(checker.isFatal(error)).isTrue()
    }
}
