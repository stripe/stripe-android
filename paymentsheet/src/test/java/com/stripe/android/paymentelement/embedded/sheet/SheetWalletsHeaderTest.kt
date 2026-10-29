package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.FakeSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.navigation.horizontalModeWalletsDividerSpacing
import com.stripe.android.paymentsheet.navigation.verticalModeWalletsDividerSpacing
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import org.junit.Test

internal class SheetWalletsHeaderTest {
    @Test
    fun `root form uses vertical spacing`() {
        val result = createState(screen = createFormScreen())

        assertThat(result!!.dividerSpacing).isEqualTo(verticalModeWalletsDividerSpacing)
    }

    @Test
    fun `root vertical payment options uses vertical spacing`() {
        val result = createState(screen = createVerticalPaymentOptionsScreen())

        assertThat(result!!.dividerSpacing).isEqualTo(verticalModeWalletsDividerSpacing)
    }

    @Test
    fun `root horizontal payment options uses horizontal spacing`() {
        val result = createState(screen = createHorizontalPaymentOptionsScreen())

        assertThat(result!!.dividerSpacing).isEqualTo(horizontalModeWalletsDividerSpacing)
    }

    @Test
    fun `root horizontal saved payment options uses horizontal spacing`() {
        val result = createState(
            screen = EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions(
                interactor = FakeSelectSavedPaymentMethodsInteractor(),
                sheetActivityState = FakeSheetActivityStateHolder().state,
                mandateText = stateFlowOf(null),
                onContinueClick = {},
                onPrimaryButtonDisabledClick = {},
            ),
        )

        assertThat(result!!.dividerSpacing).isEqualTo(horizontalModeWalletsDividerSpacing)
    }

    @Test
    fun `nested screen hides header`() {
        val result = createState(screen = createFormScreen(), canGoBack = true)

        assertThat(result).isNull()
    }

    @Test
    fun `unavailable wallets hide header`() {
        val result = createState(
            screen = createFormScreen(),
            walletsState = createWalletsState(walletsAllowedInHeader = emptyList()),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `manage screen hides header`() {
        val result = createState(screen = EmbeddedNavigator.Screen.ManageAll(FakeManageScreenInteractor()))

        assertThat(result).isNull()
    }

    @Test
    fun `manage launch mode hides header`() {
        val result = createState(
            launchMode = EmbeddedLaunchMode.Manage,
            screen = createVerticalPaymentOptionsScreen(),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `form launch mode hides header`() {
        val result = createState(
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
            screen = createFormScreen(),
        )

        assertThat(result).isNull()
    }

    private fun createState(
        launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.PaymentOptions(visibleWallets = WalletType.entries),
        screen: EmbeddedNavigator.Screen,
        canGoBack: Boolean = false,
        walletsState: WalletsState? = createWalletsState(),
    ): SheetWalletsHeaderState? {
        return walletsHeaderState(launchMode, screen, canGoBack, walletsState)
    }

    private fun createWalletsState(
        walletsAllowedInHeader: List<WalletType> = WalletType.entries,
    ): WalletsState {
        return WalletsState(
            link = WalletsState.Link(LinkButtonState.Default, LinkBrand.Link),
            googlePay = WalletsState.GooglePay(
                apiConfiguration = PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG,
                buttonType = GooglePayButtonType.Pay,
                allowCreditCards = false,
                billingAddressParameters = null,
                additionalEnabledNetworks = emptyList(),
            ),
            walletsAllowedInHeader = walletsAllowedInHeader,
            buttonsEnabled = true,
            dividerTextResource = R.string.stripe_paymentsheet_or_pay_using,
            cardFundingFilter = DefaultCardFundingFilter,
            cardBrandFilter = DefaultCardBrandFilter,
            onGooglePayPressed = {},
            onLinkPressed = {},
        )
    }

    private fun createFormScreen(): EmbeddedNavigator.Screen.Form {
        return EmbeddedNavigator.Screen.Form(
            formInteractor = FakeFormInteractor,
            sheetActivityStateHolder = FakeSheetActivityStateHolder(),
            confirmationHelper = FakeSheetActivityConfirmationHelper(),
            embeddedSelectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()),
            customerStateHolder = FakeCustomerStateHolder(),
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
        )
    }

    private fun createVerticalPaymentOptionsScreen(): EmbeddedNavigator.Screen.VerticalPaymentOptions {
        return EmbeddedNavigator.Screen.VerticalPaymentOptions(
            interactor = FakePaymentMethodVerticalLayoutInteractor.create(),
            sheetActivityState = FakeSheetActivityStateHolder().state,
            onContinueClick = {},
            onPrimaryButtonDisabledClick = {},
        )
    }

    private fun createHorizontalPaymentOptionsScreen(): EmbeddedNavigator.Screen.HorizontalPaymentOptions {
        return EmbeddedNavigator.Screen.HorizontalPaymentOptions(
            interactor = FakeAddPaymentMethodInteractor,
            sheetActivityState = FakeSheetActivityStateHolder().state,
            onContinueClick = {},
            onPrimaryButtonDisabledClick = {},
        )
    }

    private object FakeFormInteractor : VerticalModeFormInteractor {
        override val state: StateFlow<VerticalModeFormInteractor.State>
            get() = error("Not expected")

        override fun handleViewAction(viewAction: VerticalModeFormInteractor.ViewAction) {
            error("Not expected")
        }

        override fun close() {
            error("Not expected")
        }
    }

    private object FakeAddPaymentMethodInteractor : AddPaymentMethodInteractor {
        override val state: StateFlow<AddPaymentMethodInteractor.State>
            get() = error("Not expected")

        override fun handleViewAction(viewAction: AddPaymentMethodInteractor.ViewAction) {
            error("Not expected")
        }

        override fun close() {
            error("Not expected")
        }
    }
}
