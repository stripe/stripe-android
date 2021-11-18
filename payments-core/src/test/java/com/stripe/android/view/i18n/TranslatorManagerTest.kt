package com.stripe.android.view.i18n

import com.stripe.android.StripeErrorFixtures
import com.stripe.android.core.StripeError
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TranslatorManagerTest {

    @BeforeTest
    fun setup() {
        TranslatorManager.setErrorMessageTranslator(null)
    }

    @Test
    fun testDefaultErrorMessageTranslator() {
        assertEquals(
            "error!",
            TranslatorManager.getErrorMessageTranslator()
                .translate(0, "error!", STRIPE_ERROR)
        )
    }

    @Test
    fun testCustomErrorMessageTranslator() {
        TranslatorManager.setErrorMessageTranslator(
            object : ErrorMessageTranslator {
                override fun translate(
                    httpCode: Int,
                    errorMessage: String?,
                    stripeError: StripeError?
                ): String {
                    return "custom message"
                }
            }
        )
        assertEquals(
            "custom message",
            TranslatorManager.getErrorMessageTranslator()
                .translate(0, "original message", STRIPE_ERROR)
        )
    }

    private companion object {
        private val STRIPE_ERROR = StripeErrorFixtures.INVALID_REQUEST_ERROR
    }
}
