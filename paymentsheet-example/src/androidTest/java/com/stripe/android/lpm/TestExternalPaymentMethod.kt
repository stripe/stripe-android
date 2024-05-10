package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.ExternalPaymentMethodSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodOrderSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestExternalPaymentMethod : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "external_fawry",
    ) { settings ->
        settings[ExternalPaymentMethodSettingsDefinition] = "external_fawry, external_venmo"
        settings[PaymentMethodOrderSettingsDefinition] = "card, external_venmo, external_fawry"
        settings[SupportedPaymentMethodsSettingsDefinition] = listOf(
            PaymentMethod.Type.Card,
        ).joinToString(",")
    }

    @Test
    fun testExternalPaymentMethod_Success() {
        testDriver.confirmExternalPaymentMethodSuccess(testParameters)
    }

    @Test
    fun testExternalPaymentMethod_Cancel() {
        testDriver.confirmExternalPaymentMethodCanceled(testParameters)
    }

    @Test
    fun testExternalPaymentMethod_Fail() {
        testDriver.confirmExternalPaymentMethodFailed(testParameters)
    }

}