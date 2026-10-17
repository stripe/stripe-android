package com.stripe.android.common.taptoadd

import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException

internal interface TapToAddFatalErrorChecker {
    fun isFatal(error: Throwable): Boolean
}

internal class DefaultTapToAddFatalErrorChecker : TapToAddFatalErrorChecker {
    override fun isFatal(error: Throwable): Boolean {
        return when (error) {
            is TerminalException if FATAL_TERMINAL_ERROR_CODES.contains(error.errorCode) -> true
            else -> false
        }
    }

    private companion object {
        val FATAL_TERMINAL_ERROR_CODES = setOf(
            TerminalErrorCode.TAP_TO_PAY_NFC_DISABLED,
            TerminalErrorCode.TAP_TO_PAY_DEVICE_TAMPERED,
            TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE,
            TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_ANDROID_VERSION,
            TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_PROCESSOR,
            TerminalErrorCode.TAP_TO_PAY_LIBRARY_NOT_INCLUDED,
            TerminalErrorCode.TAP_TO_PAY_DEBUG_NOT_SUPPORTED,
            TerminalErrorCode.TAP_TO_PAY_INSECURE_ENVIRONMENT,
        )
    }
}
