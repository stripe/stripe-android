package com.stripe.android.lpm

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.GooglePayState
import com.stripe.android.test.core.IntentType
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestSepaDebit : BaseLpmTest() {

    private val sepaDebit = newUser.copy(
        paymentMethod = lpmRepository.fromCode("sepa_debit")!!,
        merchantCountryCode = "FR",
        delayed = DelayedPMs.On,
        authorizationAction = null,
        googlePayState = GooglePayState.Off,
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
    fun testSepaDebitSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = sepaDebit.copy(
                automatic = Automatic.On,
                intentType = IntentType.PayWithSetup,
            ),
        ) {
            rules.compose.onNodeWithText("IBAN").apply {
                performTextInput(
                    "DE89370400440532013000"
                )
            }
        }
    }

    @Test
    fun testSepaDebitSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = sepaDebit.copy(
                automatic = Automatic.On,
                intentType = IntentType.Setup,
            ),
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
