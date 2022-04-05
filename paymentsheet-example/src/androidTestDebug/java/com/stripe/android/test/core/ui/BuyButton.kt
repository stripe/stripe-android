package com.stripe.android.test.core.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.stripe.android.paymentsheet.example.R

class BuyButton(private val device: UiDevice) : EspressoIdButton(R.id.buy_button) {
    fun waitProcessingComplete() {
        device.wait(
            Until.findObject(
                By.textContains(
                    InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(
                        R.string.stripe_pay_button_amount
                    ).replace("%s", "")
                )
            ),
            InstrumentationRegistry.getInstrumentation().targetContext.resources
                .getInteger(android.R.integer.config_shortAnimTime)
                .toLong()
        )
    }
}
