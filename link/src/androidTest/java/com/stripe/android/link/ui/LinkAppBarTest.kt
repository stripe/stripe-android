package com.stripe.android.link.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.R
import com.stripe.android.link.theme.DefaultLinkTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class LinkAppBarTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun email_is_shown_when_provided() {
        val email = "test@stripe.com"
        setContent(email)

        composeTestRule.onNodeWithText(email).assertExists()
    }

    @Test
    fun on_back_button_click_callback_is_called() {
        var count = 0
        setContent(onBackPress = { count++ })

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun on_overflow_button_click_callback_is_called() {
        var count = 0
        setContent(showBottomSheetContent = { count++ })

        composeTestRule.onNodeWithContentDescription("Menu").performClick()

        assertThat(count).isEqualTo(1)
    }

    private fun setContent(
        email: String? = null,
        onBackPress: () -> Unit = {},
        onLogout: () -> Unit = {},
        showBottomSheetContent: (BottomSheetContent?) -> Unit = {}
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            LinkAppBar(
                state = LinkAppBarState(
                    navigationIcon = R.drawable.ic_link_close,
                    showHeader = true,
                    showOverflowMenu = true,
                    email = email
                ),
                onBackPress = onBackPress,
                onLogout = onLogout,
                showBottomSheetContent = showBottomSheetContent
            )
        }
    }
}
