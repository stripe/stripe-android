package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import org.junit.Test

class WalletsStateTest {

    // region WalletsState.create tests

    @Test
    fun `create returns null when no wallets available`() {
        val state = createViaFactory(
            isLinkAvailable = false,
            isGooglePayReady = false,
            isShopPayAvailable = false,
        )

        assertThat(state).isNull()
    }

    @Test
    fun `create returns state with Link when isLinkAvailable is true`() {
        val state = createViaFactory(
            isLinkAvailable = true,
            linkEmail = "test@example.com",
        )

        assertThat(state).isNotNull()
        val link = state?.link(WalletLocation.INLINE)
        assertThat(link).isNotNull()
        assertThat(link?.state).isInstanceOf(LinkButtonState.Email::class.java)
        assertThat((link?.state as LinkButtonState.Email).email).isEqualTo("test@example.com")
    }

    @Test
    fun `create returns state with GooglePay when isGooglePayReady is true`() {
        val state = createViaFactory(isGooglePayReady = true)

        assertThat(state).isNotNull()
        val googlePay = state?.googlePay(WalletLocation.INLINE)
        assertThat(googlePay).isNotNull()
        assertThat(googlePay?.buttonType).isEqualTo(GooglePayButtonType.Pay)
    }

    @Test
    fun `create returns state with ShopPay when isShopPayAvailable is true`() {
        val state = createViaFactory(isShopPayAvailable = true)

        assertThat(state).isNotNull()
        val shopPay = state?.shopPay(WalletLocation.INLINE)
        assertThat(shopPay).isEqualTo(WalletsState.ShopPay)
    }

    @Test
    fun `create sets divider text to or_pay_with_card for card only payment intent`() {
        val state = createViaFactory(
            isLinkAvailable = true,
            paymentMethodTypes = listOf(PaymentMethod.Type.Card.code),
            isSetupIntent = false,
        )

        assertThat(state?.dividerTextResource).isEqualTo(R.string.stripe_paymentsheet_or_pay_with_card)
    }

    @Test
    fun `create sets divider text to or_pay_using for multiple payment methods payment intent`() {
        val state = createViaFactory(
            isLinkAvailable = true,
            paymentMethodTypes = listOf(PaymentMethod.Type.Card.code, PaymentMethod.Type.Bancontact.code),
            isSetupIntent = false,
        )

        assertThat(state?.dividerTextResource).isEqualTo(R.string.stripe_paymentsheet_or_pay_using)
    }

    @Test
    fun `create sets divider text to or_use_a_card for card only setup intent`() {
        val state = createViaFactory(
            isLinkAvailable = true,
            paymentMethodTypes = listOf(PaymentMethod.Type.Card.code),
            isSetupIntent = true,
        )

        assertThat(state?.dividerTextResource).isEqualTo(R.string.stripe_paymentsheet_or_use_a_card)
    }

    @Test
    fun `create sets divider text to or_use for multiple payment methods setup intent`() {
        val state = createViaFactory(
            isLinkAvailable = true,
            paymentMethodTypes = listOf(PaymentMethod.Type.Card.code, PaymentMethod.Type.Bancontact.code),
            isSetupIntent = true,
        )

        assertThat(state?.dividerTextResource).isEqualTo(R.string.stripe_paymentsheet_or_use)
    }

    @Test
    fun `create returns null for Link when isLinkAvailable is null`() {
        val state = createViaFactory(
            isLinkAvailable = null,
            isGooglePayReady = true,
        )

        assertThat(state).isNotNull()
        assertThat(state?.link(WalletLocation.INLINE)).isNull()
    }

    @Test
    fun `create passes buttonsEnabled correctly`() {
        val state = createViaFactory(
            isLinkAvailable = true,
            buttonsEnabled = false,
        )

        assertThat(state?.buttonsEnabled).isFalse()
    }

