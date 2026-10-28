package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.isInstanceOf
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class VerticalModeInitialScreenFactoryTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `returns form screen when only one payment method available`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("cashapp"),
            )
        )
    ) {
        assertThat(screens).hasSize(1)
        assertThat(screens[0]).isInstanceOf<PaymentSheetScreen.VerticalModeForm>()
    }

    @Test
    fun `hides wallet header when metadata advertises wallets that will not render`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(paymentMethodTypes = listOf("card")),
            availableWallets = listOf(WalletType.GooglePay),
            isGooglePayReady = true,
        ),
        walletsState = null,
    ) {
        screens.single().showsWalletsHeader(isCompleteFlow = false).test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `shows Google Pay header when fallback metadata omits wallets`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(paymentMethodTypes = listOf("card")),
            availableWallets = emptyList(),
            isGooglePayReady = true,
        ),
        walletsState = googlePayWalletsState,
    ) {
        screens.single().showsWalletsHeader(isCompleteFlow = true).test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `returns list screen when only one payment method available with saved payment methods`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("cashapp"),
            )
        ),
        hasSavedPaymentMethods = true
    ) {
        assertThat(screens).hasSize(1)
        assertThat(screens[0]).isInstanceOf<PaymentSheetScreen.VerticalMode>()
    }

    @Test
    fun `returns both screens when selection is new card`() = runScenario(
        selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
    ) {
        assertThat(screens).hasSize(2)
        assertThat(screens[0]).isInstanceOf<PaymentSheetScreen.VerticalMode>()
        assertThat(screens[1]).isInstanceOf<PaymentSheetScreen.VerticalModeForm>()
    }

    @Test
    fun `reports promotion displayed for single supported PM with no saved PMs`() {
        val promotionsHelper = FakePaymentMethodMessagePromotionsHelper(
            promotions = FakePaymentMethodMessagePromotionsHelper.promotions
        )
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("klarna"),
                )
            ),
            paymentMethodMessagePromotionsHelper = promotionsHelper,
        ) {
            assertThat(screens).hasSize(1)
            assertThat(screens[0]).isInstanceOf<PaymentSheetScreen.VerticalModeForm>()
        }
        runTest {
            val event = promotionsHelper.reportPromotionDisplayedCalls.awaitItem()
            assertThat(event.first).isEqualTo("klarna")
            assertThat(event.second).isTrue()
        }
    }

    @Test
    fun `does not report promotion displayed when multiple PMs available`() {
        val promotionsHelper = FakePaymentMethodMessagePromotionsHelper(
            promotions = FakePaymentMethodMessagePromotionsHelper.promotions
        )
        runScenario(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card", "klarna"),
                )
            ),
            paymentMethodMessagePromotionsHelper = promotionsHelper,
        ) {
            assertThat(screens).hasSize(1)
            assertThat(screens[0]).isInstanceOf<PaymentSheetScreen.VerticalMode>()
        }
        promotionsHelper.reportPromotionDisplayedCalls.ensureAllEventsConsumed()
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION_WITH_INSTANT_DEBITS_ONBOARDING,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
        ),
        hasSavedPaymentMethods: Boolean = false,
        selection: PaymentSelection? = null,
        walletsState: WalletsState? = null,
        paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeViewModel = FakeBaseSheetViewModel.create(
            paymentMethodMetadata = paymentMethodMetadata,
            initialScreen = PaymentSheetScreen.Loading,
            canGoBack = false,
        )
        fakeViewModel.walletsStateSource.value = walletsState

        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = SavedStateHandle(),
            selection = fakeViewModel.selection,
            customerMetadata = stateFlowOf(
                paymentMethodMetadata.customerMetadata
            ),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        if (hasSavedPaymentMethods) {
            customerStateHolder.setCustomerState(
                CustomerState(
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                    defaultPaymentMethodId = null,
                )
            )
        }

        fakeViewModel.updateSelection(selection)

        val screens = VerticalModeInitialScreenFactory.create(
            viewModel = fakeViewModel,
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper
        )
        Scenario(
            screens = screens
        ).apply { block() }
    }

    private class Scenario(
        val screens: List<PaymentSheetScreen>,
    )

    private val googlePayWalletsState = WalletsState(
        link = null,
        googlePay = WalletsState.GooglePay(
            apiConfiguration = PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG,
            buttonType = GooglePayButtonType.Pay,
            allowCreditCards = true,
            billingAddressParameters = null,
            additionalEnabledNetworks = emptyList(),
        ),
        walletsAllowedInHeader = listOf(WalletType.GooglePay),
        buttonsEnabled = true,
        dividerTextResource = R.string.stripe_paymentsheet_or_pay_using,
        cardFundingFilter = DefaultCardFundingFilter,
        cardBrandFilter = DefaultCardBrandFilter,
        onGooglePayPressed = { throw AssertionError("Not expected") },
        onLinkPressed = { throw AssertionError("Not expected") },
    )
}
