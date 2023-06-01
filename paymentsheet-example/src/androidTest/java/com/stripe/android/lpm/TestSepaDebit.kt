package com.stripe.android.lpm

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.DelayedPMs
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestSepaDebit : BaseLpmTest() {

    private val sepaDebit = newUser.copy(
        paymentMethod = lpmRepository.fromCode("sepa_debit")!!,
        delayed = DelayedPMs.On,
        authorizationAction = null
    )

    @Test
    fun testSepaDebit() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = sepaDebit,
        ) {
            rules.compose.onNodeWithText("IBAN").apply {
                performTextInput(
                    "DE89370400440532013000"
                )
            }
        }
    }

    @Test
    fun testSepaDebitInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = sepaDebit,
            populateCustomLpmFields = {
                rules.compose.onNodeWithText("IBAN").apply {
                    performTextInput(
                        "DE89370400440532013000"
                    )
                }
            },
            verifyCustomLpmFields = {
                rules.compose.onNodeWithText("IBAN").apply {
                    assertContentDescriptionEquals(
                        "DE89370400440532013000"
                    )
                }
            }
        )
    }
}
