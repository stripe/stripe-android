package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.AccountParams
import com.stripe.android.model.AddressFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeEndToEndTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val stripe: Stripe by lazy {
        Stripe(context, ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
    }

    @Test
    fun testCreateAccountToken() {
        val token = stripe.createAccountTokenSynchronous(
            accountParams = AccountParams.create(
                tosShownAndAccepted = true,
                individual = AccountParams.BusinessTypeParams.Individual(
                    firstName = "Jenny",
                    lastName = "Rosen",
                    address = AddressFixtures.ADDRESS
                )
            )
        )
        assertEquals(Token.TokenType.ACCOUNT, token?.type)
    }

    @Test
    fun createPaymentMethodSynchronous_withAuBecsDebit() {
        val paymentMethod =
            Stripe(context, ApiKeyFixtures.AU_BECS_DEBIT_PUBLISHABLE_KEY)
                .createPaymentMethodSynchronous(
                    PaymentMethodCreateParamsFixtures.AU_BECS_DEBIT
                )
        requireNotNull(paymentMethod)
        assertThat(paymentMethod.type)
            .isEqualTo(PaymentMethod.Type.AuBecsDebit)
    }
}
