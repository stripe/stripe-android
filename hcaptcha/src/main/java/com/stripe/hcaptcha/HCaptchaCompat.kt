package com.stripe.hcaptcha

import android.os.Build
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.os.BundleCompat
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaInternalConfig
import java.io.Serializable

/**
 * Internal class not pollute code with SDK_INT >= VERSION_CODES.X checks
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object HCaptchaCompat {

    /**
     * Key for passing config to the dialog fragment
     */
    private const val KEY_CONFIG = "hCaptchaConfig"

    /**
     * Key for passing internal config to the dialog fragment
     */
    private const val KEY_INTERNAL_CONFIG = "hCaptchaInternalConfig"

    /**
     * Key for passing listener to the dialog fragment
     */
    private const val KEY_LISTENER = "hCaptchaDialogListener"

    fun storeValues(
        config: HCaptchaConfig,
        internalConfig: HCaptchaInternalConfig,
        listener: HCaptchaStateListener
    ): Bundle {
        val args = Bundle()
        args.putSerializable(KEY_CONFIG, config)
        args.putSerializable(KEY_INTERNAL_CONFIG, internalConfig)
        args.putParcelable(KEY_LISTENER, listener)
        return args
    }

    fun getConfig(bundle: Bundle): HCaptchaConfig? {
        return getSerializable(bundle, KEY_CONFIG, HCaptchaConfig::class.java)
    }

    fun getInternalConfig(bundle: Bundle): HCaptchaInternalConfig? {
        return getSerializable(bundle, KEY_INTERNAL_CONFIG, HCaptchaInternalConfig::class.java)
    }

    fun getStateListener(bundle: Bundle): HCaptchaStateListener? {
        return BundleCompat.getParcelable(bundle, KEY_LISTENER, HCaptchaStateListener::class.java)
    }

    @Suppress("deprecation", "UNCHECKED_CAST")
    private fun <T : Serializable?> getSerializable(bundle: Bundle, key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getSerializable(key, clazz)
        } else {
            bundle.getSerializable(key) as T?
        }
    }
}
