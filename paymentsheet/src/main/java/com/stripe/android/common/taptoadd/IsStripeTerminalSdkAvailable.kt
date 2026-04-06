package com.stripe.android.common.taptoadd

import javax.inject.Inject

internal fun interface IsStripeTerminalSdkAvailable {
    operator fun invoke(): Boolean
}

internal class DefaultIsStripeTerminalSdkAvailable @Inject constructor() : IsStripeTerminalSdkAvailable {
    override fun invoke(): Boolean {
        return try {
            // If found, 'core' library was imported
            Class.forName("com.stripe.stripeterminal.TerminalApplicationDelegate")
            // If found, 'taptopay' library was imported
            Class.forName("com.stripe.stripeterminal.taptopay.TapToPay")

            isValidVersion()
        } catch (_: Exception) {
            false
        }
    }

    private fun isValidVersion(): Boolean {
        val versionNumbers = com.stripe.stripeterminal.BuildConfig.SDK_VERSION_NAME.split(".")

        return if (versionNumbers.size == VERSION_SIZE) {
            val majorVersion = versionNumbers[0].toIntOrNull() ?: 0
            val minorVersion = versionNumbers[1].toIntOrNull() ?: 0

            majorVersion >= MIN_MAJOR_VERSION && minorVersion >= MIN_MINOR_VERSION
        } else {
            false
        }
    }

    private companion object {
        const val VERSION_SIZE = 3
        const val MIN_MAJOR_VERSION = 5
        const val MIN_MINOR_VERSION = 4
    }
}
