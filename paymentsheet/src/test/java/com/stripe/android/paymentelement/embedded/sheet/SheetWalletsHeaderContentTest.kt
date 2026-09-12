package com.stripe.android.paymentelement.embedded.sheet

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SheetWalletsHeaderContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext<Context>(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        )
    }

    @Test
    fun `renders Google Pay and Link`() {
        composeRule.setContent {
            SheetWalletsHeaderContent(
                state = createWalletsState(),
                dividerSpacing = 16.dp,
            )
        }

        composeRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(LinkButtonTestTag).assertExists()
    }

    private fun createWalletsState(): WalletsState {
        return WalletsState(
            link = WalletsState.Link(
                state = LinkButtonState.Default,
                linkBrand = LinkBrand.Link,
            ),
            googlePay = WalletsState.GooglePay(
                buttonType = GooglePayButtonType.Pay,
                allowCreditCards = false,
                billingAddressParameters = null,
                additionalEnabledNetworks = emptyList(),
            ),
            walletsAllowedInHeader = WalletType.entries,
            buttonsEnabled = true,
            dividerTextResource = R.string.stripe_paymentsheet_or_pay_using,
            cardFundingFilter = DefaultCardFundingFilter,
            cardBrandFilter = DefaultCardBrandFilter,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
    }
}
