package com.stripe.android.checkout

import android.os.Build
import android.view.View
import android.webkit.WebView
import androidx.test.platform.app.InstrumentationRegistry
import java.lang.reflect.Modifier

/**
 * System WebView 101's AwContents$AwWindowCoverageTracker never removes DecorViews from its static map,
 * so every Activity that hosted a WebView stays reachable and fails later LeakCanary checks in the same
 * process. Its name is obfuscated per build, so it's found by shape. No-op when the tracker isn't present.
 */
internal fun clearWebViewWindowCoverageTracker() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
    val awContents = try {
        Class.forName("org.chromium.android_webview.AwContents", false, WebView.getWebViewClassLoader())
    } catch (_: ClassNotFoundException) {
        return
    }
    val tracker = awContents.declaredFields.map { it.type }.firstOrNull { it.isWindowCoverageTracker() } ?: return
    val windowsField = tracker.declaredFields.singleOrNull {
        Modifier.isStatic(it.modifiers) && Map::class.java.isAssignableFrom(it.type)
    } ?: return
    windowsField.isAccessible = true
    val windows = windowsField.get(null) as? MutableMap<*, *> ?: return
    InstrumentationRegistry.getInstrumentation().runOnMainSync { windows.clear() }
}

private fun Class<*>.isWindowCoverageTracker(): Boolean {
    val instanceFieldTypes = declaredFields.filterNot { Modifier.isStatic(it.modifiers) }.map { it.type }
    return instanceFieldTypes.size == TRACKER_INSTANCE_FIELD_TYPES.size &&
        instanceFieldTypes.containsAll(TRACKER_INSTANCE_FIELD_TYPES)
}

private val TRACKER_INSTANCE_FIELD_TYPES = listOf(
    View::class.java,
    ArrayList::class.java,
    Long::class.javaPrimitiveType,
    Boolean::class.javaPrimitiveType,
)
