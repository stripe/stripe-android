@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

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
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Proxy to access hcaptcha android sdk code safely
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
suspend fun performPassiveHCaptcha(
    activity: FragmentActivity,
    siteKey: String,
    rqdata: String?
): String {
    // TODO(awush): in the future, we should convert the hCaptcha SDK to use a suspend interface instead of callbacks,
    //  then we can eliminate the {{suspendCancelableCoroutine}} here.
    return suspendCancellableCoroutine { coroutine ->
        val hcaptcha = HCaptcha.getClient(activity).apply {
            addOnSuccessListener(object : OnSuccessListener<HCaptchaTokenResponse> {
                override fun onSuccess(result: HCaptchaTokenResponse) {
                    coroutine.resume(result.tokenResult) { _ -> }
                }
            })
            addOnFailureListener(object : OnFailureListener {
                override fun onFailure(exception: HCaptchaException) {
                    coroutine.resume(exception.hCaptchaError.name) { _ -> }
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
}
