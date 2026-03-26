package com.stripe.paymentelementtestpages

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick

class CurrencySelector(
    private val composeTestRule: ComposeTestRule
) {
    fun assertCurrencyOptionIsSelected(currencyCode: String) {
        composeTestRule.onNode(
            hasTestTag("TEST_TAG_CURRENCY_OPTION_$currencyCode").and(isSelected())
        ).assertExists()
    }

    fun clickCurrencyOption(currencyCode: String) {
        val currencyOption = composeTestRule
            .onNodeWithTag("TEST_TAG_CURRENCY_OPTION_$currencyCode")

        composeTestRule.waitUntil { currencyOption.isDisplayed() }
        currencyOption.performClick()
    }
}
