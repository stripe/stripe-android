package com.stripe.android.test.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.attachPaymentMethod
import com.stripe.android.confirmPaymentIntent
import com.stripe.android.confirmSetupIntent
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.createPaymentMethod
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.MandateDataParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.updatePaymentMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test

private const val FrenchPublishableKey = "pk_test_51JtgfQKG6vc7r7YCU0qQNOkDaaHrEgeHgGKrJMNfuWwaKgXMLzPUA1f8ZlCNPonIROLOnzpUnJK1C1xFH3M3Mz8X00Q6O4GfUt"

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
internal class EndToEndTest {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val settings: Settings by lazy {
        Settings(context)
    }

    private val service = ServiceFactory().create(
        baseUrl = settings.backendUrl
    )

    /**
     * MARK: PAP.01.08d
     * In this test, a PaymentIntent object is created from an example merchant backend,
     * confirmed by the Android SDK, and then retrieved to validate that the original amount,
     * currency, and merchant are the same as the original inputs.
     *
     * https://confluence.corp.stripe.com/x/dAHfHQ
     */
    @Test
    fun testRigCon() = runTest {
        // Create a PaymentIntent on the backend
        val newPaymentIntent = service.createCardPaymentIntent()

        val stripe = Stripe(context, newPaymentIntent.publishableKey)

        // Confirm the PaymentIntent using a test card
        val confirmedPaymentIntent = requireNotNull(
            stripe.confirmPaymentIntentSynchronous(
                ConfirmPaymentIntentParams
                    .createWithPaymentMethodCreateParams(
                        clientSecret = newPaymentIntent.clientSecret,
                        paymentMethodCreateParams = CARD_PAYMENT_METHOD_CREATE_PARAMS
                    )
                    .withShouldUseStripeSdk(true)
            )
        )

        val expectedPaymentIntentData = service.fetchCardPaymentIntent(
            id = requireNotNull(confirmedPaymentIntent.id)
        )
        // Check the PI information using the backend
        assertThat(newPaymentIntent.amount)
            .isEqualTo(expectedPaymentIntentData.amount)
        assertThat(newPaymentIntent.accountId)
            .isEqualTo(expectedPaymentIntentData.onBehalfOf)
        assertThat(newPaymentIntent.currency)
            .isEqualTo(expectedPaymentIntentData.currency)

        val retrievedPaymentIntent = requireNotNull(
            stripe.retrievePaymentIntentSynchronous(newPaymentIntent.clientSecret)
        )
        // The client can't check the "on_behalf_of" field, so we check it via the merchant test above.
        assertThat(retrievedPaymentIntent.amount)
            .isEqualTo(expectedPaymentIntentData.amount)
        assertThat(retrievedPaymentIntent.currency)
            .isEqualTo(expectedPaymentIntentData.currency)
        assertThat(requireNotNull(retrievedPaymentIntent.status))
            .isEqualTo(StripeIntent.Status.Succeeded)
    }

    @Test
    fun testCreateAndConfirmPaymentIntent() = runTest {
        // Create a PaymentIntent on the backend
        val newPaymentIntent = service.createPaymentIntent(
            Request.CreatePaymentIntentParams(
                createParams = Request.CreateParams(
                    paymentMethodTypes = listOf("card")
                )
            )
        )

        val stripe = Stripe(context, settings.publishableKey)

        // Confirm the PaymentIntent using a test card
        val confirmedPaymentIntent = requireNotNull(
            stripe.confirmPaymentIntentSynchronous(
                ConfirmPaymentIntentParams
                    .createWithPaymentMethodCreateParams(
                        clientSecret = newPaymentIntent.secret,
                        paymentMethodCreateParams = CARD_PAYMENT_METHOD_CREATE_PARAMS
                    )
                    .withShouldUseStripeSdk(true)
            )
        )

        assertThat(confirmedPaymentIntent.amount)
            .isEqualTo(100)
        assertThat(confirmedPaymentIntent.currency)
            .isEqualTo("usd")
        assertThat(requireNotNull(confirmedPaymentIntent.status))
            .isEqualTo(StripeIntent.Status.Succeeded)
    }

