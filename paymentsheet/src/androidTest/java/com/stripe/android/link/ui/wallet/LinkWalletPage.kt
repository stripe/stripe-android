package com.stripe.android.link.ui.wallet

import android.app.Application
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.link.ui.LOGOUT_MENU_ROW_TAG
import com.stripe.android.paymentsheet.R

internal class LinkWalletPage(
    private val composeTestRule: ComposeTestRule,
) {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    fun waitUntilVisible() {
        val showMenuDescription = applicationContext.getString(R.string.stripe_show_menu)
        composeTestRule.waitUntil(
            conditionDescription = "Link wallet is visible",
            timeoutMillis = 5_000,
        ) {
            composeTestRule.onAllNodes(hasContentDescription(showMenuDescription))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    fun logOut() {
        waitUntilVisible()

        val showMenuDescription = applicationContext.getString(R.string.stripe_show_menu)
        composeTestRule.onNodeWithContentDescription(showMenuDescription).performClick()
        composeTestRule.onNodeWithTag(LOGOUT_MENU_ROW_TAG).performClick()
    }
}
