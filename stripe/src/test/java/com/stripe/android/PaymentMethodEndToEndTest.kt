package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import kotlin.test.Test
import kotlin.test.assertFailsWith
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodEndToEndTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun createPaymentMethod_withBacsDebit_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.BACS_DEBIT

        val paymentMethod =
            Stripe(context, ApiKeyFixtures.BACS_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.BacsDebit)
        assertThat(paymentMethod?.bacsDebit)
            .isEqualTo(
                PaymentMethod.BacsDebit(
                    fingerprint = "UkSG0HfCGxxrja1H",
                    last4 = "2345",
                    sortCode = "108800"
                )
            )
    }

    @Test
    fun createPaymentMethod_withSofort_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.SOFORT
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.SOFORT_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.Sofort)
        assertThat(paymentMethod?.sofort)
            .isEqualTo(
                PaymentMethod.Sofort(
                    country = "DE"
                )
            )
    }

    @Test
    fun createPaymentMethod_withP24_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.P24
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.P24_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.P24)
    }

    @Test
    fun createPaymentMethod_withBancontact_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.BANCONTACT
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.BANCONTACT_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.Bancontact)
    }

    @Test
    fun createPaymentMethod_withBancontact_missingName_shouldFail() {
        val params = PaymentMethodCreateParams.createBancontact(
            billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(name = null)
        )

        val exception = assertFailsWith<InvalidRequestException>(
            "A name is required to create a Bancontact payment method"
        ) {
            Stripe(context, ApiKeyFixtures.BANCONTACT_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        }
        assertThat(exception.message)
            .isEqualTo("Missing required param: billing_details[name].")
    }

    @Test
    fun createPaymentMethod_withGiropay_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.GIROPAY
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.GIROPAY_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.Giropay)
    }

    @Test
    fun createPaymentMethod_withGiropay_missingName_shouldFail() {
        val params = PaymentMethodCreateParams.createGiropay(
            billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(name = null)
        )
        val exception = assertFailsWith<InvalidRequestException>(
            "A name is required to create a Giropay payment method"
        ) {
            Stripe(context, ApiKeyFixtures.GIROPAY_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        }
        assertThat(exception.message)
            .isEqualTo("Missing required param: billing_details[name].")
    }

    @Test
    fun createPaymentMethod_withEps_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.EPS
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.EPS_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.Eps)
    }

    @Test
    fun createPaymentMethod_withEps_missingName_shouldFail() {
        val params = PaymentMethodCreateParams.createEps(
            billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(name = null)
        )
        val exception = assertFailsWith<InvalidRequestException>(
            "A name is required to create a EPS payment method"
        ) {
            Stripe(context, ApiKeyFixtures.EPS_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        }
        assertThat(exception.message)
            .isEqualTo("Missing required param: billing_details[name].")
    }

    @Test
    fun createPaymentMethod_withOxxo_shouldCreatePaymentMethodWithOxxoType() {
        val repository = StripeApiRepository(
            context,
            ApiKeyFixtures.OXXO_PUBLISHABLE_KEY,
            apiVersion = "2020-03-02;oxxo_beta=v1"
        )
        val paymentMethod = repository.createPaymentMethod(
            PaymentMethodCreateParams.createOxxo(
                billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS
            ),
            ApiRequest.Options(
                ApiKeyFixtures.OXXO_PUBLISHABLE_KEY
            )
        )
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.Oxxo)
    }

    @Test
    fun createPaymentMethod_withOxxo_shouldRequireNameAndEmail() {
        val repository = StripeApiRepository(
            context,
            ApiKeyFixtures.OXXO_PUBLISHABLE_KEY,
            apiVersion = "2020-03-02;oxxo_beta=v1"
        )

        val missingNameException = assertFailsWith<InvalidRequestException>(
            "A name is required to create an OXXO payment method."
        ) {
            repository.createPaymentMethod(
                PaymentMethodCreateParams.createOxxo(
                    billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(
                        name = null
                    )
                ),
                ApiRequest.Options(
                    ApiKeyFixtures.OXXO_PUBLISHABLE_KEY
                )
            )
        }
        assertThat(missingNameException.message)
            .isEqualTo("Missing required param: billing_details[name].")

        val missingEmailException = assertFailsWith<InvalidRequestException>(
            "An email is required to create an OXXO payment method."
        ) {
            repository.createPaymentMethod(
                PaymentMethodCreateParams.createOxxo(
                    billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(
                        email = null
                    )
                ),
                ApiRequest.Options(
                    ApiKeyFixtures.OXXO_PUBLISHABLE_KEY
                )
            )
        }
        assertThat(missingEmailException.message)
            .isEqualTo("Missing required param: billing_details[email].")
    }

    @Test
    fun createPaymentMethod_withAlipay_shouldCreateObject() {
        val repository = StripeApiRepository(
            context,
            ApiKeyFixtures.ALIPAY_PUBLISHABLE_KEY,
            apiVersion = "2020-03-02;alipay_beta=v1"
        )

        val paymentMethod = repository.createPaymentMethod(
            PaymentMethodCreateParams.createAlipay(),
            ApiRequest.Options(ApiKeyFixtures.ALIPAY_PUBLISHABLE_KEY)
        )
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.Alipay)
    }
}