    @Test
    fun testCreateAndConfirmSetupIntent() = runTest {
        // Create a SetupIntent on the backend
        val newPaymentIntent = service.createSetupIntent(
            Request.CreateSetupIntentParams(
                createParams = Request.CreateParams(
                    paymentMethodTypes = listOf("card")
                )
            )
        )

        val stripe = Stripe(context, settings.publishableKey)

        // Confirm the SetupIntent using a test card
        val confirmedSetupIntent = requireNotNull(
            stripe.confirmSetupIntentSynchronous(
                ConfirmSetupIntentParams
                    .create(
                        clientSecret = newPaymentIntent.secret,
                        paymentMethodCreateParams = CARD_PAYMENT_METHOD_CREATE_PARAMS
                    )
                    .withShouldUseStripeSdk(true)
            )
        )

        assertThat(requireNotNull(confirmedSetupIntent.status))
            .isEqualTo(StripeIntent.Status.Succeeded)
    }

    @Test
    fun `test us_bank_account payment intent flow with amounts`() = runTest {
        val stripe = Stripe(context, settings.publishableKey)

        // Create a PaymentIntent on the backend
        val newPaymentIntent = service.createPaymentIntent(
            Request.CreatePaymentIntentParams(
                createParams = Request.CreateParams(
                    paymentMethodTypes = listOf("us_bank_account")
                )
            )
        )

        // Confirm the PaymentIntent with customers details collected from merchant app
        val confirmedPaymentIntent = requireNotNull(
            stripe.confirmPaymentIntentSynchronous(
                ConfirmPaymentIntentParams
                    .createWithPaymentMethodCreateParams(
                        paymentMethodCreateParams = US_BANK_ACCOUNT_PAYMENT_METHOD_CREATE_PARAMS,
                        clientSecret = newPaymentIntent.secret,
                        paymentMethodOptions = PaymentMethodOptionsParams.USBankAccount(
                            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                        ),
                        mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
                    )
            )
        )

        assertThat(confirmedPaymentIntent.amount)
            .isEqualTo(100)
        assertThat(confirmedPaymentIntent.currency)
            .isEqualTo("usd")
        assertThat(requireNotNull(confirmedPaymentIntent.status))
            .isEqualTo(StripeIntent.Status.RequiresAction)
        assertThat(requireNotNull(confirmedPaymentIntent.nextActionData))
            .isInstanceOf(StripeIntent.NextActionData.VerifyWithMicrodeposits::class.java)
        assertThat(requireNotNull(confirmedPaymentIntent.nextActionType))
            .isEqualTo(StripeIntent.NextActionType.VerifyWithMicrodeposits)

        // Verify the bank account
        val verifiedPaymentIntent = suspendCoroutine<PaymentIntent> {
            stripe.verifyPaymentIntentWithMicrodeposits(
                clientSecret = confirmedPaymentIntent.clientSecret!!,
                firstAmount = 32,
                secondAmount = 45,
                callback = object : ApiResultCallback<PaymentIntent> {
                    override fun onSuccess(result: PaymentIntent) {
                        it.resume(result)
                    }

                    override fun onError(e: Exception) {
                        it.resumeWithException(e)
                    }
                }
            )
        }

        assertThat(verifiedPaymentIntent.status)
            .isEqualTo(StripeIntent.Status.Processing)
    }

    @Test(expected = InvalidRequestException::class)
    fun `test us_bank_account payment intent flow with amounts fails`() = runTest {
        val stripe = Stripe(context, settings.publishableKey)

        // Create a PaymentIntent on the backend
        val newPaymentIntent = service.createPaymentIntent(
            Request.CreatePaymentIntentParams(
                createParams = Request.CreateParams(
                    paymentMethodTypes = listOf("us_bank_account")
                )
            )
        )

        // Confirm the PaymentIntent with customers details collected from merchant app
        val confirmedPaymentIntent = requireNotNull(
            stripe.confirmPaymentIntentSynchronous(
                ConfirmPaymentIntentParams
                    .createWithPaymentMethodCreateParams(
                        paymentMethodCreateParams = US_BANK_ACCOUNT_PAYMENT_METHOD_CREATE_PARAMS,
                        clientSecret = newPaymentIntent.secret,
                        paymentMethodOptions = PaymentMethodOptionsParams.USBankAccount(
                            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                        ),
                        mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
                    )
            )
        )

        // Verify the bank account with invalid request
        suspendCoroutine<PaymentIntent> {
            stripe.verifyPaymentIntentWithMicrodeposits(
                clientSecret = confirmedPaymentIntent.clientSecret!!,
                firstAmount = 10,
                secondAmount = 11,
                callback = object : ApiResultCallback<PaymentIntent> {
                    override fun onSuccess(result: PaymentIntent) {
                        it.resume(result)
                    }

                    override fun onError(e: Exception) {
                        it.resumeWithException(e)
                    }
                }
            )
        }
    }

