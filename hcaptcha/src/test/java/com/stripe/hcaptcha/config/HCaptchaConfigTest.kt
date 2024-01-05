package com.stripe.hcaptcha.config

import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Locale
import kotlin.test.assertEquals

class HCaptchaConfigTest {
    @Test
    fun custom_locale() {
        val config = HCaptchaConfig(siteKey = MOCK_SITE_KEY, locale = "ro")
        assertEquals("ro", config.locale)
    }

    @Test
    fun default_config() {
        val config = HCaptchaConfig(siteKey = MOCK_SITE_KEY)

        assertEquals(MOCK_SITE_KEY, config.siteKey)
        assertEquals(true, config.sentry)
        assertEquals(HCaptchaSize.INVISIBLE, config.size)
        assertEquals(HCaptchaOrientation.PORTRAIT, config.orientation)
        assertEquals(HCaptchaTheme.LIGHT, config.theme)
        assertEquals(Locale.getDefault().language, config.locale)
        assertEquals("https://js.hcaptcha.com/1/api.js", config.jsSrc)
        assertEquals(null, config.customTheme)
        assertEquals(true, config.disableHardwareAcceleration)
        Assert.assertNull(config.rqdata)
    }

    @Test
    fun custom_config() {
        val hCaptchaOrientation = HCaptchaOrientation.LANDSCAPE
        val hCaptchaSize = HCaptchaSize.COMPACT
        val hCaptchaTheme = HCaptchaTheme.DARK
        val customRqdata = "custom rqdata value"
        val customEndpoint = "https://local/api.js"
        val customLocale = "ro"
        val sentry = false
        val disableHWAccel = false
        val customTheme = (
            "{ \"palette\": {" +
                "\"mode\": \"light\", \"primary\": { \"main\": \"#F16622\" }," +
                "\"warn\": {  \"main\": \"#F16622\" }," +
                "\"text\": { \"heading\": \"#F16622\", \"body\": \"#F16622\" } } }"
            )

        val config = HCaptchaConfig(
            siteKey = MOCK_SITE_KEY,
            jsSrc = customEndpoint,
            locale = customLocale,
            rqdata = customRqdata,
            sentry = sentry,
            theme = hCaptchaTheme,
            size = hCaptchaSize,
            orientation = hCaptchaOrientation,
            customTheme = customTheme,
            disableHardwareAcceleration = disableHWAccel
        )

        assertEquals(MOCK_SITE_KEY, config.siteKey)
        assertEquals(sentry, config.sentry)
        assertEquals(hCaptchaSize, config.size)
        assertEquals(hCaptchaOrientation, config.orientation)
        assertEquals(hCaptchaTheme, config.theme)
        assertEquals(customLocale, config.locale)
        assertEquals(customRqdata, config.rqdata)
        assertEquals(customEndpoint, config.jsSrc)
        assertEquals(customTheme, config.customTheme)
        assertEquals(disableHWAccel, config.disableHardwareAcceleration)
    }

    @Test
    @Throws(Exception::class)
    fun serialization() {
        val config = HCaptchaConfig(siteKey = MOCK_SITE_KEY)

        val bos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(bos)
        oos.writeObject(config)

        val bis = ByteArrayInputStream(bos.toByteArray())
        val ois = ObjectInputStream(bis)

        val deserializedObject = ois.readObject() as HCaptchaConfig
        assertEquals(
            config.copy(retryPredicate = null),
            deserializedObject.copy(retryPredicate = null)
        )
    }

    companion object {
        const val MOCK_SITE_KEY = "mocked-site-key"
    }
}
