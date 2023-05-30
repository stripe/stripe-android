package com.stripe.android.link.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.R
import com.stripe.android.link.model.AccountStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class LinkAppBarStateTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rejectsBlankEmailAddress() {
        val state = buildLinkAppBarState(
            currentRoute = LinkScreen.Wallet.route,
            email = " ",
            accountStatus = AccountStatus.SignedOut
        )

        assertThat(state.email).isNull()
    }

    @Test
    fun verificationDialogShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            currentRoute = LinkScreen.VerificationDialog.route,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.VerificationStarted
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showOverflowMenu = false,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.VerificationStarted
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun walletScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            currentRoute = LinkScreen.Wallet.route,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.Verified
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showOverflowMenu = true,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.Verified
        )

        assertThat(state).isEqualTo(expected)
    }

    private fun buildLinkAppBarState(
        currentRoute: String?,
        email: String?,
        accountStatus: AccountStatus?
    ): LinkAppBarState {
        var state: LinkAppBarState? = null

        composeTestRule.setContent {
            state = rememberLinkAppBarState(true, currentRoute, email, accountStatus)
        }

        return state ?: throw AssertionError(
            "buildLinkAppBarState should not produce null result"
        )
    }
}
