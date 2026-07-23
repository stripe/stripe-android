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
        )
    )
}