    private fun createViaFactory(
        isLinkAvailable: Boolean? = false,
        linkEmail: String? = null,
        isGooglePayReady: Boolean = false,
        isShopPayAvailable: Boolean = false,
        buttonsEnabled: Boolean = true,
        paymentMethodTypes: List<String> = listOf(PaymentMethod.Type.Card.code),
        isSetupIntent: Boolean = false,
    ): WalletsState? {
        return WalletsState.create(
            isLinkAvailable = isLinkAvailable,
            linkEmail = linkEmail,
            isGooglePayReady = isGooglePayReady,
            isShopPayAvailable = isShopPayAvailable,
            googlePayButtonType = GooglePayButtonType.Pay,
            buttonsEnabled = buttonsEnabled,
            paymentMethodTypes = paymentMethodTypes,
            googlePayLauncherConfig = null,
            onGooglePayPressed = {},
            onLinkPressed = {},
            onShopPayPressed = {},
            isSetupIntent = isSetupIntent,
            walletsAllowedInHeader = emptyList(),
            cardFundingFilter = DefaultCardFundingFilter,
            cardBrandFilter = DefaultCardBrandFilter,
        )
    }

    // endregion

    // region WalletsState.wallets tests

    @Test
    fun `wallets returns all wallets in header when all allowed in header`() {
        val state = createWalletsState(
            hasLink = true,
            hasGooglePay = true,
            hasShopPay = true,
            walletsAllowedInHeader = listOf(WalletType.Link, WalletType.GooglePay),
        )

        val wallets = state.wallets(WalletLocation.HEADER)

        assertThat(wallets).hasSize(2)
        assertThat(wallets.filterIsInstance<WalletsState.GooglePay>()).hasSize(1)
        assertThat(wallets.filterIsInstance<WalletsState.Link>()).hasSize(1)
    }

    @Test
    fun `wallets returns only ShopPay inline when Link and GooglePay allowed in header`() {
        val state = createWalletsState(
            hasLink = true,
            hasGooglePay = true,
            hasShopPay = true,
            walletsAllowedInHeader = listOf(WalletType.Link, WalletType.GooglePay),
        )

        val wallets = state.wallets(WalletLocation.INLINE)

        assertThat(wallets).hasSize(1)
        assertThat(wallets.first()).isEqualTo(WalletsState.ShopPay)
    }

    @Test
    fun `wallets returns all wallets inline when none allowed in header`() {
        val state = createWalletsState(
            hasLink = true,
            hasGooglePay = true,
            hasShopPay = true,
            walletsAllowedInHeader = emptyList(),
        )

        val wallets = state.wallets(WalletLocation.INLINE)

        assertThat(wallets).hasSize(3)
        assertThat(wallets.filterIsInstance<WalletsState.GooglePay>()).hasSize(1)
        assertThat(wallets.filterIsInstance<WalletsState.Link>()).hasSize(1)
        assertThat(wallets.filterIsInstance<WalletsState.ShopPay>()).hasSize(1)
    }

    @Test
    fun `wallets returns empty list in header when none allowed in header`() {
        val state = createWalletsState(
            hasLink = true,
            hasGooglePay = true,
            hasShopPay = true,
            walletsAllowedInHeader = emptyList(),
        )

        val wallets = state.wallets(WalletLocation.HEADER)

        assertThat(wallets).isEmpty()
    }

    @Test
    fun `wallets returns only Link in header when only Link allowed`() {
        val state = createWalletsState(
            hasLink = true,
            hasGooglePay = true,
            hasShopPay = true,
            walletsAllowedInHeader = listOf(WalletType.Link),
        )

        val wallets = state.wallets(WalletLocation.HEADER)

        assertThat(wallets).hasSize(1)
        assertThat(wallets.first()).isInstanceOf(WalletsState.Link::class.java)
    }

    @Test
    fun `wallets returns GooglePay and ShopPay inline when only Link allowed in header`() {
        val state = createWalletsState(
            hasLink = true,
            hasGooglePay = true,
            hasShopPay = true,
            walletsAllowedInHeader = listOf(WalletType.Link),
        )

        val wallets = state.wallets(WalletLocation.INLINE)

        assertThat(wallets).hasSize(2)
        assertThat(wallets.filterIsInstance<WalletsState.GooglePay>()).hasSize(1)
        assertThat(wallets.filterIsInstance<WalletsState.ShopPay>()).hasSize(1)
    }

