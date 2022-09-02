package com.stripe.android.link.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.R
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
            isRootScreen = true,
            currentRoute = LinkScreen.Wallet.route,
            email = " "
        )

        assertThat(state.email).isNull()
    }

    @Test
    fun loadingScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.Loading.route,
            email = null
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.ic_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = null
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun verificationScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.Verification.route,
            email = "someone@stripe.com"
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.ic_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = null
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun verificationDialogShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.VerificationDialog.route,
            email = "someone@stripe.com"
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.ic_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = "someone@stripe.com"
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun walletScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.Wallet.route,
            email = "someone@stripe.com"
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.ic_link_close,
            showHeader = true,
            showOverflowMenu = true,
            email = "someone@stripe.com"
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun paymentMethodScreenShowsCorrectAppBarStateWhenAddingFirstPaymentMethod() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.PaymentMethod.route,
            email = "someone@stripe.com"
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.ic_link_close,
            showHeader = true,
            showOverflowMenu = true,
            email = "someone@stripe.com"
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun paymentMethodScreenShowsCorrectAppBarStateWhenThereAreExistingPaymentMethods() {
        val state = buildLinkAppBarState(
            isRootScreen = false,
            currentRoute = LinkScreen.PaymentMethod.route,
            email = "someone@stripe.com"
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.ic_link_back,
            showHeader = false,
            showOverflowMenu = false,
            email = null
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun cardEditScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = false,
            currentRoute = LinkScreen.CardEdit.route,
            email = "someone@stripe.com"
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.ic_link_back,
            showHeader = false,
            showOverflowMenu = false,
            email = null
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun signupScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.SignUp.route,
            email = null
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.ic_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = null
        )

        assertThat(state).isEqualTo(expected)
    }

    private fun buildLinkAppBarState(
        isRootScreen: Boolean,
        currentRoute: String?,
        email: String?
    ): LinkAppBarState {
        var state: LinkAppBarState? = null

        composeTestRule.setContent {
            state = rememberLinkAppBarState(isRootScreen, currentRoute, email)
        }

        return state ?: throw AssertionError(
            "buildLinkAppBarState should not produce null result"
        )
    }
}
