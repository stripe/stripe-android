package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ElementsSessionTest {

    @Test
    fun `passiveCaptchaParams returns passiveCaptcha when flag is enabled`() {
        val passiveCaptcha = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data"
        )

        val session = createElementsSession(
            passiveCaptcha = passiveCaptcha,
            flags = mapOf(ElementsSession.Flag.ELEMENTS_ENABLE_PASSIVE_CAPTCHA to true)
        )

        assertThat(session.passiveCaptchaParams).isEqualTo(passiveCaptcha)
    }

    @Test
    fun `passiveCaptchaParams returns null when flag is disabled`() {
        val passiveCaptcha = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data"
        )

        val session = createElementsSession(
            passiveCaptcha = passiveCaptcha,
            flags = mapOf(ElementsSession.Flag.ELEMENTS_ENABLE_PASSIVE_CAPTCHA to false)
        )

        assertThat(session.passiveCaptchaParams).isNull()
    }

    @Test
    fun `passiveCaptchaParams returns null when flag is missing`() {
        val passiveCaptcha = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data"
        )

        val session = createElementsSession(
            passiveCaptcha = passiveCaptcha,
            flags = emptyMap()
        )

        assertThat(session.passiveCaptchaParams).isNull()
    }

    @Test
    fun `passiveCaptchaParams returns null when passiveCaptcha is null even if flag is enabled`() {
        val session = createElementsSession(
            passiveCaptcha = null,
            flags = mapOf(ElementsSession.Flag.ELEMENTS_ENABLE_PASSIVE_CAPTCHA to true)
        )

        assertThat(session.passiveCaptchaParams).isNull()
    }

    @Test
    fun `ELEMENTS_ENABLE_PASSIVE_CAPTCHA flag has correct value`() {
        assertThat(ElementsSession.Flag.ELEMENTS_ENABLE_PASSIVE_CAPTCHA.flagValue)
            .isEqualTo("elements_enable_passive_captcha")
    }

    private fun createElementsSession(
        passiveCaptcha: PassiveCaptchaParams?,
        flags: Map<ElementsSession.Flag, Boolean>
    ): ElementsSession {
        return ElementsSession(
            linkSettings = null,
            paymentMethodSpecs = null,
            externalPaymentMethodData = null,
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            orderedPaymentMethodTypesAndWallets = emptyList(),
            flags = flags,
            experimentsData = null,
            customer = null,
            merchantCountry = null,
            cardBrandChoice = null,
            isGooglePayEnabled = false,
            sessionsError = null,
            customPaymentMethods = emptyList(),
            elementsSessionId = "elements_session_test",
            passiveCaptcha = passiveCaptcha
        )
    }
}
