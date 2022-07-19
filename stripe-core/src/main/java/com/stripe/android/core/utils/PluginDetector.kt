package com.stripe.android.core.utils

internal object PluginDetector {
    var pluginType: String? =
        PluginType.values().firstOrNull {
            isPlugin(it.className)
        }?.pluginName

    private fun isPlugin(className: String): Boolean = try {
        Class.forName(className)
        true
    } catch (e: Exception) {
        false
    }

    enum class PluginType(
        val className: String,
        val pluginName: String
    ) {
        ReactNative("com.facebook.proguard.annotations.DoNotStrip", "react-native"),
        Flutter("io.flutter.embedding.android.FlutterActivity", "flutter")
    }
}