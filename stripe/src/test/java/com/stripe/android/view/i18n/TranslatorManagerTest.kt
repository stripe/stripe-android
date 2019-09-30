package com.stripe.android.view.i18n

import com.stripe.android.StripeErrorFixtures
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
        assertEquals("error!",
            TranslatorManager.getErrorMessageTranslator()
                .translate(0, "error!", STRIPE_ERROR))
    }

    @Test
    fun testCustomErrorMessageTranslator() {
        TranslatorManager.setErrorMessageTranslator { _, _, _ -> "custom message" }
        assertEquals("custom message", TranslatorManager.getErrorMessageTranslator()
            .translate(0, "original message", STRIPE_ERROR))
    }

    companion object {
        private val STRIPE_ERROR = StripeErrorFixtures.INVALID_REQUEST_ERROR
    }
}
