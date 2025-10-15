package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test

class ElementsSessionTest {

    @get:Rule
    val enablePassiveCaptchaRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enablePassiveCaptcha,
        isEnabled = true
    )

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
    fun `passiveCaptchaParams returns null when elements flag is disabled`() {
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
    fun `passiveCaptchaParams returns null when elements flag is missing`() {
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
    fun `passiveCaptchaParams returns null when feature flag is disabled`() {
        enablePassiveCaptchaRule.setEnabled(false)
        val passiveCaptcha = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test_rq_data"
        )

        val session = createElementsSession(
            passiveCaptcha = passiveCaptcha,
            flags = mapOf(ElementsSession.Flag.ELEMENTS_ENABLE_PASSIVE_CAPTCHA to true)
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

    @Test
    fun `ELEMENTS_MOBILE_ATTEST_ON_INTENT_CONFIRMATION flag has correct value`() {
        assertThat(ElementsSession.Flag.ELEMENTS_MOBILE_ATTEST_ON_INTENT_CONFIRMATION.flagValue)
            .isEqualTo("elements_mobile_attest_on_intent_confirmation")
    }

    @Test
    fun `enableAttestationOnIntentConfirmation returns true when flag is enabled`() {
        val session = createElementsSession(
            passiveCaptcha = null,
            flags = mapOf(ElementsSession.Flag.ELEMENTS_MOBILE_ATTEST_ON_INTENT_CONFIRMATION to true)
        )

        assertThat(session.enableAttestationOnIntentConfirmation).isTrue()
    }

    @Test
    fun `enableAttestationOnIntentConfirmation returns false when flag is disabled`() {
        val session = createElementsSession(
            passiveCaptcha = null,
            flags = mapOf(ElementsSession.Flag.ELEMENTS_MOBILE_ATTEST_ON_INTENT_CONFIRMATION to false)
        )

        assertThat(session.enableAttestationOnIntentConfirmation).isFalse()
    }

    @Test
    fun `enableAttestationOnIntentConfirmation returns false when flag is missing`() {
        val session = createElementsSession(
            passiveCaptcha = null,
            flags = emptyMap()
        )

        assertThat(session.enableAttestationOnIntentConfirmation).isFalse()
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
            merchantLogoUrl = null,
            cardBrandChoice = null,
            isGooglePayEnabled = false,
            sessionsError = null,
            customPaymentMethods = emptyList(),
            elementsSessionId = "elements_session_test",
            passiveCaptcha = passiveCaptcha,
        )
    }
}
