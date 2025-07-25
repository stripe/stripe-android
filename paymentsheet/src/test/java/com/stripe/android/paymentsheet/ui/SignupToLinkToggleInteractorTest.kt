package com.stripe.android.paymentsheet.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignupToLinkToggleInteractorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
    private val flowControllerState = MutableStateFlow<DefaultFlowController.State?>(null)

    private val titleText = "title"
    private val descriptionText = "description"
    private val termsText = AnnotatedString("terms")

    private val mockStringProvider = object : SignupToLinkToggleStringProvider {
        override val title: String = titleText
        override val description: String = descriptionText
        override val termsAndConditions: AnnotatedString = termsText
    }

    private val interactor = DefaultSignupToLinkToggleInteractor(
        flowControllerState = flowControllerState,
        linkAccountHolder = linkAccountHolder,
        stringProvider = mockStringProvider
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state is Visible when Link available, no account, and ShopPay not available`() = runTest {
        // Given
        setupLinkAvailable()
        setupNoExistingAccount()
        setupWalletButtonsConfiguration(walletTypesToShow = listOf("google_pay")) // No ShopPay

        // When
        val state = interactor.state.value

        // Then
        assertThat(state is PaymentSheet.LinkSignupOptInState.Visible).isTrue()
        val visibleState = state as PaymentSheet.LinkSignupOptInState.Visible
        assertThat(visibleState.title).isEqualTo(titleText)
        assertThat(visibleState.description).isEqualTo(descriptionText)
    }

    @Test
    fun `state is Hidden when ShopPay is available`() = runTest {
        // Given
        setupLinkAvailable()
        setupNoExistingAccount()
        setupWalletButtonsConfiguration(walletTypesToShow = listOf("shop_pay", "google_pay")) // ShopPay present

        // When
        val state = interactor.state.value

        // Then
        assertThat(state).isEqualTo(PaymentSheet.LinkSignupOptInState.Hidden)
    }

    @Test
    fun `state is Hidden when Link state is null`() = runTest {
        // Given
        setupLinkNotAvailable() // No Link state
        setupNoExistingAccount()
        setupWalletButtonsConfiguration(walletTypesToShow = listOf("google_pay")) // No ShopPay

        // When
        val state = interactor.state.value

        // Then
        assertThat(state).isEqualTo(PaymentSheet.LinkSignupOptInState.Hidden)
    }

    @Test
    fun `state is Hidden when Link account already exists`() = runTest {
        // Given
        setupLinkAvailable()
        setupExistingAccount() // Account exists
        setupWalletButtonsConfiguration(walletTypesToShow = listOf("google_pay")) // No ShopPay

        // When
        val state = interactor.state.value

        // Then
        assertThat(state).isEqualTo(PaymentSheet.LinkSignupOptInState.Hidden)
    }

    @Test
    fun `state is Hidden when flowControllerState is null`() = runTest {
        // Given
        flowControllerState.value = null // No state
        setupNoExistingAccount()
        setupWalletButtonsConfiguration(walletTypesToShow = listOf("google_pay"))

        // When
        val state = interactor.state.value

        // Then
        assertThat(state).isEqualTo(PaymentSheet.LinkSignupOptInState.Hidden)
    }

    @Test
    fun `state is Visible when no ShopPay in available wallets and Link available and no account`() = runTest {
        // Given
        setupLinkAvailable()
        setupNoExistingAccount()
        setupWalletButtonsConfiguration(walletTypesToShow = listOf("google_pay")) // No ShopPay in available wallets

        // When
        val state = interactor.state.value

        // Then
        // When ShopPay is not in availableWallets, so ShopPay is not considered available
        // Therefore state should be Visible (Link available + no account + no ShopPay restriction)
        assertThat(state is PaymentSheet.LinkSignupOptInState.Visible).isTrue()
    }

    @Test
    fun `handleToggleChange updates toggle value`() = runTest {
        // When
        interactor.handleToggleChange(true)

        // Then
        assertThat(interactor.getSignupToLinkValue()).isTrue()

        // When
        interactor.handleToggleChange(false)

        // Then
        assertThat(interactor.getSignupToLinkValue()).isFalse()
    }

    @Test
    fun `getSignupToLinkValue returns current toggle state`() = runTest {
        // Given
        assertThat(interactor.getSignupToLinkValue()).isFalse()

        // When
        interactor.handleToggleChange(true)

        // Then
        assertThat(interactor.getSignupToLinkValue()).isTrue()

        // When
        interactor.handleToggleChange(false)

        // Then
        assertThat(interactor.getSignupToLinkValue()).isFalse()
    }

    @Test
    fun `state updates when Link account status changes`() = runTest {
        // Given
        setupLinkAvailable()
        setupWalletButtonsConfiguration(walletTypesToShow = listOf("google_pay"))

        // Initially no account
        setupNoExistingAccount()
        assertThat(interactor.state.value is PaymentSheet.LinkSignupOptInState.Visible).isTrue()

        // When account is created
        setupExistingAccount()

        // Then
        assertThat(interactor.state.value).isEqualTo(PaymentSheet.LinkSignupOptInState.Hidden)

        // When account is removed
        setupNoExistingAccount()

        // Then
        assertThat(interactor.state.value is PaymentSheet.LinkSignupOptInState.Visible).isTrue()
    }

    @Test
    fun `state updates when wallet configuration changes`() = runTest {
        // Given
        setupLinkAvailable()
        setupNoExistingAccount()

        // Initially no ShopPay
        setupWalletButtonsConfiguration(walletTypesToShow = listOf("google_pay"))
        assertThat(interactor.state.value is PaymentSheet.LinkSignupOptInState.Visible).isTrue()

        // When ShopPay is added
        setupWalletButtonsConfiguration(walletTypesToShow = listOf("google_pay", "shop_pay"))

        // Then
        assertThat(interactor.state.value).isEqualTo(PaymentSheet.LinkSignupOptInState.Hidden)

        // When ShopPay is removed
        setupWalletButtonsConfiguration(walletTypesToShow = listOf("google_pay"))

        // Then
        assertThat(interactor.state.value is PaymentSheet.LinkSignupOptInState.Visible).isTrue()
    }

    // Helper methods
    private fun setupLinkAvailable() {
        val linkState = LinkState(
            configuration = TestFactory.LINK_CONFIGURATION,
            loginState = LinkState.LoginState.LoggedOut,
            signupMode = com.stripe.android.link.ui.inline.LinkSignupMode.InsteadOfSaveForFutureUse
        )

        // Use PaymentMethodMetadataFactory instead of mocks
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = linkState
        )

        val paymentSheetState = createPaymentSheetState(paymentMethodMetadata)
        val flowControllerState = createFlowControllerState(paymentSheetState)

        this.flowControllerState.value = flowControllerState
    }

    private fun setupLinkNotAvailable() {
        // Use PaymentMethodMetadataFactory with null linkState instead of mocks
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = null
        )

        val paymentSheetState = createPaymentSheetState(paymentMethodMetadata)
        val flowControllerState = createFlowControllerState(paymentSheetState)

        this.flowControllerState.value = flowControllerState
    }

    private fun setupNoExistingAccount() {
        linkAccountHolder.set(LinkAccountUpdate.Value(null))
    }

    private fun setupExistingAccount() {
        linkAccountHolder.set(LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT))
    }

    private fun setupWalletButtonsConfiguration(walletTypesToShow: List<String>) {
        // Convert wallet type strings to WalletType enum
        val availableWallets = walletTypesToShow.mapNotNull { walletTypeString ->
            when (walletTypeString) {
                "google_pay" -> WalletType.GooglePay
                "link" -> WalletType.Link
                "shop_pay" -> WalletType.ShopPay
                else -> null
            }
        }

        // Get the current state to preserve Link state
        val currentState = flowControllerState.value
        val currentLinkState = currentState?.paymentSheetState?.paymentMethodMetadata?.linkState

        // Create new PaymentMethodMetadata with updated availableWallets
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = currentLinkState,
            availableWallets = availableWallets
        )

        val paymentSheetState = createPaymentSheetState(paymentMethodMetadata)
        val flowControllerState = createFlowControllerState(paymentSheetState)

        this.flowControllerState.value = flowControllerState
    }

    private fun createPaymentSheetState(
        paymentMethodMetadata: PaymentMethodMetadata
    ): PaymentSheetState.Full {
        return PaymentSheetState.Full(
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            config = PaymentSheetFixtures.CONFIG_MINIMUM.asCommonConfiguration(),
            paymentSelection = null,
            validationError = null,
            paymentMethodMetadata = paymentMethodMetadata
        )
    }

    private fun createFlowControllerState(
        paymentSheetState: PaymentSheetState.Full
    ): DefaultFlowController.State {
        return DefaultFlowController.State(
            paymentSheetState = paymentSheetState,
            config = PaymentSheetFixtures.CONFIG_MINIMUM
        )
    }
}
