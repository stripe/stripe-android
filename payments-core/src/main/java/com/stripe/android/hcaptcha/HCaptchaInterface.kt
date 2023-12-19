package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.hcaptcha.HCaptcha
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.HCaptchaTokenResponse
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaSize
import com.stripe.hcaptcha.task.OnFailureListener
import com.stripe.hcaptcha.task.OnSuccessListener

/**
 * Proxy to access hcaptcha android sdk code safely
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun performPassiveHCaptcha(
    activity: FragmentActivity,
    siteKey: String,
    rqdata: String?,
    onComplete: (hcaptchaToken: String) -> Unit
) {
    val hcaptcha = HCaptcha.getClient(activity).apply {
        addOnSuccessListener(object : OnSuccessListener<HCaptchaTokenResponse> {
            override fun onSuccess(result: HCaptchaTokenResponse) {
                onComplete(result.tokenResult)
            }
        })
        addOnFailureListener(object : OnFailureListener {
            override fun onFailure(exception: HCaptchaException) {
                onComplete(exception.hCaptchaError.name)
            }
        })
    }

    val config = HCaptchaConfig(
        siteKey = siteKey,
        size = HCaptchaSize.INVISIBLE,
        rqdata = if (rqdata.isNullOrEmpty()) null else rqdata,
        loading = false,
        hideDialog = true,
        disableHardwareAcceleration = true, // defaults to true in the example app, and seems more stable?
        retryPredicate = { _, exception -> exception.hCaptchaError == HCaptchaError.SESSION_TIMEOUT }
    )

    hcaptcha.setup(config).verifyWithHCaptcha()
}
