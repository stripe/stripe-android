package com.stripe.android.core.utils

import android.util.Log
import androidx.annotation.RestrictTo

/**
 * A detector using reflection to check which plugin the SDK is being used from.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PluginDetector {
    private val TAG = PluginDetector::class.java.simpleName

    /**
     * Loop through all possible [PluginType]s and find the first one that exists in class path,
     * return null if none of the class is found.
     */
    val pluginType: String? =
        PluginType.values().firstOrNull {
            isPlugin(it.className)
        }?.pluginName

    private fun isPlugin(className: String): Boolean = try {
        Class.forName(className)
        true
    } catch (e: ClassNotFoundException) {
        Log.d(TAG, "$className not found: $e")
        false
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class PluginType(
        val className: String,
        val pluginName: String
    ) {
        ReactNative("com.facebook.react.bridge.NativeModule", "react-native"),
        Flutter("io.flutter.embedding.engine.FlutterEngine", "flutter"),
        Cordova("org.apache.cordova.CordovaActivity", "cordova"),
        Unity("com.unity3d.player.UnityPlayerActivity", "unity")
    }
}
