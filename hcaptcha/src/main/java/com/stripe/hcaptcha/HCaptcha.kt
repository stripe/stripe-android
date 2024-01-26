package com.stripe.hcaptcha

import android.util.AndroidRuntimeException
import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaInternalConfig
import com.stripe.hcaptcha.config.HCaptchaSize
import com.stripe.hcaptcha.task.Task
import com.stripe.hcaptcha.webview.HCaptchaHeadlessWebView

/**
 * hCaptcha client which allows invoking of challenge completion and listening for the result.
 *
 * <pre>
 * Usage example:
 * 1. Get a client either using the site key or customize by passing a config:
 * client = HCaptcha.getClient(this).verifyWithHCaptcha(YOUR_API_SITE_KEY)
 * client = HCaptcha.getClient(this).verifyWithHCaptcha([com.stripe.hcaptcha.config.HCaptchaConfig])
 *
 * // Improve cold start by setting up the hCaptcha client
 * client = HCaptcha.getClient(this).setup([com.stripe.hcaptcha.config.HCaptchaConfig])
 * // ui rendering...
 * // user form fill up...
 * // ready for human verification
 * client.verifyWithHCaptcha();
 * 2. Listen for the result and error events:
 *
 * client.addOnSuccessListener(new OnSuccessListener&lt;HCaptchaTokenResponse&gt;() {
 * @Override
 * public void onSuccess(HCaptchaTokenResponse response) {
 * String userResponseToken = response.getTokenResult();
 * Log.d(TAG, "hCaptcha token: " + userResponseToken);
 * // Validate the user response token using the hCAPTCHA siteverify API
 * }
 * })
 * .addOnFailureListener(new OnFailureListener() {
 * @Override
 * public void onFailure(HCaptchaException e) {
 * Log.d(TAG, "hCaptcha failed: " + e.getMessage() + "(" + e.getStatusCode() + ")");
 * }
 * });
 * </pre>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface IHCaptcha {

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param config Config to customize: size, theme, locale, endpoint, rqdata, etc.
     * @return new [HCaptcha] object
     */
    fun setup(config: HCaptchaConfig): HCaptcha?

    /**
     * Presents a captcha challenge. Depending on the configuration passed in setup, this will be either a passive
     * challenge or a dialog to be completed by the user.
     *
     * @return [HCaptcha]
     */
    fun verifyWithHCaptcha(): HCaptcha?

    /**
     * Force stop verification and release resources.
     */
    fun reset()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class HCaptcha private constructor(
    private val activity: FragmentActivity,
    private val internalConfig: HCaptchaInternalConfig
) : Task<HCaptchaTokenResponse>(), IHCaptcha {
    private var captchaVerifier: IHCaptchaVerifier? = null

    override fun setup(config: HCaptchaConfig): HCaptcha {
        val listener = HCaptchaStateListener(
            onOpen = { captchaOpened() },
            onSuccess = { token ->
                scheduleCaptchaExpired(config.tokenExpiration)
                setResult(HCaptchaTokenResponse(token, handler))
            },
            onFailure = { exception -> setException(exception) }
        )

        try {
            captchaVerifier = if (config.hideDialog) {
                // Overwrite certain config values in case the dialog is hidden to avoid behavior collision
                HCaptchaHeadlessWebView(
                    activity = activity,
                    config = config.copy(size = HCaptchaSize.INVISIBLE, loading = false),
                    internalConfig = internalConfig,
                    listener = listener
                )
            } else {
                HCaptchaDialogFragment.newInstance(config, internalConfig, listener)
            }
        } catch (e: AndroidRuntimeException) {
            listener.onFailure(HCaptchaException(HCaptchaError.ERROR))
        }
        return this
    }

    override fun verifyWithHCaptcha(): HCaptcha {
        val captchaVerifier = captchaVerifier
            ?: // Cold start at verification time.
            throw IllegalStateException("verifyWithHCaptcha must not be called before setup.")
        handler.removeCallbacksAndMessages(null)
        captchaVerifier.startVerification(activity)
        return this
    }

    override fun reset() {
        captchaVerifier?.let {
            it.reset()
            captchaVerifier = null
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Constructs a new client which allows to display a challenge dialog
         *
         * @param activity The current activity
         * @return new [HCaptcha] object
         */
        fun getClient(
            activity: FragmentActivity,
            internalConfig: HCaptchaInternalConfig = HCaptchaInternalConfig()
        ): HCaptcha {
            return HCaptcha(activity, internalConfig)
        }
    }
}
