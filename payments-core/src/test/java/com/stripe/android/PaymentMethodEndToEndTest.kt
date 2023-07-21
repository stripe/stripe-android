package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.networking.StripeApiRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class PaymentMethodEndToEndTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun createPaymentMethod_withBacsDebit_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.BACS_DEBIT

        val paymentMethod =
            Stripe(context, ApiKeyFixtures.BACS_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.BacsDebit)
        assertThat(paymentMethod.bacsDebit)
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
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.Sofort)
        assertThat(paymentMethod.sofort)
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
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.P24)
    }

    @Test
    fun createPaymentMethod_withBancontact_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.BANCONTACT
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.BANCONTACT_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod.type)
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
    fun createPaymentMethod_withNetBanking_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.NETBANKING
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.NETBANKING_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.Netbanking)
    }

    @Test
    fun createPaymentMethod_withUSBankAccount_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT
        val paymentMethod =
            Stripe(
                context,
                ApiKeyFixtures.US_BANK_ACCOUNT_PUBLISHABLE_KEY
            ).createPaymentMethodSynchronous(params)
        assertThat(paymentMethod.type).isEqualTo(PaymentMethod.Type.USBankAccount)
        assertThat(paymentMethod.usBankAccount).isEqualTo(
            PaymentMethod.USBankAccount(
                accountHolderType = PaymentMethod.USBankAccount.USBankAccountHolderType.INDIVIDUAL,
                accountType = PaymentMethod.USBankAccount.USBankAccountType.CHECKING,
                bankName = "STRIPE TEST BANK",
                fingerprint = "FFDMA0xfhBjWSZLu",
                last4 = "6789",
                linkedAccount = null,
                networks = PaymentMethod.USBankAccount.USBankNetworks(
                    preferred = "ach",
                    supported = listOf("ach", "us_domestic_wire")
                ),
                routingNumber = "110000000"
            )
        )
    }

    @Test
    fun createPaymentMethod_withUSBankAccount_missingEmail_shouldCreateObject() {
        val params = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT.copy(
            billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(email = null)
        )
        val paymentMethod =
            Stripe(
                context,
                ApiKeyFixtures.US_BANK_ACCOUNT_PUBLISHABLE_KEY
            ).createPaymentMethodSynchronous(params)
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.USBankAccount)
    }

    @Test
    fun createPaymentMethod_withUSBankAccount_missingName_shouldFail() {
        val params = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT.copy(
            billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(name = null)
        )
        val exception = assertFailsWith<InvalidRequestException>(
            "A name is required to create a US Bank Account payment method"
        ) {
            Stripe(
                context,
                ApiKeyFixtures.US_BANK_ACCOUNT_PUBLISHABLE_KEY
            ).createPaymentMethodSynchronous(params)
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
        assertThat(paymentMethod.type)
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
        assertThat(paymentMethod.type)
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
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.Upi)
    }

    @Test
    fun createPaymentMethod_withOxxo_shouldCreatePaymentMethodWithOxxoType() {
        val params = PaymentMethodCreateParams.createOxxo(
            billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS
        )
        val paymentMethod = Stripe(context, ApiKeyFixtures.OXXO_PUBLISHABLE_KEY)
            .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod.type)
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
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.Alipay)
    }

    @Test
    fun createPaymentMethod_withGrabPay_shouldCreateObject() = runTest {
        val repository = StripeApiRepository(
            context,
            { ApiKeyFixtures.GRABPAY_PUBLISHABLE_KEY },
            workContext = testDispatcher
        )

        val params = PaymentMethodCreateParamsFixtures.GRABPAY
        val paymentMethod = repository.createPaymentMethod(
            params,
            ApiRequest.Options(ApiKeyFixtures.GRABPAY_PUBLISHABLE_KEY)
        ).getOrThrow()
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.GrabPay)
    }

    @Test
    fun `createPaymentMethod() with PayPal PaymentMethod should create expected object`() = runTest {
        val paymentMethod = StripeApiRepository(
            context,
            { ApiKeyFixtures.PAYPAL_PUBLISHABLE_KEY },
            workContext = testDispatcher
        ).createPaymentMethod(
            PaymentMethodCreateParams.createPayPal(),
            ApiRequest.Options(ApiKeyFixtures.PAYPAL_PUBLISHABLE_KEY)
        ).getOrThrow()

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
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.AfterpayClearpay)
    }

    @Test
    fun `createPaymentMethod with Afterpay should require name, email`() {
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

        assertThat(missingNameException.message)
            .isEqualTo("Missing required param: billing_details[name].")

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

        assertThat(missingEmailException.message)
            .isEqualTo("Missing required param: billing_details[email].")

        // Address is optional
        assertThat(
            stripe
                .createPaymentMethodSynchronous(
                    PaymentMethodCreateParams.createAfterpayClearpay(
                        billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(address = null)
                    )
                )
        ).isNotNull()
    }

    @Test
    fun createPaymentMethod_withBlik_shouldCreateObject() {
        val params = PaymentMethodCreateParams.createBlik()
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.BLIK_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.Blik)
    }

    @Test
    fun createPaymentMethod_withWeChatPay_shouldCreateObject() {
        val params = PaymentMethodCreateParams.createWeChatPay()
        val paymentMethod =
            Stripe(
                context,
                ApiKeyFixtures.WECHAT_PAY_PUBLISHABLE_KEY,
                betas = setOf(StripeApiBeta.WeChatPayV1)
            )
                .createPaymentMethodSynchronous(params)
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.WeChatPay)
    }

    @Test
    fun createPaymentMethod_withKlarna_shouldCreateObject() {
        val missingAddressException = assertFailsWith<InvalidRequestException>(
            "Address is required to create a klarna payment method"
        ) {
            val params = PaymentMethodCreateParams.createKlarna(
                billingDetails =
                PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(address = null)
            )
            Stripe(context, ApiKeyFixtures.KLARNA_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        }

        assertThat(missingAddressException.message)
            .isEqualTo("You must provide `billing_details[address][country]` to use Klarna.")

        val missingCountryException = assertFailsWith<InvalidRequestException>(
            "Country is required to create a klarna payment method"
        ) {
            val address = Address(country = null)
            val params = PaymentMethodCreateParams.createKlarna(
                billingDetails =
                PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy(address = address)
            )

            Stripe(context, ApiKeyFixtures.KLARNA_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)
        }

        assertThat(missingCountryException.message)
            .isEqualTo("You must provide `billing_details[address][country]` to use Klarna.")

        val params = PaymentMethodCreateParams.createKlarna(
            billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS.copy()
        )

        val paymentMethod =
            Stripe(context, ApiKeyFixtures.KLARNA_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(params)

        assertThat(paymentMethod.type).isEqualTo(PaymentMethod.Type.Klarna)
    }

    @Test
    fun `createPaymentMethod with Affirm should create expected object`() {
        val paymentMethod = Stripe(context, ApiKeyFixtures.AFFIRM_PUBLISHABLE_KEY)
            .createPaymentMethodSynchronous(
                PaymentMethodCreateParams.createAffirm(
                    billingDetails = PaymentMethodCreateParamsFixtures.BILLING_DETAILS
                )
            )
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.Affirm)
    }

    @Test
    fun createPaymentMethod_withCashAppPay_shouldCreateObject() {
        val stripe = Stripe(context, ApiKeyFixtures.CASH_APP_PAY_PUBLISHABLE_KEY)
        val params = PaymentMethodCreateParamsFixtures.CASH_APP_PAY

        val paymentMethod = stripe.createPaymentMethodSynchronous(params)
        assertThat(paymentMethod.type).isEqualTo(PaymentMethod.Type.CashAppPay)
    }
}
