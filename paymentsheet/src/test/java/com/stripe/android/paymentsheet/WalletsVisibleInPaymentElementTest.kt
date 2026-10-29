package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentsheet.PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility
import com.stripe.android.paymentsheet.PaymentSheet.WalletButtonsConfiguration.Wallet
import com.stripe.android.paymentsheet.PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility
import org.junit.Test

class WalletsVisibleInPaymentElementTest {
    @Test
    fun `all wallets are visible by default when wallet buttons are not rendered`() {
        assertThat(visibleWallets(walletButtonsRendered = false)).containsExactlyElementsIn(WalletType.entries)
    }

    @Test
    fun `no wallets are visible by default when wallet buttons are rendered`() {
        assertThat(visibleWallets(walletButtonsRendered = true)).isEmpty()
    }

    @Test
    fun `automatic wallets are hidden when wallet buttons are rendered`() {
        val visibleWallets = visibleWallets(
            walletButtonsRendered = true,
            paymentElement = mapOf(
                Wallet.GooglePay to PaymentElementVisibility.Automatic,
                Wallet.Link to PaymentElementVisibility.Automatic,
            ),
        )

        assertThat(visibleWallets).isEmpty()
    }

    @Test
    fun `never wallets are hidden when wallet buttons are not rendered`() {
        val visibleWallets = visibleWallets(
            walletButtonsRendered = false,
            paymentElement = mapOf(Wallet.GooglePay to PaymentElementVisibility.Never),
        )

        assertThat(visibleWallets).containsExactly(WalletType.Link)
    }

    @Test
    fun `never wallets are hidden when wallet buttons are rendered`() {
        val visibleWallets = visibleWallets(
            walletButtonsRendered = true,
            paymentElement = mapOf(
                Wallet.GooglePay to PaymentElementVisibility.Never,
                Wallet.Link to PaymentElementVisibility.Always,
            ),
            walletButtonsView = mapOf(Wallet.GooglePay to WalletButtonsViewVisibility.Never),
        )

        assertThat(visibleWallets).containsExactly(WalletType.Link)
    }

    @Test
    fun `always wallets are visible when wallet buttons are rendered`() {
        val visibleWallets = visibleWallets(
            walletButtonsRendered = true,
            paymentElement = mapOf(Wallet.Link to PaymentElementVisibility.Always),
        )

        assertThat(visibleWallets).containsExactly(WalletType.Link)
    }

    @Test
    fun `automatic wallets hidden from wallet buttons are visible when wallet buttons are rendered`() {
        val visibleWallets = visibleWallets(
            walletButtonsRendered = true,
            walletButtonsView = mapOf(
                Wallet.GooglePay to WalletButtonsViewVisibility.Never,
                Wallet.Link to WalletButtonsViewVisibility.Always,
            ),
        )

        assertThat(visibleWallets).containsExactly(WalletType.GooglePay)
    }

    private fun visibleWallets(
        walletButtonsRendered: Boolean,
        paymentElement: Map<Wallet, PaymentElementVisibility> = emptyMap(),
        walletButtonsView: Map<Wallet, WalletButtonsViewVisibility> = emptyMap(),
    ): List<WalletType> {
        return PaymentSheet.WalletButtonsConfiguration(
            visibility = PaymentSheet.WalletButtonsConfiguration.Visibility(
                paymentElement = paymentElement,
                walletButtonsView = walletButtonsView,
            ),
        ).walletsVisibleInPaymentElement(walletButtonsRendered = walletButtonsRendered)
    }
}
