package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.SetupIntentFixtures
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class SetupIntentJsonParserTest {
    @Test
    fun parse_withNotExpandedPaymentMethod_shouldCreateExpectedObject() {
        val setupIntent = requireNotNull(
            SetupIntentJsonParser().parse(SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT_JSON)
        )
        assertThat(setupIntent.paymentMethodId)
            .isEqualTo("pm_1EqTSoGMT9dGPIDG7dgafX1H")
        assertThat(setupIntent.paymentMethod)
            .isNull()
    }

    @Test
    fun parse_withExpandedPaymentMethod_shouldCreateExpectedObject() {
        val setupIntent = requireNotNull(
            SetupIntentJsonParser().parse(SetupIntentFixtures.EXPANDED_PAYMENT_METHOD)
        )
        assertThat(setupIntent.paymentMethodId)
            .isEqualTo("pm_1GSmaGCRMbs6F")
        assertThat(setupIntent.paymentMethod?.id)
            .isEqualTo("pm_1GSmaGCRMbs6F")
    }

    @Test
    fun parse_withCountryCode_shouldCreateExpectedObject() {
        val setupIntent = requireNotNull(
            SetupIntentJsonParser().parse(SetupIntentFixtures.SI_WITH_COUNTRY_CODE)
        )
        assertThat(setupIntent.countryCode).isEqualTo("CA")
    }

    @Test
    fun `automaticPaymentMethodsEnabled=false when automatic payment methods field is not present`() {
        val setupIntent = requireNotNull(
            SetupIntentJsonParser().parse(SetupIntentFixtures.SI_WITH_COUNTRY_CODE)
        )

        assertThat(setupIntent.automaticPaymentMethodsEnabled).isFalse()
    }

    @Test
    fun `automaticPaymentMethodsEnabled=false when automatic payments field enabled=false`() {
        val setupIntent =
            requireNotNull(
                SetupIntentJsonParser().parse(
                    SetupIntentFixtures.SI_WITH_AUTOMATIC_PAYMENT_METHODS_NOT_ENABLED
                )
            )

        assertThat(setupIntent.automaticPaymentMethodsEnabled).isFalse()
    }

    @Test
    fun `automaticPaymentMethodsEnabled=true when automatic payments field enabled=true`() {
        val setupIntent =
            requireNotNull(
                SetupIntentJsonParser().parse(
                    SetupIntentFixtures.SI_WITH_AUTOMATIC_PAYMENT_METHODS_ENABLED
                )
            )

        assertThat(setupIntent.automaticPaymentMethodsEnabled).isTrue()
    }
}
