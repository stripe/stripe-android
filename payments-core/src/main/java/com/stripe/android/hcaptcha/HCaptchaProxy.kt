package com.stripe.android.hcaptcha

import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.BuildConfig
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig.HCaptchaConfigBuilder
import com.hcaptcha.sdk.HCaptchaError
import com.hcaptcha.sdk.HCaptchaSize

/**
 * Proxy to access hcaptcha android sdk code safely
 *
 */
internal interface HCaptchaProxy {

    fun performPassiveHCaptcha()

    companion object {
        fun create(
            activity: AppCompatActivity,
            siteKey: String,
            rqdata: String?,
            onComplete: (hcaptchaToken: String) -> Unit,
            provider: () -> HCaptchaProxy = {
                val hcaptcha = HCaptcha.getClient(activity)
                HCaptcha.getClient(activity)
                    .addOnSuccessListener { onComplete(it.tokenResult) }
                    .addOnFailureListener { onComplete(it.hCaptchaError.name) }
                DefaultHCaptchaProxy(hcaptcha, siteKey, rqdata)
            },
            isHCaptchaAvailable: IsHCaptchaAvailable = DefaultIsHCaptchaAvailable()
        ): HCaptchaProxy {
            return if (isHCaptchaAvailable()) {
                provider()
            } else {
                UnsupportedHCaptchaProxy()
            }
        }
    }
}

internal class DefaultHCaptchaProxy(
    private val hcaptcha: HCaptcha,
    private val siteKey: String,
    private val rqdata: String?
) : HCaptchaProxy {
    override fun performPassiveHCaptcha() {
        val configBuilder = HCaptchaConfigBuilder()
            .siteKey(siteKey)
            .size(HCaptchaSize.INVISIBLE)
            .loading(false)
            .hideDialog(true)
            .disableHardwareAcceleration(true) // defaults to true in the example app, and seems more stable?
            .tokenExpiration(10)
            .retryPredicate { _, exception -> exception.hCaptchaError == HCaptchaError.SESSION_TIMEOUT }

        if (!rqdata.isNullOrEmpty()) {
            configBuilder.rqdata(rqdata)
        }

        hcaptcha.setup().verifyWithHCaptcha(configBuilder.build())
    }
}

internal class UnsupportedHCaptchaProxy : HCaptchaProxy {
    override fun performPassiveHCaptcha() {
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(
                "Missing hcaptcha android SDK dependency, please add it to your apps build.gradle"
            )
        }
    }
}
