package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_SELECTION
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentsheet.PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
import com.stripe.android.paymentsheet.PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.cvcrecollection.FakeCvcRecollectionHandler
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.Args
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionInteractor
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.elements.CardDetailsSectionController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.time.Duration

@RunWith(RobolectricTestRunner::class)
internal class FormHelperOpenCardScanAutomaticallyTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `should not AutomaticallyLaunchCardScan if card form will be filled PaymentSheet`() {
        val viewModel = createPaymentSheetViewModel()

        viewModel.newPaymentSelection = NewPaymentOptionSelection.New(
            CARD_PAYMENT_SELECTION
        )

        testFormElementsForCardCode(
            viewModel = viewModel,
        ) { formElements ->
            assertThat(formElements[0].controller).isInstanceOf<CardDetailsSectionController>()

            assertThat(
                (formElements[0].controller as CardDetailsSectionController).shouldAutomaticallyLaunchCardScan()
            ).isFalse()
        }
    }

    @Test
    fun `should AutomaticallyLaunchCardScan if card form will be empty PaymentSheet`() {
        val viewModel = createPaymentSheetViewModel()

        viewModel.newPaymentSelection = null
        testFormElementsForCardCode(
            viewModel = viewModel,
        ) { formElements ->
            assertThat(formElements[0].controller).isInstanceOf<CardDetailsSectionController>()

            assertThat(
                (formElements[0].controller as CardDetailsSectionController).shouldAutomaticallyLaunchCardScan()
            ).isTrue()
        }
    }

    @Test
    fun `should not AutomaticallyLaunchCardScan if card form will be filled FlowController`() {
        val viewModel = createPaymentOptionsViewModel()

        viewModel.newPaymentSelection = NewPaymentOptionSelection.New(
            CARD_PAYMENT_SELECTION
        )

        testFormElementsForCardCode(
            viewModel = viewModel,
        ) { formElements ->
            assertThat(formElements[0].controller).isInstanceOf<CardDetailsSectionController>()

            assertThat(
                (formElements[0].controller as CardDetailsSectionController).shouldAutomaticallyLaunchCardScan()
            ).isFalse()
        }
    }

    @Test
    fun `should AutomaticallyLaunchCardScan if card form will be empty FlowController`() {
        val viewModel = createPaymentOptionsViewModel()

        viewModel.newPaymentSelection = null
        testFormElementsForCardCode(
            viewModel = viewModel,
        ) { formElements ->
            assertThat(formElements[0].controller).isInstanceOf<CardDetailsSectionController>()

            assertThat(
                (formElements[0].controller as CardDetailsSectionController).shouldAutomaticallyLaunchCardScan()
            ).isTrue()
        }
    }

    private fun testFormElementsForCardCode(
        viewModel: BaseSheetViewModel,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            openCardScanAutomatically = true
        ),
        block: (List<FormElement>) -> Unit,
    ) {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(true)

        val defaultFormHelper = DefaultFormHelper.create(
            viewModel = viewModel,
            paymentMethodMetadata = paymentMethodMetadata,
            shouldCreateAutomaticallyLaunchedCardScanFormDataHelper = true
        )

        val formElements = defaultFormHelper.formElementsForCode(PaymentMethod.Type.Card.code)

        block(formElements)
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)
    }

    private fun createPaymentOptionsViewModel(
        customer: CustomerState? = EMPTY_CUSTOMER_STATE.copy(paymentMethods = listOf(CARD_PAYMENT_METHOD)),
        args: PaymentOptionContract.Args = PAYMENT_OPTION_CONTRACT_ARGS,
    ): PaymentOptionsViewModel {
        return TestViewModelFactory.create(
            savedStateHandle = SavedStateHandle(),
        ) { linkHandler, thisSavedStateHandle ->
            PaymentOptionsViewModel(
                args = args,
                eventReporter = FakeEventReporter(),
                customerRepository = FakeCustomerRepository(customer?.paymentMethods ?: emptyList()),
                workContext = testDispatcher,
                savedStateHandle = thisSavedStateHandle,
                linkHandler = linkHandler,
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                linkGateFactory = { FakeLinkGate() },
                linkPaymentLauncher = mock<LinkPaymentLauncher>(),
                linkAccountHolder = LinkAccountHolder(SavedStateHandle())
            )
        }
    }

    private fun createPaymentSheetViewModel(
        customer: CustomerState? = EMPTY_CUSTOMER_STATE.copy(paymentMethods = listOf(CARD_PAYMENT_METHOD)),
        initialPaymentSelection: PaymentSelection? =
            customer?.paymentMethods?.firstOrNull()?.let { PaymentSelection.Saved(it) },
        paymentElementLoader: PaymentElementLoader = FakePaymentElementLoader(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            shouldFail = false,
            linkState = null,
            customer = customer,
            delay = Duration.ZERO,
            isGooglePayAvailable = false,
            paymentSelection = initialPaymentSelection,
            validationError = null,
        ),
    ): PaymentSheetViewModel {
        return TestViewModelFactory.create(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            savedStateHandle = SavedStateHandle(),
        ) { linkHandler, thisSavedStateHandle ->
            PaymentSheetViewModel(
                args = ARGS_CUSTOMER_WITH_GOOGLEPAY,
                eventReporter = FakeEventReporter(),
                paymentElementLoader = paymentElementLoader,
                customerRepository = FakeCustomerRepository(customer?.paymentMethods ?: emptyList()),
                prefsRepository = FakePrefsRepository(),
                logger = Logger.noop(),
                workContext = testDispatcher,
                savedStateHandle = thisSavedStateHandle,
                linkHandler = linkHandler,
                confirmationHandlerFactory = { FakeConfirmationHandler() },
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                errorReporter = FakeErrorReporter(),
                cvcRecollectionHandler = FakeCvcRecollectionHandler(),
                cvcRecollectionInteractorFactory = object : CvcRecollectionInteractor.Factory {
                    override fun create(
                        args: Args,
                        processing: StateFlow<Boolean>,
                        coroutineScope: CoroutineScope,
                    ): CvcRecollectionInteractor {
                        return FakeCvcRecollectionInteractor()
                    }
                },
                isLiveModeProvider = { false }
            )
        }
    }

    private val PAYMENT_OPTION_CONTRACT_ARGS = PaymentOptionContract.Args(
        state = PaymentSheetState.Full(
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY.asCommonConfiguration(),
            paymentSelection = null,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFactory.create(),
                isGooglePayReady = true,
            ),
        ),
        configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        enableLogging = false,
        productUsage = mock(),
        paymentElementCallbackIdentifier = "PaymentOptionsViewModelTestCallbackIdentifier",
        linkAccountInfo = LinkAccountUpdate.Value(
            account = null,
            lastUpdateReason = null
        ),
        walletButtonsRendered = false,
    )
}
