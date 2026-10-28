package com.stripe.android.test.core.ui

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT

internal class GooglePayPage(private val device: UiDevice) {
    fun selectTestCard(last4: String) {
        val paymentMethodSelectorArrow = device.findObject(
            UiSelector()
                .className("android.widget.ImageView")
                .descriptionContains("Show list of payment methods")
        )
        check(paymentMethodSelectorArrow.waitForExists(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)) {
            "Google Pay payment method selector not found"
        }
        paymentMethodSelectorArrow.click()

        val targetCardSelector = UiSelector()
            .className("android.widget.TextView")
            .textContains(last4)
        val cardList = object : UiScrollable(UiSelector().className("android.widget.ScrollView")) {
            override fun scrollForward(): Boolean = scrollForward(SCROLL_STEPS)

            override fun scrollToBeginning(maxSwipes: Int): Boolean =
                scrollToBeginning(maxSwipes, SCROLL_STEPS)
        }
        check(cardList.scrollIntoView(targetCardSelector)) { "Google Pay card ending in $last4 not found" }
        device.findObject(targetCardSelector).click()
    }

    private companion object {
        const val SCROLL_STEPS = 20
    }
}
