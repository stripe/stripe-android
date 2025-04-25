package com.stripe.android.test.core

import androidx.test.espresso.web.sugar.Web.WebInteraction
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.Locator
import java.util.concurrent.TimeUnit

/**
 * Find a button in a web view using its `data-testid` ID (commonly used in Stripe frontend surfaces)
 */
internal fun WebInteraction<Void>.withElementByTestId(
    testId: String
): WebInteraction<Void?> {
    return withTimeout(10, TimeUnit.SECONDS)
        .withElement(findElement(Locator.CSS_SELECTOR, "[data-testid='$testId']"))
}

/**
 * Find a button in a web view by text
 */
internal fun WebInteraction<Void>.withElementByText(
    text: String
): WebInteraction<Void?> {
    return withTimeout(10, TimeUnit.SECONDS)
        .withElement(findElement(Locator.XPATH, "//*[contains(text(), '$text')]"))
}