    @Test
    fun `wallets returns only GooglePay in header when only GooglePay allowed`() {
        val state = createWalletsState(
            hasLink = true,
            hasGooglePay = true,
            hasShopPay = true,
            walletsAllowedInHeader = listOf(WalletType.GooglePay),
        )

        val wallets = state.wallets(WalletLocation.HEADER)

        assertThat(wallets).hasSize(1)
        assertThat(wallets.first()).isInstanceOf(WalletsState.GooglePay::class.java)
    }

    @Test
    fun `wallets returns Link and ShopPay inline when only GooglePay allowed in header`() {
        val state = createWalletsState(
            hasLink = true,
            hasGooglePay = true,
            hasShopPay = true,
            walletsAllowedInHeader = listOf(WalletType.GooglePay),
        )

        val wallets = state.wallets(WalletLocation.INLINE)

        assertThat(wallets).hasSize(2)
        assertThat(wallets.filterIsInstance<WalletsState.Link>()).hasSize(1)
        assertThat(wallets.filterIsInstance<WalletsState.ShopPay>()).hasSize(1)
    }

    @Test
    fun `wallets never returns ShopPay in header`() {
        val state = createWalletsState(
            hasLink = false,
            hasGooglePay = false,
            hasShopPay = true,
            walletsAllowedInHeader = WalletType.entries,
        )

        val wallets = state.wallets(WalletLocation.HEADER)

        assertThat(wallets).isEmpty()
    }

    @Test
    fun `wallets returns ShopPay inline even when all wallets allowed in header`() {
        val state = createWalletsState(
            hasLink = false,
            hasGooglePay = false,
            hasShopPay = true,
            walletsAllowedInHeader = WalletType.entries,
        )

        val wallets = state.wallets(WalletLocation.INLINE)

        assertThat(wallets).hasSize(1)
        assertThat(wallets.first()).isEqualTo(WalletsState.ShopPay)
    }

    @Test
    fun `wallets returns empty list when no wallets available`() {
        val state = createWalletsState(
            hasLink = false,
            hasGooglePay = false,
            hasShopPay = false,
            walletsAllowedInHeader = WalletType.entries,
        )

        assertThat(state.wallets(WalletLocation.HEADER)).isEmpty()
        assertThat(state.wallets(WalletLocation.INLINE)).isEmpty()
    }

    @Test
    fun `wallets maintains order GooglePay then Link then ShopPay`() {
        val state = createWalletsState(
            hasLink = true,
            hasGooglePay = true,
            hasShopPay = true,
            walletsAllowedInHeader = emptyList(),
        )

        val wallets = state.wallets(WalletLocation.INLINE)

        assertThat(wallets[0]).isInstanceOf(WalletsState.GooglePay::class.java)
        assertThat(wallets[1]).isInstanceOf(WalletsState.Link::class.java)
        assertThat(wallets[2]).isInstanceOf(WalletsState.ShopPay::class.java)
    }

    // endregion

    private fun createWalletsState(
        hasLink: Boolean,
        hasGooglePay: Boolean,
        hasShopPay: Boolean,
        walletsAllowedInHeader: List<WalletType>,
    ): WalletsState {
        return WalletsState(
            link = if (hasLink) WalletsState.Link(state = LinkButtonState.Default) else null,
            googlePay = if (hasGooglePay) {
                WalletsState.GooglePay(
                    buttonType = GooglePayButtonType.Pay,
                    allowCreditCards = true,
                    billingAddressParameters = null,
                    additionalEnabledNetworks = emptyList(),
                )
            } else {
                null
            },
            shopPay = if (hasShopPay) WalletsState.ShopPay else null,
            walletsAllowedInHeader = walletsAllowedInHeader,
            buttonsEnabled = true,
            dividerTextResource = R.string.stripe_paymentsheet_or_pay_using,
            onGooglePayPressed = {},
            onLinkPressed = {},
            onShopPayPressed = {},
            cardFundingFilter = DefaultCardFundingFilter,
            cardBrandFilter = DefaultCardBrandFilter,
        )
    }
}
