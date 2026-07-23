package com.stripe.android.utils

import leakcanary.LeakCanary
import shark.AndroidReferenceMatchers
import shark.IgnoredReferenceMatcher
import shark.ReferencePattern

internal fun configureLeakCanaryForManagedDevices() {
    LeakCanary.config = LeakCanary.config.copy(
        referenceMatchers = AndroidReferenceMatchers.appDefaults + listOf(
            IgnoredReferenceMatcher(
                pattern = ReferencePattern.InstanceFieldPattern(
                    className = "java.lang.ClassLoader",
                    fieldName = "runtimeInternalObjects"
                )
            ),
            IgnoredReferenceMatcher(
                pattern = ReferencePattern.InstanceFieldPattern(
                    className = "dalvik.system.PathClassLoader",
                    fieldName = "runtimeInternalObjects"
                )
            ),
            IgnoredReferenceMatcher(
                pattern = ReferencePattern.InstanceFieldPattern(
                    className = "java.lang.DexCache",
                    fieldName = "runtimeInternalObjects"
                )
            ),
            // Chromium's WindowAndroid is held by a native global (GC root: "Global variable in
            // native code") and can retain a destroyed host Activity via PhoneWindow#mDestroyed
            // until Chrome's native side releases it. Only reproduces on WebView/Chrome-bearing
            // images (e.g. google_apis) used for browser-redirect LPM e2e tests; the aosp-atd
            // image has no WebView implementation so this never fires there. This is a
            // library/platform leak, not one we can fix from app code.
            IgnoredReferenceMatcher(
                pattern = ReferencePattern.NativeGlobalVariablePattern(
                    className = "org.chromium.ui.base.WindowAndroid"
                )
            ),
        )
    )
}