    @Test
    fun `test us_bank_account payment intent flow with descriptor code`() = runTest {
        val stripe = Stripe(context, settings.publishableKey)

        // Create a PaymentIntent on the backend
        val newPaymentIntent = service.createPaymentIntent(
            Request.CreatePaymentIntentParams(
                createParams = Request.CreateParams(
                    paymentMethodTypes = listOf("us_bank_account")
                )
            )
        )

        // Confirm the PaymentIntent with customers details collected from merchant app
        val confirmedPaymentIntent = requireNotNull(
            stripe.confirmPaymentIntentSynchronous(
                ConfirmPaymentIntentParams
                    .createWithPaymentMethodCreateParams(
                        paymentMethodCreateParams = US_BANK_ACCOUNT_PAYMENT_METHOD_CREATE_PARAMS,
                        clientSecret = newPaymentIntent.secret,
                        paymentMethodOptions = PaymentMethodOptionsParams.USBankAccount(
                            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                        ),
                        mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
                    )
            )
        )

        assertThat(confirmedPaymentIntent.amount)
            .isEqualTo(100)
        assertThat(confirmedPaymentIntent.currency)
            .isEqualTo("usd")
        assertThat(requireNotNull(confirmedPaymentIntent.status))
            .isEqualTo(StripeIntent.Status.RequiresAction)
        assertThat(requireNotNull(confirmedPaymentIntent.nextActionData))
            .isInstanceOf(StripeIntent.NextActionData.VerifyWithMicrodeposits::class.java)
        assertThat(requireNotNull(confirmedPaymentIntent.nextActionType))
            .isEqualTo(StripeIntent.NextActionType.VerifyWithMicrodeposits)

        // Verify the bank account
        val verifiedPaymentIntent = suspendCoroutine<PaymentIntent> {
            stripe.verifyPaymentIntentWithMicrodeposits(
                clientSecret = confirmedPaymentIntent.clientSecret!!,
                descriptorCode = "SM11AA",
                callback = object : ApiResultCallback<PaymentIntent> {
                    override fun onSuccess(result: PaymentIntent) {
                        it.resume(result)
                    }

                    override fun onError(e: Exception) {
                        it.resumeWithException(e)
                    }
                }
            )
        }

        assertThat(verifiedPaymentIntent.status)
            .isEqualTo(StripeIntent.Status.Processing)
    }

    @Test
    fun `test us_bank_account setup intent flow with amounts`() = runTest {
        val stripe = Stripe(context, settings.publishableKey)

        // Create a SetupIntent on the backend
        val newPaymentIntent = service.createSetupIntent(
            Request.CreateSetupIntentParams(
                createParams = Request.CreateParams(
                    paymentMethodTypes = listOf("us_bank_account")
                )
            )
        )

        // Confirm the SetupIntent with customers details collected from merchant app
        val confirmedSetupIntent = requireNotNull(
            stripe.confirmSetupIntentSynchronous(
                ConfirmSetupIntentParams
                    .create(
                        paymentMethodCreateParams = US_BANK_ACCOUNT_PAYMENT_METHOD_CREATE_PARAMS,
                        clientSecret = newPaymentIntent.secret,
                        mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
                    )
            )
        )

        assertThat(requireNotNull(confirmedSetupIntent.status))
            .isEqualTo(StripeIntent.Status.RequiresAction)
        assertThat(requireNotNull(confirmedSetupIntent.nextActionData))
            .isInstanceOf(StripeIntent.NextActionData.VerifyWithMicrodeposits::class.java)
        assertThat(requireNotNull(confirmedSetupIntent.nextActionType))
            .isEqualTo(StripeIntent.NextActionType.VerifyWithMicrodeposits)

        // Verify the bank account
        val verifiedSetupIntent = suspendCoroutine<SetupIntent> {
            stripe.verifySetupIntentWithMicrodeposits(
                clientSecret = confirmedSetupIntent.clientSecret!!,
                firstAmount = 32,
                secondAmount = 45,
                callback = object : ApiResultCallback<SetupIntent> {
                    override fun onSuccess(result: SetupIntent) {
                        it.resume(result)
                    }

                    override fun onError(e: Exception) {
                        it.resumeWithException(e)
                    }
                }
            )
        }
        assertThat(verifiedSetupIntent.status)
            .isEqualTo(StripeIntent.Status.Succeeded)
    }

