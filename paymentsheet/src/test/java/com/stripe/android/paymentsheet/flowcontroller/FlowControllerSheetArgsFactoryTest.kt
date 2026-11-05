package com.stripe.android.paymentsheet.flowcontroller

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility
import com.stripe.android.paymentsheet.PaymentSheet.WalletButtonsConfiguration.Wallet
import com.stripe.android.paymentsheet.PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FlowControllerSheetArgsFactoryTest {

    @OptIn(AddressAutocompletePreview::class)
    @Test
    fun `create maps FlowController state into payment options sheet args`() {
        val configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
            .primaryButtonLabel("Pick")
            .googlePlacesApiKey("places_key")
            .build()
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        val linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
        val state = PaymentSheetFixtures.FLOW_CONTROLLER_PAYMENT_SHEET_STATE

        val args = FlowControllerSheetArgsFactory.create(
            paymentSheetState = state,
            configuration = configuration,
            paymentSelection = selection,
            linkAccountInfo = linkAccountInfo,
            walletButtonsRendered = false,
            productUsage = setOf("PaymentSheet.FlowController"),
            paymentElementCallbackIdentifier = "callback_id",
            promotions = null,
        )

        assertThat(args.paymentMethodMetadata).isEqualTo(state.paymentMethodMetadata)
        assertThat(args.configuration.primaryButtonLabel).isEqualTo("Pick")
        assertThat(args.productUsage).containsExactly("PaymentSheet.FlowController")
        assertThat(args.paymentElementCallbackIdentifier).isEqualTo("callback_id")
        assertThat(args.statusBarColor).isNull()
        assertThat(args.selection).isEqualTo(selection)
        assertThat(args.previousNewSelections.isEmpty).isTrue()
        assertThat(args.customerState).isEqualTo(state.customer)
        assertThat(args.linkAccountInfo).isEqualTo(linkAccountInfo)
        assertThat(args.promotions).isEmpty()
        assertThat(args.launchMode).isEqualTo(
            EmbeddedLaunchMode.PaymentOptions(
                flowController = EmbeddedLaunchMode.FlowControllerOptions(
                    visibleWallets = WalletType.entries,
                    googlePlacesApiKey = "places_key",
                ),
            )
        )
        assertThat(args.presentationState).isEqualTo(EmbeddedActivityArgs.PresentationState.Ready)
    }

    @Test
    fun `embedded sheet configuration always continues instead of confirming`() {
        val configuration = PaymentSheet.Configuration.Builder("Example, Inc.")
            .appearance(PaymentSheet.Appearance(colorsLight = PaymentSheet.Colors.defaultDark))
            .allowsDelayedPaymentMethods(true)
            .build()

        val embeddedConfiguration = configuration.asEmbeddedSheetConfiguration()

        assertThat(embeddedConfiguration.formSheetAction).isEqualTo(EmbeddedPaymentElement.FormSheetAction.Continue)
        assertThat(embeddedConfiguration.merchantDisplayName).isEqualTo("Example, Inc.")
        assertThat(embeddedConfiguration.appearance).isEqualTo(configuration.appearance)
        assertThat(embeddedConfiguration.allowsDelayedPaymentMethods).isTrue()
    }

    @Test
    fun `Link but not Google Pay should be available if only Link can be visible when buttons not rendered`() =
        assertVisibleWallets(
            paymentElement = mapOf(
                Wallet.GooglePay to PaymentElementVisibility.Never,
                Wallet.Link to PaymentElementVisibility.Automatic,
            ),
            walletButtonsRendered = false,
            expected = listOf(WalletType.Link),
        )

    @Test
    fun `Google Pay but not Link should be available if only Google Pay can be visible when buttons not rendered`() =
        assertVisibleWallets(
            paymentElement = mapOf(
                Wallet.GooglePay to PaymentElementVisibility.Automatic,
                Wallet.Link to PaymentElementVisibility.Never,
            ),
            walletButtonsRendered = false,
            expected = listOf(WalletType.GooglePay),
        )

    @Test
    fun `All wallets should be available if wallet buttons not rendered & automatic`() = assertVisibleWallets(
        paymentElement = mapOf(
            Wallet.GooglePay to PaymentElementVisibility.Automatic,
            Wallet.Link to PaymentElementVisibility.Automatic,
        ),
        walletButtonsRendered = false,
        expected = WalletType.entries,
    )

    @Test
    fun `Wallets should not be available if wallet buttons not rendered & never`() = assertVisibleWallets(
        paymentElement = mapOf(
            Wallet.GooglePay to PaymentElementVisibility.Never,
            Wallet.Link to PaymentElementVisibility.Never,
        ),
        walletButtonsRendered = false,
        expected = emptyList(),
    )

    @Test
    fun `Wallets should not be available if wallet buttons rendered & automatic`() = assertVisibleWallets(
        paymentElement = mapOf(
            Wallet.GooglePay to PaymentElementVisibility.Automatic,
            Wallet.Link to PaymentElementVisibility.Automatic,
        ),
        walletButtonsRendered = true,
        expected = emptyList(),
    )

    @Test
    fun `Wallets should be available if wallet buttons rendered & always`() = assertVisibleWallets(
        paymentElement = mapOf(
            Wallet.GooglePay to PaymentElementVisibility.Always,
            Wallet.Link to PaymentElementVisibility.Always,
        ),
        walletButtonsRendered = true,
        expected = WalletType.entries,
    )

    @Test
    fun `Wallets should be available if wallet buttons rendered but visibility for wallets is set to never`() =
        assertVisibleWallets(
            paymentElement = mapOf(
                Wallet.GooglePay to PaymentElementVisibility.Automatic,
                Wallet.Link to PaymentElementVisibility.Automatic,
            ),
            walletButtonsView = mapOf(
                Wallet.GooglePay to WalletButtonsViewVisibility.Never,
                Wallet.Link to WalletButtonsViewVisibility.Never,
            ),
            walletButtonsRendered = true,
            expected = WalletType.entries,
        )

    private fun assertVisibleWallets(
        paymentElement: Map<Wallet, PaymentElementVisibility>,
        walletButtonsView: Map<Wallet, WalletButtonsViewVisibility> = emptyMap(),
        walletButtonsRendered: Boolean,
        expected: List<WalletType>,
    ) {
        val walletButtons = PaymentSheet.WalletButtonsConfiguration(
            visibility = PaymentSheet.WalletButtonsConfiguration.Visibility(
                paymentElement = paymentElement,
                walletButtonsView = walletButtonsView,
            ),
        )

        assertThat(walletButtons.walletsVisibleInPaymentOptions(walletButtonsRendered))
            .containsExactlyElementsIn(expected)
    }
}
