package com.stripe.android.link.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.model.LinkBrand
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LinkAppBarMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun logout_label_uses_link_brand_name() {
        setContent(LinkBrand.Link)

        composeRule.onNodeWithTag(LOGOUT_MENU_ROW_TAG).assertTextEquals("Log out of Link")
    }

    @Test
    fun logout_label_uses_notlink_brand_name() {
        setContent(LinkBrand.Onelink)

        composeRule.onNodeWithTag(LOGOUT_MENU_ROW_TAG).assertTextEquals("Log out of Notlink")
    }

    private fun setContent(linkBrand: LinkBrand) {
        composeRule.setContent {
            DefaultLinkTheme {
                LinkAppBarMenu(
                    linkBrand = linkBrand,
                    onLogoutClicked = {},
                )
            }
        }
    }
}
