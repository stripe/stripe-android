package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.GooglePayState
import com.stripe.android.test.core.IntentType
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestSofort : BaseLpmTest() {
    private val sofort = newUser.copy(
        paymentMethod = lpmRepository.fromCode("sofort")!!,
        currency = Currency.EUR,
        merchantCountryCode = "FR",
        delayed = DelayedPMs.On,
        googlePayState = GooglePayState.Off,
    )

    @Test
    fun testSofort() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = sofort,
        )
    }

    @Test
    fun testSofortSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = sofort.copy(
                delayed = DelayedPMs.On,
                automatic = Automatic.On,
                intentType = IntentType.PayWithSetup,
            ),
        )
    }

    @Test
    fun testSofortSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = sofort.copy(
                delayed = DelayedPMs.On,
                automatic = Automatic.On,
                intentType = IntentType.Setup,
                authorizationAction = AuthorizeAction.AuthorizeSetup,
            ),
        )
    }

    @Test
    fun testSofortInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = sofort,
        )
    }
}
