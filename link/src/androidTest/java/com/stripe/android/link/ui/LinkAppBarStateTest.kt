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
            isRootScreen = true,
            currentRoute = LinkScreen.Wallet.route,
            email = " ",
            accountStatus = AccountStatus.SignedOut
        )

        assertThat(state.email).isNull()
    }

    @Test
    fun loadingScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.Loading.route,
            email = null,
            accountStatus = AccountStatus.SignedOut
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = null,
            accountStatus = AccountStatus.SignedOut
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun verificationScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.Verification.route,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.VerificationStarted
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = null,
            accountStatus = AccountStatus.VerificationStarted
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun verificationDialogShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.VerificationDialog.route,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.VerificationStarted
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.VerificationStarted
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun walletScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.Wallet.route,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.Verified
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showHeader = true,
            showOverflowMenu = true,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.Verified
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun paymentMethodScreenShowsCorrectAppBarStateWhenAddingFirstPaymentMethod() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.PaymentMethod.route,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.Verified
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showHeader = true,
            showOverflowMenu = true,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.Verified
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun paymentMethodScreenShowsCorrectAppBarStateWhenThereAreExistingPaymentMethods() {
        val state = buildLinkAppBarState(
            isRootScreen = false,
            currentRoute = LinkScreen.PaymentMethod.route,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.Verified
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_back,
            showHeader = false,
            showOverflowMenu = false,
            email = null,
            accountStatus = AccountStatus.Verified
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun cardEditScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = false,
            currentRoute = LinkScreen.CardEdit.route,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.Verified
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_back,
            showHeader = false,
            showOverflowMenu = false,
            email = null,
            accountStatus = AccountStatus.Verified
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun signupScreenShowsCorrectAppBarState() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.SignUp.route,
            email = null,
            accountStatus = AccountStatus.SignedOut
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = null,
            accountStatus = AccountStatus.SignedOut
        )

        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun signupScreenShowsCorrectAppBarStateWithEmail() {
        val state = buildLinkAppBarState(
            isRootScreen = true,
            currentRoute = LinkScreen.SignUp.route,
            email = "someone@stripe.com",
            accountStatus = AccountStatus.NeedsVerification
        )

        val expected = LinkAppBarState(
            navigationIcon = R.drawable.stripe_link_close,
            showHeader = true,
            showOverflowMenu = false,
            email = null,
            accountStatus = AccountStatus.NeedsVerification
        )

        assertThat(state).isEqualTo(expected)
    }

    private fun buildLinkAppBarState(
        isRootScreen: Boolean,
        currentRoute: String?,
        email: String?,
        accountStatus: AccountStatus?
    ): LinkAppBarState {
        var state: LinkAppBarState? = null

        composeTestRule.setContent {
            state = rememberLinkAppBarState(isRootScreen, currentRoute, email, accountStatus)
        }

        return state ?: throw AssertionError(
            "buildLinkAppBarState should not produce null result"
        )
    }
}
