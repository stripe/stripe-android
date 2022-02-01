package com.stripe.android.stripe3ds2.init

import android.os.Build
import android.os.Debug
import android.os.Environment
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl
import java.io.File

internal sealed class SecurityCheck(
    val warning: Warning
) {

    /**
     * @return true if the security check failed
     */
    abstract fun check(): Boolean

    class RootedCheck : SecurityCheck(WARNING) {
        override fun check() = findSuBinary() || findSuperuserApk()

        private fun findSuBinary(): Boolean {
            return BINARY_PATHS.any { File("${it}su").exists() }
        }

        private fun findSuperuserApk(): Boolean {
            return File(Environment.getRootDirectory().toString() + "/Superuser").isDirectory
        }

        private companion object {
            private val BINARY_PATHS = listOf(
                "/sbin/",
                "/system/bin/",
                "/system/xbin/",
                "/data/local/xbin/",
                "/data/local/bin/",
                "/system/sd/xbin/",
                "/system/bin/failsafe/",
                "/data/local/"
            )

            private val WARNING = Warning(
                id = "SW01",
                message = "The device is jailbroken.",
                severity = Warning.Severity.HIGH
            )
        }
    }

    class Tampered : SecurityCheck(WARNING) {
        override fun check(): Boolean {
            return !hasValidFields() || !hasValidMethods()
        }

        private fun hasValidFields(): Boolean {
            val fieldCount = StripeThreeDs2ServiceImpl::class.java.declaredFields.size
            return fieldCount == 6
        }

        private fun hasValidMethods(): Boolean {
            val methodCount = StripeThreeDs2ServiceImpl::class.java.declaredMethods.size
            return methodCount == 3
        }

        private companion object {
            private val WARNING = Warning(
                id = "SW02",
                message = "The integrity of the SDK has been tampered.",
                severity = Warning.Severity.HIGH
            )
        }
    }

    class Emulator : SecurityCheck(WARNING) {
        private val isEmulator: Boolean
            get() = (
                Build.FINGERPRINT.startsWith("generic") ||
                    Build.FINGERPRINT.startsWith("unknown") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK built for x86") ||
                    Build.MODEL.contains("google_sdk") ||
                    Build.MANUFACTURER.contains("Genymotion") ||
                    Build.BRAND.startsWith("generic") &&
                    Build.DEVICE.startsWith("generic") ||
                    "google_sdk" == Build.PRODUCT
                )

        override fun check() = isEmulator

        private companion object {
            private val WARNING = Warning(
                id = "SW02",
                message = "An emulator is being used to run the App.",
                severity = Warning.Severity.HIGH
            )
        }
    }

    class DebuggerAttached(
        val isDebuggerConnected: Boolean = Debug.isDebuggerConnected()
    ) : SecurityCheck(WARNING) {
        override fun check() = isDebuggerConnected

        private companion object {
            private val WARNING = Warning(
                id = "SW04",
                message = "A debugger is attached to the App.",
                severity = Warning.Severity.MEDIUM
            )
        }
    }

    class UnsupportedOS : SecurityCheck(WARNING) {
        override fun check() = false

        private companion object {
            private val WARNING = Warning(
                id = "SW05",
                message = "The OS or the OS version is not supported.",
                severity = Warning.Severity.HIGH
            )
        }
    }
}
