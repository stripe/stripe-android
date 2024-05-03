package com.stripe.hcaptcha.webview

import android.os.Handler
import android.webkit.JavascriptInterface
import androidx.annotation.RestrictTo
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.IHCaptchaVerifier
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.encode.encodeToJson
import java.io.Serializable

/**
 * The JavaScript Interface which bridges the js and the java code
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class HCaptchaJSInterface(
    @field:Transient private val handler: Handler,
    config: HCaptchaConfig,
    @field:Transient private val captchaVerifier: IHCaptchaVerifier
) : Serializable {

    @get:JavascriptInterface
    val config: String by lazy {
        encodeToJson(HCaptchaConfig.serializer(), config)
    }

    @JavascriptInterface
    fun onPass(token: String) {
        handler.post { captchaVerifier.onSuccess(token) }
    }

    @JavascriptInterface
    fun onError(errCode: Int) {
        val error: HCaptchaError = HCaptchaError.fromId(errCode)
        handler.post { captchaVerifier.onFailure(HCaptchaException(error)) }
    }

    @JavascriptInterface
    fun onLoaded() {
        handler.post { captchaVerifier.onLoaded() }
    }

    @JavascriptInterface
    fun onOpen() {
        handler.post { captchaVerifier.onOpen() }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        private const val serialVersionUID: Long = -4830797392637553617L
        const val JS_INTERFACE_TAG = "JSInterface"
    }
}
