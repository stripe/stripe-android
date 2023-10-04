package com.stripe.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Browser
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestAuthorization : BasePlaygroundTest() {

    private val testParameters = TestParameters.create(
        paymentMethodCode = "bancontact",
    ).copy(
        saveCheckboxValue = false,
        saveForFutureUseCheckboxVisible = false,
        useBrowser = Browser.Chrome,
        authorizationAction = AuthorizeAction.AuthorizePayment,
    )

    @Test
    fun testAuthorizeSuccess() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.AuthorizePayment,
            )
        )
    }

    @Test
    fun testAuthorizeFailure() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "We are unable to authenticate your payment method. " +
                        "Please choose a different payment method and try again.",
                ),
            )
        )
    }

    @Test
    fun testAuthorizeCancel() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copy(
                authorizationAction = AuthorizeAction.Cancel,
            )
        )
    }
}
