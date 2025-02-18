package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class ExternalPaymentMethodContractTest {

    @Test
    fun completedResultParsedCorrectly() {
        val externalPaymentMethodContract = ExternalPaymentMethodContract(FakeErrorReporter())
        val actualResult = externalPaymentMethodContract.parseResult(Activity.RESULT_OK, intent = null)

        assertThat(actualResult).isEqualTo(PaymentResult.Completed)
    }

    @Test
    fun canceledResultParsedCorrectly() {
        val externalPaymentMethodContract = ExternalPaymentMethodContract(FakeErrorReporter())
        val actualResult = externalPaymentMethodContract.parseResult(Activity.RESULT_CANCELED, intent = null)

        assertThat(actualResult).isEqualTo(PaymentResult.Canceled)
    }

    @Test
    fun failedResultParsedCorrectly() {
        val externalPaymentMethodContract = ExternalPaymentMethodContract(FakeErrorReporter())
        val expectedErrorMessage = "external payment method payment failed"
        val intent = Intent().putExtra(ExternalPaymentMethodResult.Failed.DISPLAY_MESSAGE_EXTRA, expectedErrorMessage)

        val actualResult = externalPaymentMethodContract.parseResult(Activity.RESULT_FIRST_USER, intent = intent)

        assertThat(actualResult is PaymentResult.Failed).isTrue()
        assertThat((actualResult as PaymentResult.Failed).throwable.message).isEqualTo(expectedErrorMessage)
    }

    @Test
    fun invalidResultCodeParsedCorrectly() = runTest {
        val errorReporter = FakeErrorReporter()
        val externalPaymentMethodContract = ExternalPaymentMethodContract(errorReporter)
        val expectedErrorMessage = "Invalid result code returned by external payment method activity"
        val intent = Intent().putExtra(ExternalPaymentMethodResult.Failed.DISPLAY_MESSAGE_EXTRA, expectedErrorMessage)

        val actualResult = externalPaymentMethodContract.parseResult(resultCode = 100, intent = intent)

        assertThat(actualResult is PaymentResult.Failed).isTrue()
        assertThat((actualResult as PaymentResult.Failed).throwable.message).isEqualTo(expectedErrorMessage)
        assertThat(errorReporter.awaitCall().errorEvent).isEqualTo(
            ErrorReporter.UnexpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_UNEXPECTED_RESULT_CODE
        )
    }
}
