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
    fun on_button_click_button_callback_is_called() {
        var count = 0
        setContent(onCloseButtonClick = { count++ })

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertThat(count).isEqualTo(1)
    }

    private fun setContent(
        email: String? = null,
        onCloseButtonClick: () -> Unit = {}
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            LinkAppBar(
                email = email,
                buttonIconResource = R.drawable.ic_link_close,
                onButtonClick = onCloseButtonClick
            )
        }
    }
}
