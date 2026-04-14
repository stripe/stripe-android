package com.stripe.android.common.taptoadd

import javax.inject.Inject

internal fun interface IsStripeTerminalSdkAvailable {
    operator fun invoke(): Boolean
}

internal fun interface StripeTerminalVersionValidator {
    operator fun invoke(versionName: String): Boolean
}

internal class DefaultIsStripeTerminalSdkAvailable @Inject constructor(
    private val versionValidator: StripeTerminalVersionValidator,
) : IsStripeTerminalSdkAvailable {
    override fun invoke(): Boolean {
        return try {
            // If found, 'core' library was imported
            Class.forName("com.stripe.stripeterminal.TerminalApplicationDelegate")
            // If found, 'taptopay' library was imported
            Class.forName("com.stripe.stripeterminal.taptopay.TapToPay")

            versionValidator(versionName = com.stripe.stripeterminal.BuildConfig.SDK_VERSION_NAME)
        } catch (_: Exception) {
            false
        }
    }
}

internal class DefaultStripeTerminalVersionValidator @Inject constructor() : StripeTerminalVersionValidator {
    override operator fun invoke(versionName: String): Boolean {
        val versionNumbers = versionName.split(".")

        if (versionNumbers.size == VERSION_SIZE) {
            val majorVersion = versionNumbers[0].toIntOrNull() ?: 0

            if (majorVersion < MIN_MAJOR_VERSION) {
                return false
            }

            val minorVersion = versionNumbers[1].toIntOrNull() ?: 0

            if (minorVersion < MIN_MINOR_VERSION) {
                return false
            }

            val patchVersion = versionNumbers[2].toIntOrNull() ?: 0

            return patchVersion >= PATCH_VERSION || minorVersion >= MIN_MINOR_VERSION + 1
        } else {
            return false
        }
    }

    private companion object {
        const val VERSION_SIZE = 3
        const val MIN_MAJOR_VERSION = 5
        const val MIN_MINOR_VERSION = 4
        const val PATCH_VERSION = 1
    }
}
