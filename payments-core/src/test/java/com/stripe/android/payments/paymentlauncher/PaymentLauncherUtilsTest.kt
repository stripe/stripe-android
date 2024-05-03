package com.stripe.android.payments.paymentlauncher

import com.stripe.android.model.PaymentIntent
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class PaymentLauncherUtilsTest {
    private val paymentIntent = mock<PaymentIntent>()

    @Test
    fun `Converted 'PaymentResultCallback' should return completed result`() {
        val mockedResultCallback = mock<PaymentLauncher.PaymentResultCallback>()

        val launcherResultCallback = mockedResultCallback.toInternalResultCallback()

        launcherResultCallback.onPaymentResult(InternalPaymentResult.Completed(paymentIntent))
        verify(mockedResultCallback).onPaymentResult(PaymentResult.Completed)
    }

    @Test
    fun `Converted 'PaymentResultCallback' should return failed result`() {
        val mockedResultCallback = mock<PaymentLauncher.PaymentResultCallback>()

        val launcherResultCallback = mockedResultCallback.toInternalResultCallback()

        launcherResultCallback.onPaymentResult(InternalPaymentResult.Failed(Exception()))
        verify(mockedResultCallback).onPaymentResult(any<PaymentResult.Failed>())
    }

    @Test
    fun `Converted 'PaymentResultCallback' should return canceled result`() {
        val mockedResultCallback = mock<PaymentLauncher.PaymentResultCallback>()

        val launcherResultCallback = mockedResultCallback.toInternalResultCallback()

        launcherResultCallback.onPaymentResult(InternalPaymentResult.Canceled)
        verify(mockedResultCallback).onPaymentResult(PaymentResult.Canceled)
    }

    @Test
    fun `Converted 'PaymentResult' lambda should return completed result`() {
        val mockedResultCallback = mock<(result: PaymentResult) -> Unit>()

        val launcherResultCallback = toInternalPaymentResultCallback(mockedResultCallback)

        launcherResultCallback(InternalPaymentResult.Completed(paymentIntent))
        verify(mockedResultCallback).invoke(PaymentResult.Completed)
    }

    @Test
    fun `Converted 'PaymentResult' lambda should return failed result`() {
        val mockedResultCallback = mock<(result: PaymentResult) -> Unit>()

        val launcherResultCallback = toInternalPaymentResultCallback(mockedResultCallback)

        val exception = Exception()

        launcherResultCallback(InternalPaymentResult.Failed(exception))
        verify(mockedResultCallback).invoke(any<PaymentResult.Failed>())
    }

    @Test
    fun `Converted 'PaymentResult' lambda should return canceled result`() {
        val mockedResultCallback = mock<(result: PaymentResult) -> Unit>()

        val launcherResultCallback = toInternalPaymentResultCallback(mockedResultCallback)

        launcherResultCallback(InternalPaymentResult.Canceled)
        verify(mockedResultCallback).invoke(PaymentResult.Canceled)
    }
}
