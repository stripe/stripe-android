package com.stripe.android.stripe3ds2.init

/**
 * Checks for security warnings during SDK initialization as defined in
 * "EMV® 3-D Secure SDK - 8.2 SDK Initialization Security Checks"
 */
internal fun interface SecurityChecker {
    /**
     * A list of warning ids as defined in
     * "EMV® 3-D Secure SDK - Table 8.1: 3DS SDK Initialization Security Checks"
     */
    fun getWarnings(): List<Warning>
}

internal class DefaultSecurityChecker internal constructor(
    private val securityChecks: List<SecurityCheck> = DEFAULT_CHECKS
) : SecurityChecker {
    override fun getWarnings(): List<Warning> {
        return securityChecks
            .filter { it.check() }
            .map { it.warning }
    }

    private companion object {
        val DEFAULT_CHECKS = listOf(
            SecurityCheck.RootedCheck(),
            SecurityCheck.Tampered(),
            SecurityCheck.Emulator(),
            SecurityCheck.DebuggerAttached(),
            SecurityCheck.UnsupportedOS()
        )
    }
}
