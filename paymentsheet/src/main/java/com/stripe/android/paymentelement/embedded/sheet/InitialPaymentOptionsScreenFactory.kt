package com.stripe.android.paymentelement.embedded.sheet

import androidx.annotation.VisibleForTesting
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodOrientation
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.NullUiDefinitionFactoryHelper
import com.stripe.android.paymentelement.embedded.manage.EmbeddedManageScreenInteractorFactory
import com.stripe.android.paymentelement.embedded.manage.EmbeddedUpdateScreenInteractorFactory
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.utils.asGooglePayButtonType
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.ui.core.elements.CvcConfig
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.mapAsStateFlow
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
    private val configuration: EmbeddedPaymentElement.Configuration,
    private val confirmationHelper: SheetActivityConfirmationHelper,
    private val launchMode: EmbeddedLaunchMode,
    private val savedPaymentMethodMutator: SavedPaymentMethodMutator,
    private val cvcRecollectionHandler: CvcRecollectionHandler,
) {
    private val cvcController: CvcController? by lazy {
        if (launchMode is EmbeddedLaunchMode.Complete &&
            cvcRecollectionHandler.cvcRecollectionEnabled(paymentMethodMetadata.stripeIntent)
        ) {
            CvcController(
                cvcTextFieldConfig = CvcConfig(),
                cardBrandFlow = selectionHolder.selection.mapAsStateFlow { selection ->
                    (selection as? PaymentSelection.Saved)?.paymentMethod?.card?.brand ?: CardBrand.Unknown
                },
            )
        } else {
            null
        }
    }

    fun createInitialScreen(): List<EmbeddedNavigator.Screen> {
        return when (paymentMethodMetadata.paymentMethodOrientation()) {
            PaymentMethodOrientation.Vertical -> createVerticalInitialScreens()
            PaymentMethodOrientation.Horizontal -> listOf(createHorizontalInitialScreen())
        }
    }

    private fun createVerticalInitialScreens(): List<EmbeddedNavigator.Screen> {
        val supportedPaymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes()
        val isLegacySheet = launchMode is EmbeddedLaunchMode.PaymentOptions ||
            launchMode is EmbeddedLaunchMode.Complete
        if (isLegacySheet &&
            supportedPaymentMethodTypes.size == 1 &&
            customerStateHolder.paymentMethods.value.isEmpty()
        ) {
            val paymentMethodType = supportedPaymentMethodTypes.single()
            paymentMethodMessagePromotionsHelper.reportPromotionDisplayed(
                paymentMethodType,
                paymentMethodMetadata,
            )
            eventReporter.onPaymentMethodFormShown(paymentMethodType)
            return listOf(formScreenFactory.createFormScreen(paymentMethodType, launchMode))
        }

        val coroutineScope = viewModelScope.childScope(Dispatchers.Default)
        val selectionHelper = createFormHelper(coroutineScope.childScope(Dispatchers.Main))
        val paymentOptionsScreen = EmbeddedNavigator.Screen.VerticalPaymentOptions(
            interactor = createInteractor(
                selectionHelper = selectionHelper,
                coroutineScope = coroutineScope,
            ),
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            sheetActivityState = sheetActivityStateHolder.state,
            onContinueClick = ::onPrimaryButtonClick,
            onDisabledClick = sheetActivityStateHolder::requestValidation,
            onProcessingCompleted = ::onProcessingCompleted,
        )
        return buildList {
            add(paymentOptionsScreen)
            // When a new payment method requiring a form is already selected, open directly on that
            // form with the payment options list underneath, so back returns to the list.
            val selection = selectionHolder.selection.value
            if (selection is PaymentSelection.New &&
                formTypeForCode(selection.paymentMethodType) == FormType.UserInteractionRequired
            ) {
                add(formScreenFactory.createFormScreen(selection.paymentMethodType, launchMode))
            }
        }
    }

    private fun createHorizontalInitialScreen(): EmbeddedNavigator.Screen {
        val shouldShowSavedPaymentOptions = customerStateHolder.paymentMethods.value.isNotEmpty() ||
            (launchMode is EmbeddedLaunchMode.PaymentOptions && paymentMethodMetadata.isGooglePayReady)
        return if (!shouldShowSavedPaymentOptions) {
            val initialPaymentMethodType = paymentMethodMetadata.supportedPaymentMethodTypes().first()
            paymentMethodMessagePromotionsHelper.reportPromotionDisplayed(
                initialPaymentMethodType,
                paymentMethodMetadata,
            )
            eventReporter.onPaymentMethodFormShown(initialPaymentMethodType)
            createHorizontalFormScreen()
        } else {
            EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions(
                mutator = savedPaymentMethodMutator,
                selection = selectionHolder.selection,
                cvcControllerFlow = cvcController?.let(::stateFlowOf),
                sheetActivityState = sheetActivityStateHolder.state,
                isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
                onAddCardPressed = {
                    embeddedNavigatorProvider.get().performAction(
                        EmbeddedNavigator.Action.GoToScreen(createHorizontalFormScreen())
                    )
                },
                onItemSelected = { selection ->
                    selectionHolder.setSelection(selection)
                    if (selection != null) {
                        eventReporter.onSelectPaymentOption(selection)
                        if (launchMode is EmbeddedLaunchMode.PaymentOptions) {
                            onContinueClick()
                        }
                    }
                },
                onContinueClick = ::onHorizontalSavedContinueClick,
                onDisabledClick = sheetActivityStateHolder::requestValidation,
                onProcessingCompleted = ::onProcessingCompleted,
            )
        }
    }

    private fun createHorizontalFormScreen(): EmbeddedNavigator.Screen {
        return EmbeddedNavigator.Screen.HorizontalPaymentOptions(
            interactor = addPaymentMethodInteractorFactory.create(
                walletsState = if (launchMode is EmbeddedLaunchMode.Complete) walletsState() else null,
            ),
            eventReporter = eventReporter,
            sheetActivityState = sheetActivityStateHolder.state,
            onContinueClick = ::onPrimaryButtonClick,
            onDisabledClick = sheetActivityStateHolder::requestValidation,
            onProcessingCompleted = ::onProcessingCompleted,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun onContinueClick() {
        when (configuration.formSheetAction) {
            EmbeddedPaymentElement.FormSheetAction.Continue -> {
                continueCoordinator.onContinue()
            }
            EmbeddedPaymentElement.FormSheetAction.Confirm -> confirmationHelper.confirm()
        }
    }

    private fun onPrimaryButtonClick() {
        confirmationHelper.confirm()
    }

    private fun onHorizontalSavedContinueClick() {
        val selection = selectionHolder.selection.value
        val controller = cvcController
        if (controller != null && selection is PaymentSelection.Saved &&
            selection.paymentMethod.type == PaymentMethod.Type.Card
        ) {
            val cardOptions = selection.paymentMethodOptionsParams as? PaymentMethodOptionsParams.Card
                ?: PaymentMethodOptionsParams.Card()
            selectionHolder.setSelection(
                selection.copy(
                    paymentMethodOptionsParams = cardOptions.copy(cvc = controller.fieldValue.value),
                )
            )
        }
        onPrimaryButtonClick()
    }

    private fun onProcessingCompleted() {
        sheetActivityStateHolder.setResult(
            EmbeddedActivityResult.Complete(
                selection = null,
                previousNewSelections = selectionHolder.previousNewSelections,
                hasBeenConfirmed = true,
                customerState = customerStateHolder.customer.value,
                shouldInvokeSelectionCallback = false,
                launchMode = launchMode,
            )
        )
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
        selectionHelper: FormHelper,
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
            formTypeForCode = ::formTypeForCode,
            onFormFieldValuesChanged = selectionHelper::onFormFieldValuesChanged,
            transitionToManageScreen = ::navigateToManageScreen,
            transitionToFormScreen = { code ->
                sheetActivityStateHolder.updateMandate(null)
                val formScreen = formScreenFactory.createFormScreen(code, launchMode)
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
                shouldUpdateSelection(paymentMethodCode)
            },
            invokeRowSelectionCallback = if (launchMode is EmbeddedLaunchMode.PaymentOptions) {
                ::onContinueClick
            } else {
                null
            },
            displaysMandatesInFormScreen = false,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { visiblePaymentMethods, hiddenPaymentMethods ->
                eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                    visiblePaymentMethods = visiblePaymentMethods,
                    hiddenPaymentMethods = hiddenPaymentMethods,
                    walletsState = walletsState(),
                    isVerticalLayout = true,
                )
            },
            updateMandateText = if (launchMode is EmbeddedLaunchMode.Form ||
                launchMode is EmbeddedLaunchMode.Manage
            ) {
                null
            } else {
                { mandateText, _ -> sheetActivityStateHolder.updateMandate(mandateText) }
            },
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

    private fun shouldUpdateSelection(paymentMethodCode: String?): Boolean {
        // Don't fold a selection that requires a form into the vertical list's remembered selection.
        // The form writes its in-progress selection to the shared holder, so tracking it here would
        // pollute the list's selection and defeat the restore-on-return behavior that reasserts the
        // list's selection when it becomes the current screen again (backing out of the form).
        val requiresFormScreen = paymentMethodCode != null &&
            formTypeForCode(paymentMethodCode) == FormType.UserInteractionRequired
        return !requiresFormScreen
    }

    private fun formTypeForCode(paymentMethodCode: String): FormType {
        val formElements = paymentMethodMetadata.formElementsForCode(
            code = paymentMethodCode,
            uiDefinitionFactoryArgumentsFactory = NullUiDefinitionFactoryHelper.nullEmbeddedUiDefinitionFactory,
        ).orEmpty()
        val requiresFormScreen = formElements.any { it.allowsUserInteraction } ||
            paymentMethodCode == PaymentMethod.Type.USBankAccount.code ||
            paymentMethodCode == PaymentMethod.Type.Link.code
        return if (requiresFormScreen) {
            FormType.UserInteractionRequired
        } else {
            formElements.firstNotNullOfOrNull { it.mandateText }
                ?.let(FormType::MandateOnly)
                ?: FormType.Empty
        }
    }

    internal fun walletsState(): WalletsState? {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
        return WalletsState.create(
            isLinkAvailable = paymentMethodMetadata.shouldShowLinkButton,
            linkEmail = null,
            isGooglePayReady = paymentMethodMetadata.isGooglePayReady,
            buttonsEnabled = true,
            paymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes(),
            googlePayLauncherConfig = null,
            googlePayButtonType = configuration.googlePay?.buttonType.asGooglePayButtonType,
            onGooglePayPressed = {
                onWalletPressed(PaymentSelection.GooglePay)
            },
            onLinkPressed = {
                onWalletPressed(PaymentSelection.Link(paymentMethodMetadata.effectiveLinkBrand(linkAccount)))
            },
            isSetupIntent = paymentMethodMetadata.stripeIntent is SetupIntent,
            walletsAllowedInHeader = walletsAllowedInHeader(),
            cardBrandFilter = paymentMethodMetadata.cardBrandFilter,
            cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
            linkBrand = paymentMethodMetadata.effectiveLinkBrand(linkAccount),
        )
    }

    private fun walletsAllowedInHeader(): List<WalletType> {
        return when (launchMode) {
            is EmbeddedLaunchMode.Complete -> WalletType.entries
            is EmbeddedLaunchMode.PaymentOptions -> {
                val showsDirectForm = paymentMethodMetadata.supportedPaymentMethodTypes().size == 1 &&
                    customerStateHolder.paymentMethods.value.isEmpty()
                if (showsDirectForm) WalletType.entries else listOf(WalletType.Link)
            }
            is EmbeddedLaunchMode.Form,
            is EmbeddedLaunchMode.Manage -> emptyList()
        }
    }

    private fun onWalletPressed(selection: PaymentSelection) {
        selectionHolder.setSelection(selection)
        when (launchMode) {
            is EmbeddedLaunchMode.Complete -> confirmationHelper.confirm()
            is EmbeddedLaunchMode.PaymentOptions -> onContinueClick()
            is EmbeddedLaunchMode.Form,
            is EmbeddedLaunchMode.Manage -> Unit
        }
    }
}
