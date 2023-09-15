package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.LinkState
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestLink : BaseLpmTest() {
    private val linkNewUser = newUser.copy(
        paymentMethod = lpmRepository.fromCode("card")!!,
        linkState = LinkState.On,
    )

    @Test
    @Ignore("neutral-culminate")
    fun testLinkInlineCustom() {
        testDriver.testLinkCustom(linkNewUser)
    }
}
