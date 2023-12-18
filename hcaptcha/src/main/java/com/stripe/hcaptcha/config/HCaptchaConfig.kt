package com.stripe.hcaptcha.config

import androidx.annotation.RestrictTo
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.encode.DurationSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * hCaptcha config builder which allows further customization of UI and other logic.
 * [.siteKey] is the only mandatory property.
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class HCaptchaConfig(

    /**
     * The site key. Get one here [hcaptcha.com](https://www.hcaptcha.com)
     */
    val siteKey: String,

    /**
     * Enable / Disable sentry error reporting.
     */
    val sentry: Boolean = true,

    /**
     * Show / Hide loading dialog.
     */
    val loading: Boolean = true,

    /**
     * Can be used in combination with passive sitekey to achieve full invisible flow.
     * See enterprise docs for more information.
     */
    val hideDialog: Boolean = false,

    /**
     * Custom supplied challenge data.
     */
    val rqdata: String? = null,

    /**
     * The url of api.js
     * Default: https://js.hcaptcha.com/1/api.js (Override only if using first-party hosting feature.)
     */
    val jsSrc: String = "https://js.hcaptcha.com/1/api.js",

    /**
     * Point hCaptcha JS Ajax Requests to alternative API Endpoint.
     * Default: https://api.hcaptcha.com (Override only if using first-party hosting feature.)
     */
    val endpoint: String? = null,

    /**
     * Point hCaptcha Bug Reporting Request to alternative API Endpoint.
     * Default: https://accounts.hcaptcha.com (Override only if using first-party hosting feature.)
     */
    val reportapi: String? = null,

    /**
     * Points loaded hCaptcha assets to a user defined asset location, used for proxies.
     * Default: https://newassets.hcaptcha.com (Override only if using first-party hosting feature.)
     */
    val assethost: String? = null,

    /**
     * Points loaded hCaptcha challenge images to a user defined image location, used for proxies.
     * Default: https://imgs.hcaptcha.com (Override only if using first-party hosting feature.)
     */
    val imghost: String? = null,

    /**
     * The locale: 2 characters language code iso 639-1
     * Default: current default locale for this instance of the JVM.
     */
    val locale: String = Locale.getDefault().language,

    /**
     * The size of the checkbox. Default is [HCaptchaSize.INVISIBLE].
     */
    val size: HCaptchaSize = HCaptchaSize.INVISIBLE,

    /**
     * The orientation of the challenge. Default is [HCaptchaOrientation.PORTRAIT].
     */
    val orientation: HCaptchaOrientation = HCaptchaOrientation.PORTRAIT,

    /**
     * The theme. Default is [HCaptchaTheme.LIGHT].
     */
    val theme: HCaptchaTheme = HCaptchaTheme.LIGHT,

    /**
     * hCaptcha SDK host identifier. null value means that it will be generated by SDK
     */
    val host: String? = null,

    /**
     * Custom theme JSON string
     */
    val customTheme: String? = null,

    /**
     * The lambda will decide should we retry or not
     */
    @Transient
    val retryPredicate: ((HCaptchaConfig, HCaptchaException?) -> Boolean)? = null,

    /**
     * hCaptcha token expiration timeout (seconds)
     */
    @Serializable(with = DurationSerializer::class)
    val tokenExpiration: Duration = 120.seconds,

    /**
     * Disable hardware acceleration for WebView
     */
    val disableHardwareAcceleration: Boolean = true,
) : java.io.Serializable
