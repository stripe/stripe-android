package com.stripe.android.paymentelement.embedded.sheet

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentelement.embedded.form.OnClickDelegateOverrideImpl
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteLauncher
import com.stripe.android.paymentsheet.addresselement.analytics.FakeAddressLauncherEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.FakeSavedPaymentMethodConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import javax.inject.Provider

internal class EmbeddedNavigatorScreenScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.padding(16.dp),
    )

    @Test
    fun displaysManageAll() {
        val cards = listOf(
            createCard("4242"),
            createCard("4000"),
            createCard("0007"),
        ).map { it.toDisplayableSavedPaymentMethod() }
        val screen = EmbeddedNavigator.Screen.ManageAll(
            FakeManageScreenInteractor(
                initialState = ManageScreenInteractor.State(
                    paymentMethods = cards,
                    currentSelection = cards.first(),
                    isEditing = false,
                    canEdit = true,
                    linkBrand = LinkBrand.Link,
                ),
            ),
        )

        snapshot(screen)
    }

    @Test
    fun displaysManageUpdate() {
        val screen = EmbeddedNavigator.Screen.ManageUpdate(FakeUpdatePaymentMethodInteractor())

        snapshot(
            screen = screen,
            previousScreen = EmbeddedNavigator.Screen.ManageAll(FakeManageScreenInteractor()),
        )
    }

    @Test
    fun displaysForm() {
        val screen = createFormScreen()

        snapshot(
            screen = screen,
            previousScreen = createVerticalPaymentOptionsScreen(),
        )
    }

    @Test
    fun displaysSavedPaymentMethodConfirm() {
        val screen = EmbeddedNavigator.Screen.SavedPaymentMethodConfirm(
            interactor = FakeSavedPaymentMethodConfirmInteractor(formEnabled = true),
            isLiveMode = false,
            sheetActivityStateHolder = createSheetActivityStateHolder(),
            confirmationHelper = FakeSheetActivityConfirmationHelper(),
            embeddedSelectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()),
            customerStateHolder = FakeCustomerStateHolder(),
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
        )

        snapshot(
            screen = screen,
            previousScreen = createVerticalPaymentOptionsScreen(),
        )
    }

    @Test
    fun displaysVerticalPaymentOptions() {
        snapshot(createVerticalPaymentOptionsScreen())
    }

    @Test
    fun displaysHorizontalPaymentOptions() {
        val metadata = createPaymentOptionsMetadata(PaymentSheet.PaymentMethodLayout.Horizontal)
        val screen = EmbeddedNavigator.Screen.HorizontalPaymentOptions(
            interactor = FakeAddPaymentMethodInteractor(
                initialState = FakeAddPaymentMethodInteractor.createState(metadata),
            ),
            sheetActivityState = createSheetActivityStateHolder().state,
            onContinueClick = {},
            onPrimaryButtonDisabledClick = {},
        )

        snapshot(screen)
    }

    private fun createFormScreen(): EmbeddedNavigator.Screen.Form {
        val metadata = PaymentMethodMetadataFactory.create()
        val launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card")
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val stateHolder = createSheetActivityStateHolder(metadata, selectionHolder)
        val eventReporter = FakeEventReporter()
        val interactor = EmbeddedFormInteractorFactory(
            paymentMethodMetadata = metadata,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = EmbeddedFormHelperFactory(
                linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
                embeddedSelectionHolder = selectionHolder,
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                savedStateHandle = SavedStateHandle(),
                isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
            ),
            viewModelScope = TestScope(UnconfinedTestDispatcher()),
            sheetActivityStateHolder = stateHolder,
            tapToAddHelper = FakeTapToAddHelper.noOp(),
            eventReporter = eventReporter,
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
            autocompleteAddressInteractorFactory = TestAutocompleteAddressInteractor.noOpFactory(),
            launchMode = launchMode,
        ).create(
            paymentMethodCode = "card",
            hasSavedPaymentMethods = false,
        )

        return EmbeddedNavigator.Screen.Form(
            formInteractor = interactor,
            sheetActivityStateHolder = stateHolder,
            confirmationHelper = FakeSheetActivityConfirmationHelper(),
            embeddedSelectionHolder = selectionHolder,
            customerStateHolder = FakeCustomerStateHolder(),
            launchMode = launchMode,
        )
    }

    private fun createVerticalPaymentOptionsScreen(): EmbeddedNavigator.Screen.VerticalPaymentOptions {
        return EmbeddedNavigator.Screen.VerticalPaymentOptions(
            interactor = FakePaymentMethodVerticalLayoutInteractor.create(
                paymentMethodMetadata = createPaymentOptionsMetadata(PaymentSheet.PaymentMethodLayout.Vertical),
            ),
            isLiveMode = false,
            sheetActivityState = createSheetActivityStateHolder().state,
            onContinueClick = {},
            onPrimaryButtonDisabledClick = {},
        )
    }

    private fun createPaymentOptionsMetadata(
        layout: PaymentSheet.PaymentMethodLayout,
    ): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp", "klarna"),
            ),
            paymentMethodLayout = layout,
        )
    }

    private fun createCard(last4: String): PaymentMethod {
        val card = PaymentMethodFixtures.createCard()
        return card.copy(card = card.card?.copy(last4 = last4))
    }

    private fun createSheetActivityStateHolder(
        metadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        selectionHolder: DefaultEmbeddedSelectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()),
    ): DefaultSheetActivityStateHolder {
        return DefaultSheetActivityStateHolder(
            paymentMethodMetadata = metadata,
            selectionHolder = selectionHolder,
            configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration,
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            onClickDelegate = OnClickDelegateOverrideImpl(),
            eventReporter = FakeEventReporter(),
            confirmationHandler = FakeConfirmationHandler(),
            tapToAddHelper = FakeTapToAddHelper.noOp(),
            customerStateHolder = FakeCustomerStateHolder(),
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
            linkHandler = LinkHandler(FakeLinkConfigurationCoordinator()),
            eventReporterMode = EventReporter.Mode.Embedded,
            autocompleteAddressInteractorFactory = PaymentElementAutocompleteAddressInteractor.Factory(
                launcher = TestAutocompleteLauncher.noOp(),
                autocompleteConfig = AutocompleteAddressInteractor.Config(
                    googlePlacesApiKey = null,
                    autocompleteCountries = emptySet(),
                ),
                placesClient = null,
                stripeAutocompleteRepository = null,
                coroutineScope = null,
                shouldUseAutocompleteProxyEndpointsProvider = { false },
                eventReporter = FakeAddressLauncherEventReporter(),
            ),
            savedStateHandle = SavedStateHandle(),
            embeddedNavigatorProvider = Provider { error("Not expected") },
            savedPaymentMethodConfirmScreenFactoryProvider = Provider { error("Not expected") },
        )
    }

    private fun snapshot(
        screen: EmbeddedNavigator.Screen,
        previousScreen: EmbeddedNavigator.Screen? = null,
    ) {
        val eventReporter = FakeEventReporter()
        val navigator = EmbeddedNavigator(
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            initialBackStack = listOfNotNull(previousScreen, screen),
            eventReporter = eventReporter,
        )

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                EventReporterProvider(eventReporter) {
                    EmbeddedSheetScreenContent(navigator, screen)
                }
            }
        }
    }
}
