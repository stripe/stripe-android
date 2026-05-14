package com.stripe.android.common.taptoadd

import androidx.annotation.VisibleForTesting
import javax.inject.Inject

internal fun interface IsStripeTerminalSdkAvailable {
    operator fun invoke(): Boolean
}

internal fun interface HasStripeTerminalCoreLibrary {
    operator fun invoke(): String
}

internal fun interface HasStripeTerminalTapToPayLibrary {
    operator fun invoke()
}

internal fun interface StripeTerminalVersionValidator {
    operator fun invoke(versionName: String): Boolean
}

internal class DefaultIsStripeTerminalSdkAvailable @Inject constructor(
    private val versionValidator: StripeTerminalVersionValidator,
    private val hasStripeTerminalCoreLibrary: HasStripeTerminalCoreLibrary,
    private val hasStripeTerminalTapToPayLibrary: HasStripeTerminalTapToPayLibrary,
) : IsStripeTerminalSdkAvailable {
    override fun invoke(): Boolean {
        return IsStripeTerminalSdkAvailableOverride.override ?: run {
            try {
                val versionName = hasStripeTerminalCoreLibrary()
                hasStripeTerminalTapToPayLibrary()

                versionValidator(versionName)
            } catch (_: Exception) {
                false
            }
        }
    }
}

@VisibleForTesting
internal object IsStripeTerminalSdkAvailableOverride {
    @VisibleForTesting
    @Volatile
    var override: Boolean? = null
}

internal class DefaultHasStripeTerminalCoreLibrary @Inject constructor() :
    HasStripeTerminalCoreLibrary {
    override operator fun invoke(): String {
        Class.forName("com.stripe.stripeterminal.TerminalApplicationDelegate")

        return com.stripe.stripeterminal.BuildConfig.SDK_VERSION_NAME
    }
}

internal class DefaultHasStripeTerminalTapToPayLibrary @Inject constructor() :
    HasStripeTerminalTapToPayLibrary {
    override operator fun invoke() {
        Class.forName("com.stripe.stripeterminal.taptopay.TapToPay")
    }
}

internal class DefaultStripeTerminalVersionValidator @Inject constructor() : StripeTerminalVersionValidator {
    override operator fun invoke(versionName: String): Boolean {
        val segments = versionName.split(".")

        if (segments.size != VERSION_SIZE) {
            return false
        }

        val major = segments[0].toIntOrNull() ?: return false
        val minor = segments[1].toIntOrNull() ?: return false
        val patch = segments[2].toIntOrNull() ?: return false

        return when {
            major > MIN_MAJOR_VERSION -> true
            major < MIN_MAJOR_VERSION -> false
            minor > MIN_MINOR_VERSION -> true
            minor < MIN_MINOR_VERSION -> false
            else -> patch >= MIN_PATCH_VERSION
        }
    }

    private companion object {
        const val VERSION_SIZE = 3
        const val MIN_MAJOR_VERSION = 5
        const val MIN_MINOR_VERSION = 4
        const val MIN_PATCH_VERSION = 1
    }
}