    @Test(expected = InvalidRequestException::class)
    fun `test us_bank_account setup intent flow with amounts fails`() = runTest {
        val stripe = Stripe(context, settings.publishableKey)

        // Create a SetupIntent on the backend
        val newPaymentIntent = service.createSetupIntent(
            Request.CreateSetupIntentParams(
                createParams = Request.CreateParams(
                    paymentMethodTypes = listOf("us_bank_account")
                )
            )
        )

        // Confirm the SetupIntent with customers details collected from merchant app
        val confirmedSetupIntent = requireNotNull(
            stripe.confirmSetupIntentSynchronous(
                ConfirmSetupIntentParams
                    .create(
                        paymentMethodCreateParams = US_BANK_ACCOUNT_PAYMENT_METHOD_CREATE_PARAMS,
                        clientSecret = newPaymentIntent.secret,
                        mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
                    )
            )
        )

        // Verify the bank account
        suspendCoroutine<SetupIntent> {
            stripe.verifySetupIntentWithMicrodeposits(
                clientSecret = confirmedSetupIntent.clientSecret!!,
                firstAmount = 10,
                secondAmount = 11,
                callback = object : ApiResultCallback<SetupIntent> {
                    override fun onSuccess(result: SetupIntent) {
                        it.resume(result)
                    }

                    override fun onError(e: Exception) {
                        it.resumeWithException(e)
                    }
                }
            )
        }
    }

    @Test
    fun `test cashapp payment intent flow`() = runTest {
        val stripe = Stripe(context, settings.publishableKey)

        val paymentIntent = service.createPaymentIntent(
            Request.CreatePaymentIntentParams(
                createParams = Request.CreateParams(paymentMethodTypes = listOf("cashapp")),
            )
        )

        val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(),
            clientSecret = paymentIntent.secret,
        ).copy(
            returnUrl = "myapp://success",
        )

        val confirmedPaymentIntent = stripe.confirmPaymentIntent(confirmParams)

        assertThat(confirmedPaymentIntent.status).isEqualTo(StripeIntent.Status.RequiresAction)

