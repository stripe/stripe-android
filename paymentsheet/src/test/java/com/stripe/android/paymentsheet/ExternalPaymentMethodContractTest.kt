package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.stripe.android.payments.paymentlauncher.PaymentResult
import kotlin.test.Test

internal class ExternalPaymentMethodContractTest {

    private val externalPaymentMethodContract: ExternalPaymentMethodContract = ExternalPaymentMethodContract()

    @Test
    fun completedResultParsedCorrectly() {
        val actualResult = externalPaymentMethodContract.parseResult(Activity.RESULT_OK, intent = null)

        assertThat(actualResult).isEqualTo(PaymentResult.Completed)
    }

    @Test
    fun canceledResultParsedCorrectly() {
        val actualResult = externalPaymentMethodContract.parseResult(Activity.RESULT_CANCELED, intent = null)

        assertThat(actualResult).isEqualTo(PaymentResult.Canceled)
    }

    @Test
    fun failedResultParsedCorrectly() {
        val expectedErrorMessage = "external payment method payment failed"
        val intent = Intent().putExtra(ExternalPaymentMethodResult.Failed.ERROR_MESSAGE_EXTRA, expectedErrorMessage)

        val actualResult = externalPaymentMethodContract.parseResult(Activity.RESULT_FIRST_USER, intent = intent)

        assertThat(actualResult is PaymentResult.Failed).isTrue()
        assertThat((actualResult as PaymentResult.Failed).throwable.message).isEqualTo(expectedErrorMessage)
    }

    @Test
    fun invalidResultCodeParsedCorrectly() {
        val expectedErrorMessage = "Invalid result code returned by external payment method activity"
        val intent = Intent().putExtra(ExternalPaymentMethodResult.Failed.ERROR_MESSAGE_EXTRA, expectedErrorMessage)

        val actualResult = externalPaymentMethodContract.parseResult(resultCode = 100, intent = intent)

        assertThat(actualResult is PaymentResult.Failed).isTrue()
        assertThat((actualResult as PaymentResult.Failed).throwable.message).isEqualTo(expectedErrorMessage)
    }
}
