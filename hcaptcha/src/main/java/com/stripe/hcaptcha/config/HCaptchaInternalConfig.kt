package com.stripe.hcaptcha.config

import androidx.annotation.RestrictTo
import com.stripe.hcaptcha.HCAPTCHA_WEBVIEW_HTML_PROVIDER
import java.io.Serializable

/**
 * hCaptcha internal config keep internal configuration, which should not be accessible by end user
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class HCaptchaInternalConfig(
    /**
     * HTML Provider
     */
    val htmlProvider: () -> String = HCAPTCHA_WEBVIEW_HTML_PROVIDER
) : Serializable
