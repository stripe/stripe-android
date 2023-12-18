package com.stripe.hcaptcha.webview

import android.os.Handler
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import com.stripe.hcaptcha.IHCaptchaVerifier
import com.stripe.hcaptcha.config.HCaptchaConfig
import com.stripe.hcaptcha.config.HCaptchaOrientation
import com.stripe.hcaptcha.config.HCaptchaSize
import com.stripe.hcaptcha.config.HCaptchaTheme
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
class HCaptchaJSInterfaceTest {
    private val handler = mock<Handler>()
    private val captchaVerifier = spy<IHCaptchaVerifier>()
    private val tokenCaptor = argumentCaptor<String>()
    private val exceptionCaptor = argumentCaptor<HCaptchaException>()
    private val testConfig = HCaptchaConfig(siteKey = "0000-1111-2222-3333")

    @Before
    fun init() {
        whenever(handler.post(ArgumentMatchers.any(Runnable::class.java)))
            .thenAnswer { invocation: InvocationOnMock ->
                invocation.getArgument(0, Runnable::class.java).run()
                null
            }
    }

    @Test
    @Throws(JSONException::class)
    fun full_config_serialization() {
        val siteKey = "0000-1111-2222-3333"
        val locale = "ro"
        val orientation = HCaptchaOrientation.PORTRAIT
        val size = HCaptchaSize.NORMAL
        val rqdata = "custom rqdata"
        val jsSrc = "127.0.0.1/api.js"
        val endpoint = "https://example.com/endpoint"
        val assethost = "https://example.com/assethost"
        val imghost = "https://example.com/imghost"
        val reportapi = "https://example.com/reportapi"
        val host = "custom-host"
        val timeout = 60.seconds

        val config = HCaptchaConfig(
            siteKey = siteKey,
            locale = locale,
            size = size,
            orientation = orientation,
            theme = HCaptchaTheme.DARK,
            rqdata = rqdata,
            jsSrc = jsSrc,
            endpoint = endpoint,
            assethost = assethost,
            imghost = imghost,
            reportapi = reportapi,
            host = host,
            hideDialog = true,
            tokenExpiration = timeout,
            disableHardwareAcceleration = false,
        )

        val jsInterface = HCaptchaJSInterface(handler, config, captchaVerifier)

        val expected = buildJsonObject {
            put("siteKey", siteKey)
            put("sentry", true)
            put("loading", true)
            put("hideDialog", true)
            put("rqdata", rqdata)
            put("jsSrc", jsSrc)
            put("endpoint", endpoint)
            put("reportapi", reportapi)
            put("assethost", assethost)
            put("imghost", imghost)
            put("locale", locale)
            put("size", "normal")
            put("orientation", "portrait")
            put("theme", "dark")
            put("host", host)
            put("customTheme", null)
            put("tokenExpiration", timeout.inWholeSeconds)
            put("disableHardwareAcceleration", false)
        }

        assertEquals(expected.toString(), jsInterface.config)
    }

    @Test
    @Throws(JSONException::class)
    fun subset_config_serialization() {
        val siteKey = "0000-1111-2222-3333"
        val locale = "ro"
        val size: HCaptchaSize = HCaptchaSize.NORMAL
        val orientation: HCaptchaOrientation = HCaptchaOrientation.LANDSCAPE
        val rqdata = "custom rqdata"
        val defaultTimeout = 120L

        val config = HCaptchaConfig(
            siteKey = siteKey,
            locale = locale,
            size = size,
            orientation = orientation,
            theme = HCaptchaTheme.DARK,
            rqdata = rqdata,
        )

        val jsInterface = HCaptchaJSInterface(handler, config, captchaVerifier)

        val expected = buildJsonObject {
            put("siteKey", siteKey)
            put("sentry", true)
            put("loading", true)
            put("hideDialog", false)
            put("rqdata", rqdata)
            put("jsSrc", "https://js.hcaptcha.com/1/api.js")
            put("endpoint", null)
            put("reportapi", null)
            put("assethost", null)
            put("imghost", null)
            put("locale", locale)
            put("size", "normal")
            put("orientation", "landscape")
            put("theme", "dark")
            put("host", null)
            put("customTheme", null)
            put("tokenExpiration", defaultTimeout)
            put("disableHardwareAcceleration", true)
        }

        assertEquals(expected.toString(), jsInterface.config)
    }

    @Test
    fun calls_on_challenge_ready() {
        val jsInterface = HCaptchaJSInterface(handler, testConfig, captchaVerifier)
        jsInterface.onLoaded()
        verify(captchaVerifier, Mockito.times(1)).onLoaded()
    }

    @Test
    fun calls_on_challenge_visible_cb() {
        val jsInterface = HCaptchaJSInterface(handler, testConfig, captchaVerifier)
        jsInterface.onOpen()
        verify(captchaVerifier, Mockito.times(1)).onOpen()
    }

    @Test
    fun on_pass_forwards_token_to_listeners() {
        val token = "mock-token"
        val jsInterface = HCaptchaJSInterface(handler, testConfig, captchaVerifier)
        jsInterface.onPass(token)
        verify(captchaVerifier, Mockito.times(1)).onSuccess(tokenCaptor.capture())
        Assert.assertEquals(token, tokenCaptor.firstValue)
    }

    @Test
    fun on_error_forwards_error_to_listeners() {
        val error = HCaptchaError.CHALLENGE_CLOSED
        val jsInterface = HCaptchaJSInterface(handler, testConfig, captchaVerifier)
        jsInterface.onError(error.errorId)
        verify(captchaVerifier, Mockito.times(1)).onFailure(exceptionCaptor.capture())
        Assert.assertEquals(error.message, exceptionCaptor.firstValue.message)
        Assert.assertNotNull(exceptionCaptor.firstValue)
    }
}
