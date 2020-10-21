package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
    fun createPaymentMethod_withUpi_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.UPI
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.UPI_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.Upi)
    }

    @Test
    fun createPaymentMethod_withOxxo_shouldCreatePaymentMethodWithOxxoType() {
        val params = PaymentMethodCreateParams.createOxxo(
            billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS
        )
        val paymentMethod = Stripe(context, ApiKeyFixtures.OXXO_PUBLISHABLE_KEY)
            .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.Oxxo)
    }

    @Test
    fun createPaymentMethod_withOxxo_shouldRequireNameAndEmail() {
        val stripe = Stripe(context, ApiKeyFixtures.OXXO_PUBLISHABLE_KEY)
        val missingNameException = assertFailsWith<InvalidRequestException>(
            "A name is required to create an OXXO payment method."
        ) {
            stripe
                .createPaymentMethodSynchronous(
                    PaymentMethodCreateParams.createOxxo(
                        billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(
                            name = null
                        )
                    )
                )
        }
        assertThat(missingNameException.message)
            .isEqualTo("Missing required param: billing_details[name].")

        val missingEmailException = assertFailsWith<InvalidRequestException>(
            "An email is required to create an OXXO payment method."
        ) {
            stripe.createPaymentMethodSynchronous(
                PaymentMethodCreateParams.createOxxo(
                    billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(
                        email = null
                    )
                )
            )
        }
        assertThat(missingEmailException.message)
            .isEqualTo("Missing required param: billing_details[email].")
    }

    @Test
    fun createPaymentMethod_withAlipay_shouldCreateObject() {
        val params = PaymentMethodCreateParams.createAlipay()
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.ALIPAY_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.Alipay)
    }

    @Test
    fun createPaymentMethod_withGrabPay_shouldCreateObject() {
        val repository = StripeApiRepository(
            context,
            ApiKeyFixtures.GRABPAY_PUBLISHABLE_KEY
        )

        val params = PaymentMethodCreateParamsFixtures.GRABPAY
        val paymentMethod = repository.createPaymentMethod(
            params,
            ApiRequest.Options(ApiKeyFixtures.GRABPAY_PUBLISHABLE_KEY)
        )
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.GrabPay)
    }

    @Test
    fun `createPaymentMethod() with PayPal PaymentMethod should create expected object`() {
        val paymentMethod = StripeApiRepository(
            context,
            ApiKeyFixtures.PAYPAL_PUBLISHABLE_KEY
        ).createPaymentMethod(
            PaymentMethodCreateParams.createPayPal(),
            ApiRequest.Options(ApiKeyFixtures.PAYPAL_PUBLISHABLE_KEY)
        )

        requireNotNull(paymentMethod)
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.PayPal)
    }

    @Test
    fun `createPaymentMethod with Afterpay should create expected object`() {
        val paymentMethod = Stripe(context, ApiKeyFixtures.AFTERPAY_PUBLISHABLE_KEY)
            .createPaymentMethodSynchronous(
                PaymentMethodCreateParams.createAfterpayClearpay(
                    billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS
                )
            )
        assertThat(paymentMethod?.type)
            .isEqualTo(PaymentMethod.Type.AfterpayClearpay)
    }

    @Test
    fun `createPaymentMethod with Afterpay should require name, email, and address`() {
        val stripe = Stripe(context, ApiKeyFixtures.AFTERPAY_PUBLISHABLE_KEY)
        val missingNameException = assertFailsWith<InvalidRequestException>(
            "Name is required to create an Afterpay payment method"
        ) {
            stripe
                .createPaymentMethodSynchronous(
                    PaymentMethodCreateParams.createAfterpayClearpay(
                        billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(name = null)
                    )
                )
        }

        assertThat(missingNameException.message).isEqualTo("Missing required param: billing_details[name].")

        val missingEmailException = assertFailsWith<InvalidRequestException>(
            "Email is required to create an Afterpay payment method"
        ) {
            stripe
                .createPaymentMethodSynchronous(
                    PaymentMethodCreateParams.createAfterpayClearpay(
                        billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(email = null)
                    )
                )
        }

        assertThat(missingEmailException.message).isEqualTo("Missing required param: billing_details[email].")

        val missingAddressException = assertFailsWith<InvalidRequestException>(
            "Email is required to create an Afterpay payment method"
        ) {
            stripe
                .createPaymentMethodSynchronous(
                    PaymentMethodCreateParams.createAfterpayClearpay(
                        billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(address = null)
                    )
                )
        }

        assertThat(missingAddressException.message).isEqualTo("Missing required param: billing_details[address][line1].")
    }
}
