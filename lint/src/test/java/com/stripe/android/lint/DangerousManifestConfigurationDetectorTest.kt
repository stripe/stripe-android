package com.stripe.android.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class DangerousManifestConfigurationDetectorTest {

    private fun lintManifest(manifestContent: String) = lint()
        .files(xml("AndroidManifest.xml", manifestContent).indented())
        .issues(DangerousManifestConfigurationDetector.ISSUE)
        .allowMissingSdk() // Allow the test to run without an Android SDK

    @Test
    fun testManifestInstallLocationPreferExternal() {
        lintManifest(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                android:installLocation="preferExternal">
                <application />
            </manifest>
            """
        ).run().expect(
            """
            AndroidManifest.xml:1: Error: android:installLocation="preferExternal" can make the app vulnerable to tampering [DangerousManifestConfiguration]
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            ^
            1 errors, 0 warnings
            """
        )
    }

    @Test
    fun testApplicationDebuggable() {
        lintManifest(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application android:debuggable="true" />
            </manifest>
            """
        ).run().expect(
            """
            AndroidManifest.xml:2: Error: android:debuggable="true" should not be used in production builds [DangerousManifestConfiguration]
                <application android:debuggable="true" />
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
        )
    }

    @Test
    fun testApplicationAllowBackup() {
        lintManifest(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application android:allowBackup="true" />
            </manifest>
            """
        ).run().expect(
            """
            AndroidManifest.xml:2: Error: Consider setting android:allowBackup="false" for security reasons [DangerousManifestConfiguration]
                <application android:allowBackup="true" />
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
        )
    }

    @Test
    fun testApplicationUsesCleartextTraffic() {
        lintManifest(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application android:usesCleartextTraffic="true" />
            </manifest>
            """
        ).run().expect(
            """
            AndroidManifest.xml:2: Error: android:usesCleartextTraffic="true" allows cleartext network traffic [DangerousManifestConfiguration]
                <application android:usesCleartextTraffic="true" />
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
        )
    }

    @Test
    fun testApplicationTestOnly() {
        lintManifest(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application android:testOnly="true" />
            </manifest>
            """
        ).run().expect(
            """
            AndroidManifest.xml:2: Error: android:testOnly="true" should not be used in production builds [DangerousManifestConfiguration]
                <application android:testOnly="true" />
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
        )
    }

    @Test
    fun testComponentDynamicReceiverNotExportedPermission() {
        lintManifest(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application>
                    <receiver android:permission="com.example.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />
                </application>
            </manifest>
            """
        ).run().expect(
            """
            AndroidManifest.xml:3: Error: Using DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION might not provide intended security [DangerousManifestConfiguration]
                    <receiver android:permission="com.example.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
        )
    }

    @Test
    fun testDangerousPermissions() {
        val dangerousPermissions = listOf(
            "android.permission.READ_PHONE_STATE",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.SYSTEM_ALERT_WINDOW"
        )

        dangerousPermissions.forEach { permission ->
            val badLine = "<uses-permission android:name=\"$permission\" />"
            lintManifest(
                """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <uses-permission android:name="$permission" />
                </manifest>
                """
            ).run().expect(
                """
                AndroidManifest.xml:2: Error: Using dangerous permission: $permission. Ensure it's necessary and handle it securely. [DangerousManifestConfiguration]
                    $badLine
                    ${"~".repeat(badLine.length)}
                1 errors, 0 warnings
                """
            )
        }
    }

    @Test
    fun testDangerousProtectionLevel() {
        lintManifest(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <permission android:name="com.example.MY_PERMISSION"
                            android:protectionLevel="dangerous" />
            </manifest>
            """
        ).run().expect(
            """
            AndroidManifest.xml:2: Error: android:protectionLevel="dangerous" should be avoided if possible [DangerousManifestConfiguration]
                <permission android:name="com.example.MY_PERMISSION"
                ^
            1 errors, 0 warnings
            """
        )
    }

    @Test
    fun testNoIssuesReported() {
        lintManifest(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application
                    android:allowBackup="false"
                    android:debuggable="false"
                    android:usesCleartextTraffic="false"
                    android:testOnly="false" />
                <uses-permission android:name="android.permission.INTERNET" />
            </manifest>
            """
        ).run().expectClean()
    }
}
