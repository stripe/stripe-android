package com.stripe.android.paymentelement.embedded.sheet

import androidx.annotation.VisibleForTesting
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodOrientation
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.manage.EmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedUpdateScreenInteractorFactory
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Provider

internal class InitialPaymentOptionsScreenFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val eventReporter: EventReporter,
    private val embeddedNavigatorProvider: Provider<EmbeddedNavigator>,
    private val embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val manageInteractorFactory: EmbeddedManageScreenInteractorFactory,
    private val updateScreenInteractorFactory: EmbeddedUpdateScreenInteractorFactory,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
    private val formScreenFactory: EmbeddedFormScreenFactory,
    private val linkAccountHolder: LinkAccountHolder,
    private val addPaymentMethodInteractorFactory: EmbeddedAddPaymentMethodInteractorFactory,
    private val continueCoordinator: SheetActivityContinueCoordinator,
) {
    fun createInitialScreen(): List<EmbeddedNavigator.Screen> {
        return when (paymentMethodMetadata.paymentMethodOrientation()) {
            PaymentMethodOrientation.Vertical -> createVerticalInitialScreens()
            PaymentMethodOrientation.Horizontal -> listOf(createHorizontalScreen())
        }
    }

    private fun createVerticalInitialScreens(): List<EmbeddedNavigator.Screen> {
        val coroutineScope = viewModelScope.childScope(Dispatchers.Default)
        val formHelperScope = coroutineScope.childScope(Dispatchers.Main)
        val formHelper = createFormHelper(formHelperScope)
        val paymentOptionsScreen = EmbeddedNavigator.Screen.VerticalPaymentOptions(
            interactor = createInteractor(formHelper, coroutineScope),
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            sheetActivityState = sheetActivityStateHolder.state,
            onContinueClick = ::onContinueClick,
        )
        return buildList {
            add(paymentOptionsScreen)
            // When a new payment method requiring a form is already selected, open directly on that
            // form with the payment options list underneath, so back returns to the list.
            val selection = selectionHolder.selection.value
            if (selection is PaymentSelection.New &&
                formHelper.formTypeForCode(selection.paymentMethodType) == FormType.UserInteractionRequired
            ) {
                add(formScreenFactory.createFormScreen(selection.paymentMethodType))
            }
        }
    }

    private fun createHorizontalScreen(): EmbeddedNavigator.Screen {
        return EmbeddedNavigator.Screen.HorizontalPaymentOptions(
            interactor = addPaymentMethodInteractorFactory.create(),
            eventReporter = eventReporter,
            sheetActivityState = sheetActivityStateHolder.state,
            onContinueClick = ::onContinueClick,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun onContinueClick() {
        continueCoordinator.onContinue()
    }

    private fun createFormHelper(coroutineScope: CoroutineScope): FormHelper {
        return embeddedFormHelperFactory.createForVerticalLayout(
            coroutineScope = coroutineScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            selectionUpdater = { selectionHolder.setSelection(it) },
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
        )
    }

    @Suppress("LongMethod")
    private fun createInteractor(
        formHelper: FormHelper,
        coroutineScope: CoroutineScope,
    ): PaymentMethodVerticalLayoutInteractor {
        return DefaultPaymentMethodVerticalLayoutInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            processing = stateFlowOf(false),
            temporarySelection = stateFlowOf(null),
            selection = selectionHolder.selection,
            paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor(
                incentive = paymentMethodMetadata.paymentMethodIncentive,
            ),
            formTypeForCode = { code -> formHelper.formTypeForCode(code) },
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            transitionToManageScreen = ::navigateToManageScreen,
            transitionToFormScreen = { code ->
                val formScreen = formScreenFactory.createFormScreen(code)
                embeddedNavigatorProvider.get().performAction(EmbeddedNavigator.Action.GoToScreen(formScreen))
            },
            paymentMethods = customerStateHolder.paymentMethods,
            mostRecentlySelectedSavedPaymentMethod = customerStateHolder.mostRecentlySelectedSavedPaymentMethod,
            canRemove = customerStateHolder.canRemove,
            canUpdateCardExpiryAndBillingDetails = customerStateHolder.canUpdateCardExpiryAndBillingDetails,
            canChangeCbc = customerStateHolder.canChangeCbc,
            walletsState = stateFlowOf(walletsState()),
            updateSelection = { updatedSelection, _ ->
                selectionHolder.setSelection(updatedSelection)
            },
            isCurrentScreen = isCurrentScreen(),
            reportPaymentMethodTypeSelected = eventReporter::onSelectPaymentMethod,
            reportFormShown = eventReporter::onPaymentMethodFormShown,
            onUpdatePaymentMethod = { savedPaymentMethod ->
                val screen = EmbeddedNavigator.Screen.ManageUpdate(
                    interactor = updateScreenInteractorFactory.createUpdateScreenInteractor(
                        displayableSavedPaymentMethod = savedPaymentMethod
                    )
                )
                embeddedNavigatorProvider.get().performAction(EmbeddedNavigator.Action.GoToScreen(screen))
            },
            shouldUpdateVerticalModeSelection = { paymentMethodCode ->
                shouldUpdateSelection(formHelper, paymentMethodCode)
            },
            invokeRowSelectionCallback = null,
            displaysMandatesInFormScreen = false,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { visiblePaymentMethods, hiddenPaymentMethods ->
                eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                    visiblePaymentMethods = visiblePaymentMethods,
                    hiddenPaymentMethods = hiddenPaymentMethods,
                    walletsState = null,
                    isVerticalLayout = true,
                )
            },
            // Embedded renders mandate text through its own path, not the mandate-above-button handler.
            updateMandateText = null,
            coroutineScope = coroutineScope,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
            linkAccount = linkAccountHolder.linkAccountInfo,
        )
    }

    // The navigator is built from this initial screen (see EmbeddedActivityModule.provideEmbeddedNavigator), so
    // embeddedNavigatorProvider.get() can't be called synchronously here without recursing into the @Singleton
    // mid-construction. flow { } defers the get() until first collection, by which point the navigator exists.
    private fun isCurrentScreen(): StateFlow<Boolean> = flow {
        emitAll(embeddedNavigatorProvider.get().screen)
    }.map { screen ->
        screen is EmbeddedNavigator.Screen.VerticalPaymentOptions
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    private fun navigateToManageScreen() {
        val paymentMethods = customerStateHolder.customer.value?.paymentMethods
        val screen = if (paymentMethods?.size == 1) {
            val paymentMethod = paymentMethods.first()
            val displayName = paymentMethod.type?.code?.let { code ->
                paymentMethodMetadata.supportedPaymentMethodForCode(code)
            }?.displayName.orEmpty()
            val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
                displayName = displayName,
                paymentMethod = paymentMethod,
            )
            EmbeddedNavigator.Screen.ManageUpdate(
                interactor = updateScreenInteractorFactory.createUpdateScreenInteractor(
                    displayableSavedPaymentMethod = displayableSavedPaymentMethod
                )
            )
        } else {
            EmbeddedNavigator.Screen.ManageAll(
                interactor = manageInteractorFactory.createManageScreenInteractor()
            )
        }
        embeddedNavigatorProvider.get().performAction(EmbeddedNavigator.Action.GoToScreen(screen))
    }

    private fun shouldUpdateSelection(formHelper: FormHelper, paymentMethodCode: String?): Boolean {
        // Don't fold a selection that requires a form into the vertical list's remembered selection.
        // The form writes its in-progress selection to the shared holder, so tracking it here would
        // pollute the list's selection and defeat the restore-on-return behavior that reasserts the
        // list's selection when it becomes the current screen again (backing out of the form).
        val requiresFormScreen = paymentMethodCode != null &&
            formHelper.formTypeForCode(paymentMethodCode) == FormType.UserInteractionRequired
        return !requiresFormScreen
    }

    private fun walletsState(): WalletsState? {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
        return WalletsState.create(
            isLinkAvailable = paymentMethodMetadata.shouldShowLinkButton,
            linkEmail = null,
            isGooglePayReady = paymentMethodMetadata.isGooglePayReady,
            buttonsEnabled = true,
            paymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes(),
            googlePayLauncherConfig = null,
            googlePayButtonType = GooglePayButtonType.Pay,
            onGooglePayPressed = { throw IllegalStateException("Not possible.") },
            onLinkPressed = { throw IllegalStateException("Not possible.") },
            isSetupIntent = paymentMethodMetadata.stripeIntent is SetupIntent,
            walletsAllowedInHeader = emptyList(),
            cardBrandFilter = paymentMethodMetadata.cardBrandFilter,
            cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
            linkBrand = paymentMethodMetadata.effectiveLinkBrand(linkAccount),
        )
    }
}
