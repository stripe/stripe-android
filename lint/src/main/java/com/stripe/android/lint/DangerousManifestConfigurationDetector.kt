package com.stripe.android.lint

import com.android.SdkConstants.ANDROID_URI
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import org.w3c.dom.Element

internal class DangerousManifestConfigurationDetector : Detector(), XmlScanner {
    companion object {
        val ISSUE = Issue.create(
            id = "DangerousManifestConfiguration",
            briefDescription = "Dangerous Manifest Configuration Detected",
            explanation = "This check flags potentially dangerous configurations in the Android Manifest.",
            category = Category.SECURITY,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(DangerousManifestConfigurationDetector::class.java, Scope.MANIFEST_SCOPE)
        )

        private val DANGEROUS_PERMISSIONS = listOf(
            "android.permission.READ_PHONE_STATE",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.SYSTEM_ALERT_WINDOW"
        )
    }

    override fun getApplicableElements() = listOf(
        "manifest",
        "application",
        "activity",
        "service",
        "receiver",
        "provider",
        "uses-permission",
        "permission"
    )

    override fun visitElement(context: XmlContext, element: Element) {
        when (element.tagName) {
            "manifest" -> checkManifestElement(context, element)
            "application" -> checkApplicationElement(context, element)
            "activity", "service", "receiver", "provider" -> checkComponentElement(context, element)
            "uses-permission", "permission" -> checkPermissionElement(context, element)
        }
    }

    private fun checkManifestElement(context: XmlContext, element: Element) {
        checkAttribute(
            context,
            element,
            "installLocation",
            "preferExternal",
            "android:installLocation=\"preferExternal\" can make the app vulnerable to tampering"
        )
    }

    private fun checkApplicationElement(context: XmlContext, element: Element) {
        checkAttribute(
            context,
            element,
            "debuggable",
            "true",
            "android:debuggable=\"true\" should not be used in production builds"
        )

        checkAttribute(
            context,
            element,
            "allowBackup",
            "true",
            "Consider setting android:allowBackup=\"false\" for security reasons"
        )

        checkAttribute(
            context,
            element,
            "usesCleartextTraffic",
            "true",
            "android:usesCleartextTraffic=\"true\" allows cleartext network traffic"
        )

        checkAttribute(
            context,
            element,
            "testOnly",
            "true",
            "android:testOnly=\"true\" should not be used in production builds"
        )
    }

    private fun checkComponentElement(context: XmlContext, element: Element) {
        val permission = element.getAttributeNS(ANDROID_URI, "permission")
        if (permission.isNotEmpty() && permission.endsWith(".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")) {
            context.report(
                ISSUE,
                element,
                context.getLocation(element),
                "Using DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION might not provide intended security"
            )
        }
    }

    private fun checkPermissionElement(context: XmlContext, element: Element) {
        val name = element.getAttributeNS(ANDROID_URI, "name")
        if (name in DANGEROUS_PERMISSIONS) {
            context.report(
                ISSUE,
                element,
                context.getLocation(element),
                "Using dangerous permission: $name. Ensure it's necessary and handle it securely."
            )
        }

        checkAttribute(
            context,
            element,
            "protectionLevel",
            "dangerous",
            "android:protectionLevel=\"dangerous\" should be avoided if possible"
        )
    }

    private fun checkAttribute(
        context: XmlContext,
        element: Element,
        attributeName: String,
        dangerousValue: String,
        message: String
    ) {
        val value = element.getAttributeNS(ANDROID_URI, attributeName)
        if (value == dangerousValue) {
            context.report(ISSUE, element, context.getLocation(element), message)
        }
    }
}
