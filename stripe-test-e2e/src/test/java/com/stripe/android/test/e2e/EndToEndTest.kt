package com.stripe.android.test.e2e

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.Stripe
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
internal class EndToEndTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val settings: Settings by lazy {
        Settings(context)
    }

    private val service = ServiceFactory().create(
        baseUrl = settings.backendUrl
    )

    /**
     * MARK: LOG.04.01c
     * In this test, a PaymentIntent object is created from an example merchant backend,
     * confirmed by the Android SDK, and then retrieved to validate that the original amount,
     * currency, and merchant are the same as the original inputs.
     */
    @Test
    fun testRigCon() = runBlocking {
        // Create a PaymentIntent on the backend
        val newPaymentIntent = service.createPaymentIntent()

        val stripe = Stripe(context, newPaymentIntent.publishableKey)

        // Confirm the PaymentIntent using a test card
        val confirmedPaymentIntent = requireNotNull(
            stripe.confirmPaymentIntentSynchronous(
                ConfirmPaymentIntentParams
                    .createWithPaymentMethodCreateParams(
                        clientSecret = newPaymentIntent.clientSecret,
                        paymentMethodCreateParams = PAYMENT_METHOD_CREATE_PARAMS
                    )
                    .withShouldUseStripeSdk(true)
            )
        )

        val expectedPaymentIntentData = service.fetchPaymentIntent(
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

    private companion object {
        val PAYMENT_METHOD_CREATE_PARAMS = PaymentMethodCreateParams.createCard(
            CardParams(
                number = "4242424242424242",
                expMonth = 1,
                expYear = 2025,
                cvc = "123"
            )
        )
    }
}
