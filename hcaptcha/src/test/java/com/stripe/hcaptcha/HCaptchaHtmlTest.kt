package com.stripe.hcaptcha

import org.junit.Assert
import org.junit.Test

class HCaptchaHtmlTest {

    @Test
    fun html_not_empty() {
        Assert.assertFalse(HCAPTCHA_WEBVIEW_HTML_PROVIDER.invoke().isEmpty())
    }
}
