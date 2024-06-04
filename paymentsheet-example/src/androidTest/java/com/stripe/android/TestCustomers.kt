package com.stripe.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.test.core.Browser
import com.stripe.android.test.core.TestParameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCustomers : BasePlaygroundTest() {
    private val testParameters = TestParameters.create(
        paymentMethodCode = "bancontact",
    ).copy(
        useBrowser = Browser.Chrome,
    )

    @Test
    fun testAuthorizeGuest() {
        testDriver.confirmNewOrGuestComplete(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.GUEST
            }
        )
    }

    @Test
    fun testAuthorizeNew() {
        testDriver.confirmNewOrGuestComplete(
            testParameters
        )
    }
}
