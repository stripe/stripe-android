package com.stripe.android.paymentelement.confirmation.cpms

import android.app.Activity
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CustomPaymentMethodContractTest {
    private val customPaymentMethodContract: CustomPaymentMethodContract = CustomPaymentMethodContract()

    @Test
    fun `on create intent, should have expected extras`() {
        val paymentElementCallbackIdentifier = "CustomPaymentMethodTestIdentifier"
        val customPaymentMethodType = PaymentSheet.CustomPaymentMethod(
            id = "cpmt_123",
            subtitle = "Pay now".resolvableString,
            disableBillingDetailCollection = false,
        )
        val billingDetails = PaymentMethod.BillingDetails(
            name = "John Doe",
        )

        val intent = customPaymentMethodContract.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            input = CustomPaymentMethodInput(
                paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                type = customPaymentMethodType,
                billingDetails = billingDetails
            )
        )

        val extras = intent.extras

        assertThat(
            extras?.getString(CustomPaymentMethodProxyActivity.EXTRA_PAYMENT_ELEMENT_IDENTIFIER)
        ).isEqualTo(paymentElementCallbackIdentifier)
        assertThat(
            extras?.let {
                BundleCompat.getParcelable(
                    it,
                    CustomPaymentMethodProxyActivity.EXTRA_CUSTOM_PAYMENT_METHOD_TYPE,
                    PaymentSheet.CustomPaymentMethod::class.java,
                )
            }
        ).isEqualTo(customPaymentMethodType)
        assertThat(
            extras?.let {
                BundleCompat.getParcelable(
                    it,
                    CustomPaymentMethodProxyActivity.EXTRA_BILLING_DETAILS,
                    PaymentMethod.BillingDetails::class.java,
                )
            }
        ).isEqualTo(billingDetails)
    }

    @Test
    fun `failed result on null intent`() {
        val actualResult = customPaymentMethodContract.parseResult(Activity.RESULT_OK, intent = null)

        assertThat(actualResult).isInstanceOf<InternalCustomPaymentMethodResult.Failed>()

        val failedResult = actualResult as InternalCustomPaymentMethodResult.Failed
        val exception = failedResult.throwable

        assertThat(exception).isInstanceOf<IllegalStateException>()
        assertThat(exception.message).isEqualTo("Failed to find custom payment method result!")
    }

    @Test
    fun `failed result on intent with incorrect arguments`() {
        val actualResult = customPaymentMethodContract.parseResult(
            Activity.RESULT_OK,
            intent = Intent().putExtras(
                bundleOf("unknown_arguments" to InternalCustomPaymentMethodResult.Canceled)
            )
        )

        assertThat(actualResult).isInstanceOf<InternalCustomPaymentMethodResult.Failed>()

        val failedResult = actualResult as InternalCustomPaymentMethodResult.Failed
        val exception = failedResult.throwable

        assertThat(exception).isInstanceOf<IllegalStateException>()
        assertThat(exception.message).isEqualTo("Failed to find custom payment method result!")
    }

    @Test
    fun `should parse out complete result`() {
        val actualResult = customPaymentMethodContract.parseResult(
            Activity.RESULT_OK,
            intent = Intent().putExtras(InternalCustomPaymentMethodResult.Completed.toBundle())
        )

        assertThat(actualResult).isEqualTo(InternalCustomPaymentMethodResult.Completed)
    }

    @Test
    fun `should parse out canceled result`() {
        val actualResult = customPaymentMethodContract.parseResult(
            Activity.RESULT_OK,
            intent = Intent().putExtras(InternalCustomPaymentMethodResult.Canceled.toBundle())
        )

        assertThat(actualResult).isEqualTo(InternalCustomPaymentMethodResult.Canceled)
    }

    @Test
    fun `should parse out failed result`() {
        val exception = LocalStripeException(
            displayMessage = "Failed to complete CPM payment",
            analyticsValue = "cpmFailedSadly"
        )

        val actualResult = customPaymentMethodContract.parseResult(
            Activity.RESULT_OK,
            intent = Intent().putExtras(InternalCustomPaymentMethodResult.Failed(exception).toBundle())
        )

        assertThat(actualResult).isInstanceOf<InternalCustomPaymentMethodResult.Failed>()

        val failedResult = actualResult as InternalCustomPaymentMethodResult.Failed

        assertThat(failedResult.throwable).isEqualTo(exception)
    }
}