        assertThat(confirmedPaymentIntent.nextActionType)
            .isEqualTo(StripeIntent.NextActionType.CashAppRedirect)
    }

    @Test
    fun `test cashapp payment intent flow with setup future usage`() = runTest {
        val stripe = Stripe(context, settings.publishableKey)

        val paymentIntent = service.createPaymentIntent(
            Request.CreatePaymentIntentParams(
                createParams = Request.CreateParams(paymentMethodTypes = listOf("cashapp")),
            )
        )

        val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(),
            clientSecret = paymentIntent.secret,
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
        ).copy(
            returnUrl = "myapp://success",
            mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT),
        )

        val confirmedPaymentIntent = stripe.confirmPaymentIntent(confirmParams)

        assertThat(confirmedPaymentIntent.status).isEqualTo(StripeIntent.Status.RequiresAction)

        assertThat(confirmedPaymentIntent.nextActionType)
            .isEqualTo(StripeIntent.NextActionType.CashAppRedirect)
    }

    @Test
    fun `test cashapp setup intent flow`() = runTest {
        val stripe = Stripe(context, settings.publishableKey)

        val paymentIntent = service.createSetupIntent(
            Request.CreateSetupIntentParams(
                createParams = Request.CreateParams(
                    paymentMethodTypes = listOf("cashapp"),
                ),
            )
        )

        val confirmParams = ConfirmSetupIntentParams.create(
            paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(),
            clientSecret = paymentIntent.secret,
        ).copy(
            returnUrl = "myapp://success",
            mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT),
        )

        val confirmedSetupIntent = stripe.confirmSetupIntent(confirmParams)

        assertThat(confirmedSetupIntent.status).isEqualTo(StripeIntent.Status.RequiresAction)

        assertThat(confirmedSetupIntent.nextActionType)
            .isEqualTo(StripeIntent.NextActionType.CashAppRedirect)
    }

    @Test
    fun `test us_bank_account setup intent flow with descriptor code`() = runTest {
        val stripe = Stripe(context, settings.publishableKey)

        // Create a SetupIntent on the backend
        val newPaymentIntent = service.createSetupIntent(
            Request.CreateSetupIntentParams(
                createParams = Request.CreateParams(
                    paymentMethodTypes = listOf("us_bank_account")
                )
            )
        )

        // Confirm the SetupIntent with customers details collected from merchant app
        val confirmedSetupIntent = requireNotNull(
            stripe.confirmSetupIntentSynchronous(
                ConfirmSetupIntentParams
                    .create(
                        paymentMethodCreateParams = US_BANK_ACCOUNT_PAYMENT_METHOD_CREATE_PARAMS,
                        clientSecret = newPaymentIntent.secret,
                        mandateData = MandateDataParams(MandateDataParams.Type.Online.DEFAULT)
                    )
            )
        )

        assertThat(requireNotNull(confirmedSetupIntent.status))
            .isEqualTo(StripeIntent.Status.RequiresAction)
        assertThat(requireNotNull(confirmedSetupIntent.nextActionData))
            .isInstanceOf(StripeIntent.NextActionData.VerifyWithMicrodeposits::class.java)
        assertThat(requireNotNull(confirmedSetupIntent.nextActionType))
            .isEqualTo(StripeIntent.NextActionType.VerifyWithMicrodeposits)

        // Verify the bank account
        val verifiedSetupIntent = suspendCoroutine<SetupIntent> {
            stripe.verifySetupIntentWithMicrodeposits(
                clientSecret = confirmedSetupIntent.clientSecret!!,
                descriptorCode = "SM11AA",
                callback = object : ApiResultCallback<SetupIntent> {
                    override fun onSuccess(result: SetupIntent) {
                        it.resume(result)
                    }

                    override fun onError(e: Exception) {
                        it.resumeWithException(e)
                    }
                }
            )
        }
        assertThat(verifiedSetupIntent.status)
            .isEqualTo(StripeIntent.Status.Succeeded)
    }

    @Test
    fun `Test update payment method`() = runTest {
        val stripe = Stripe(context, FrenchPublishableKey)

        val card = PaymentMethodCreateParams.Card.Builder()
            .setNumber("4000002500001001")
            .setExpiryMonth(1)
            .setExpiryYear(2028)
            .setCvc("123")
            .build()

        val paymentMethod = stripe.createPaymentMethod(
            paymentMethodCreateParams = PaymentMethodCreateParams.create(card),
        )

        val createEphemeralKeyResponse = service.createEphemeralKey(
            request = Request.CreateEphemeralKeyParams(account = "fr"),
        )

        val attachResult = stripe.attachPaymentMethod(
            paymentMethodId = paymentMethod.id!!,
            customerId = createEphemeralKeyResponse.customerId,
            ephemeralKeySecret = createEphemeralKeyResponse.ephemeralKeySecret,
        )
        assertThat(attachResult.isSuccess).isTrue()

        val updateParams = PaymentMethodUpdateParams.createCard(
            expiryMonth = 2,
            networks = PaymentMethodUpdateParams.Card.Networks(
                preferred = "visa",
            ),
        )

        val updatedPaymentMethod = stripe.updatePaymentMethod(
            paymentMethodId = paymentMethod.id!!,
            paymentMethodUpdateParams = updateParams,
            ephemeralKeySecret = createEphemeralKeyResponse.ephemeralKeySecret,
        )

        assertThat(updatedPaymentMethod.card?.expiryMonth).isEqualTo(2)
        assertThat(updatedPaymentMethod.card?.networks?.preferred).isEqualTo("visa")
    }

    private companion object {
        val CARD_PAYMENT_METHOD_CREATE_PARAMS = PaymentMethodCreateParams.createCard(
            CardParams(
                number = "4242424242424242",
                expMonth = 1,
                expYear = 2045,
                cvc = "123"
            )
        )

        val US_BANK_ACCOUNT_PAYMENT_METHOD_CREATE_PARAMS = PaymentMethodCreateParams.create(
            usBankAccount = PaymentMethodCreateParams.USBankAccount(
                accountNumber = "000123456789",
                routingNumber = "110000000",
                accountType = PaymentMethod.USBankAccount.USBankAccountType.CHECKING,
                accountHolderType = PaymentMethod.USBankAccount.USBankAccountHolderType.INDIVIDUAL
            ),
            billingDetails = PaymentMethod.BillingDetails(
                name = "Jane Doe",
                email = "jane+test_email@doe.com"
            )
        )
    }
}
