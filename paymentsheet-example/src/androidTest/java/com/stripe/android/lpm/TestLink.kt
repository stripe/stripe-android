package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.paymentsheet.example.playground.settings.LinkSettingsDefinition
import com.stripe.android.test.core.TestParameters
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestLink : BasePlaygroundTest() {
    private val linkNewUser = TestParameters.create(
        paymentMethodCode = "card",
    ) { settings ->
        settings[LinkSettingsDefinition] = true
    }

    @Test
    @Ignore("neutral-culminate")
    fun testLinkInlineCustom() {
        testDriver.testLinkCustom(linkNewUser)
    }
}
