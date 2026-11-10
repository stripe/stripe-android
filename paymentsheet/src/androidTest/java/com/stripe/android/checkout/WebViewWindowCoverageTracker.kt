package com.stripe.android.checkout

import android.os.Build
import android.view.View
import android.webkit.WebView
import androidx.test.platform.app.InstrumentationRegistry
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * System WebView 101's AwContents$AwWindowCoverageTracker never removes DecorViews from its static map,
 * so every Activity that hosted a WebView stays reachable and fails later LeakCanary checks in the same
 * process. Its name is obfuscated per build, so it's found by shape. No-op when the tracker isn't present.
 */
internal fun clearWebViewWindowCoverageTracker() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
    val awContents =
        Class.forName("org.chromium.android_webview.AwContents", false, WebView.getWebViewClassLoader())
    val windowsField = awContents.declaredFields.firstNotNullOfOrNull { it.type.windowCoverageTrackerMapField() }
        ?: return
    windowsField.isAccessible = true
    val windows = windowsField.get(null) as MutableMap<*, *>
    InstrumentationRegistry.getInstrumentation().runOnMainSync { windows.clear() }
}

private fun Class<*>.windowCoverageTrackerMapField(): Field? {
    val (staticFields, instanceFields) = declaredFields.partition { Modifier.isStatic(it.modifiers) }
    val instanceFieldTypes = instanceFields.map { it.type }
    if (instanceFieldTypes.size != TRACKER_INSTANCE_FIELD_TYPES.size ||
        !instanceFieldTypes.containsAll(TRACKER_INSTANCE_FIELD_TYPES)
    ) {
        return null
    }
    return staticFields.singleOrNull { Map::class.java.isAssignableFrom(it.type) }
}

private val TRACKER_INSTANCE_FIELD_TYPES = listOf(
    View::class.java,
    ArrayList::class.java,
    Long::class.javaPrimitiveType,
    Boolean::class.javaPrimitiveType,
)
