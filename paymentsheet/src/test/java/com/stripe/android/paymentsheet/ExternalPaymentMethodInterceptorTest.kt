package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultLauncher
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.testing.FakeErrorReporter
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.Test

class ExternalPaymentMethodInterceptorTest {

    @Test
    fun `intercept with null external payment confirm handler logs and fails`() {
        val errorReporter = FakeErrorReporter()
        var paymentResult: PaymentResult? = null
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = null

        ExternalPaymentMethodInterceptor.intercept(
            externalPaymentMethodType = "external_example",
            billingDetails = null,
            onPaymentResult = { paymentResult = it },
            externalPaymentMethodLauncher = mock<ActivityResultLauncher<ExternalPaymentMethodInput>>(),
            errorReporter = errorReporter,
        )

        assertThat(paymentResult).isInstanceOf(PaymentResult.Failed::class.java)
        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.ExpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_CONFIRM_HANDLER_NULL.eventName
        )
    }

    @Test
    fun `intercept with null external payment launcher logs and fails`() {
        val errorReporter = FakeErrorReporter()
        var paymentResult: PaymentResult? = null
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler =
            DefaultExternalPaymentMethodConfirmHandler

        ExternalPaymentMethodInterceptor.intercept(
            externalPaymentMethodType = "external_example",
            billingDetails = null,
            onPaymentResult = { paymentResult = it },
            externalPaymentMethodLauncher = null,
            errorReporter = errorReporter,
        )

        assertThat(paymentResult).isInstanceOf(PaymentResult.Failed::class.java)
        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.ExpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_LAUNCHER_NULL.eventName
        )
    }

    @Test
    fun `intercept with correct params succeeds`() {
        val errorReporter = FakeErrorReporter()
        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler =
            DefaultExternalPaymentMethodConfirmHandler

        ExternalPaymentMethodInterceptor.intercept(
            externalPaymentMethodType = "external_example",
            billingDetails = null,
            onPaymentResult = {},
            externalPaymentMethodLauncher = mock<ActivityResultLauncher<ExternalPaymentMethodInput>?>().also {
                whenever(it.launch(any())).then {}
            },
            errorReporter = errorReporter,
        )

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.SuccessEvent.EXTERNAL_PAYMENT_METHODS_LAUNCH_SUCCESS.eventName
        )
    }

    object DefaultExternalPaymentMethodConfirmHandler : ExternalPaymentMethodConfirmHandler {
        override fun confirmExternalPaymentMethod(
            externalPaymentMethodType: String,
            billingDetails: PaymentMethod.BillingDetails
        ) {
            // Do nothing.
        }
    }
}
