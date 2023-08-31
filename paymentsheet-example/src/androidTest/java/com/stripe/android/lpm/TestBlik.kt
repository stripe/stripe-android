package com.stripe.android.lpm

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Currency
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestBlik : BaseLpmTest() {
    private val blik = newUser.copy(
        paymentMethod = lpmRepository.fromCode("blik")!!,
        currency = Currency.PLN,
        merchantCountryCode = "FR",
        authorizationAction = AuthorizeAction.PollingSucceedsAfterDelay,
        supportedPaymentMethods = listOf("card", "blik"),
    )

    @Test
    fun testBlik() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = blik,
            populateCustomLpmFields = {
                rules.compose.onNodeWithText("BLIK code").apply {
                    performTextInput(
                        "123456"
                    )
                }
            },
        )
    }
}
