package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.FieldPopulator
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class Test3ds2 : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "card",
        authorizationAction = null,
    ).copy(
        saveForFutureUseCheckboxVisible = true,
    )

    @Test
    fun testCardWith3ds2() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Authorize3ds2,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000000000003220")
        )
    }

    @Test
    fun test3DS2HSBCHTML() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Test3DS2.HSBCHTML,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000582600000292"),
        )
    }

    @Test
    fun test3DS2OTP() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Test3DS2.OTP,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000582600000045"),
        )
    }

    @Test
    fun test3DS2OOB() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Test3DS2.OOB,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000582600000094"),
        )
    }

    @Test
    fun test3DS2SingleSelect() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Test3DS2.SingleSelect,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000582600000102"),
        )
    }

    @Test
    fun test3DS2MultiSelect() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Test3DS2.MultiSelect,
            ),
            populateCustomLpmFields = {
                populateCardDetails()
            },
            values = FieldPopulator.Values(cardNumber = "4000582600000110"),
        )
    }
}
