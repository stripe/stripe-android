package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.ExternalPaymentMethodSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.ExternalPaymentMethodType
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodOrderSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestExternalPaymentMethod : BasePlaygroundTest() {

    private val externalFawryCode = "external_fawry"

    private val testParameters = TestParameters.create(
        paymentMethodCode = externalFawryCode,
        executeInNightlyRun = true,
    ) { settings ->
        settings[ExternalPaymentMethodSettingsDefinition] = ExternalPaymentMethodType.Fawry
        settings[PaymentMethodOrderSettingsDefinition] = externalFawryCode
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
