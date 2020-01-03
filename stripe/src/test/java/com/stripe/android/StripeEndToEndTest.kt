package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.AccountParams
import com.stripe.android.model.AddressFixtures
import com.stripe.android.model.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StripeEndToEndTest {

    private val stripe: Stripe by lazy {
        Stripe(ApplicationProvider.getApplicationContext(), ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
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
}
