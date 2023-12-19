package com.stripe.hcaptcha.webview

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.annotation.RestrictTo
import com.stripe.hcaptcha.encode.encodeToJson
import dalvik.system.DexFile
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Serializable
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Debug info for CI.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class HCaptchaDebugInfo(private val context: Context) : Serializable {

    @get:JavascriptInterface
    val debugInfo: String by lazy {
        try {
            encodeToJson(ListSerializer(String.serializer()), debugInfo(context.packageName, context.packageCodePath))
        } catch (e: IOException) {
            Log.d(JS_INTERFACE_TAG, "Cannot build debugInfo")
            "[]"
        }
    }

    @get:JavascriptInterface
    val sysDebug: String by lazy {
        try {
            encodeToJson(MapSerializer(String.serializer(), String.serializer()), roBuildProps())
        } catch (e: IOException) {
            Log.d(JS_INTERFACE_TAG, "Cannot build sysDebug")
            "{}"
        }
    }

    @Throws(IOException::class, NoSuchAlgorithmException::class)
    private fun debugInfo(packageName: String?, packageCode: String?): List<String> {
        val result: MutableList<String> = ArrayList(512)
        val androidMd5 = MessageDigest.getInstance("MD5")
        val appMd5 = MessageDigest.getInstance("MD5")
        val depsMd5 = MessageDigest.getInstance("MD5")
        val dexFile = DexFile(packageCode)
        try {
            val classes = dexFile.entries()
            while (classes.hasMoreElements()) {
                val cls = classes.nextElement()
                if (cls.startsWith("com.google.android.") || cls.startsWith("android.")) {
                    androidMd5.update(cls.toByteArray(charset(CHARSET_NAME)))
                } else if (cls.startsWith(packageName!!)) {
                    appMd5.update(cls.toByteArray(charset(CHARSET_NAME)))
                } else {
                    depsMd5.update(cls.toByteArray(charset(CHARSET_NAME)))
                }
            }
        } finally {
            dexFile.close()
        }
        val hexFormat = "%032x"
        result.add("sys_" + String.format(hexFormat, BigInteger(1, androidMd5.digest())))
        result.add("deps_" + String.format(hexFormat, BigInteger(1, depsMd5.digest())))
        result.add("app_" + String.format(hexFormat, BigInteger(1, appMd5.digest())))
        result.add("aver_" + Build.VERSION.RELEASE)
        return result
    }

    @Throws(IOException::class)
    private fun roBuildProps(): Map<String, String> {
        var getpropProcess: Process? = null
        val props: MutableMap<String, String> = HashMap()
        try {
            getpropProcess = ProcessBuilder(GET_PROP_BIN).start()
            BufferedReader(
                InputStreamReader(getpropProcess.inputStream, CHARSET_NAME)
            ).use { br ->
                var line: String
                val entry = StringBuilder()
                while (br.readLine().also { line = it } != null) {
                    if (line.endsWith("]")) {
                        entry.replace(0, if (entry.length == 0) 0 else entry.length - 1, line)
                    } else {
                        entry.append(line)
                        continue
                    }
                    val parsedLine =
                        entry.toString().split("]: \\[".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val key = parsedLine[0].substring(1) // strip first [
                    if (key.startsWith("ro")) {
                        val value = parsedLine[1].substring(0, parsedLine[1].length - 2) // strip last ]
                        props[key] = value
                    }
                }
            }
        } finally {
            getpropProcess?.destroy()
        }
        return props
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        private const val serialVersionUID: Long = -2969617621043154137L
        const val JS_INTERFACE_TAG = "JSDI"

        private const val GET_PROP_BIN = "/system/bin/getprop"
        private const val CHARSET_NAME = "UTF-8"
    }
}
