package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
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
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
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
    private val configuration: EmbeddedPaymentElement.Configuration,
    private val manageInteractorFactory: EmbeddedManageScreenInteractorFactory,
    private val updateScreenInteractorFactory: EmbeddedUpdateScreenInteractorFactory,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
    private val formScreenFactory: EmbeddedFormScreenFactory,
    private val linkAccountHolder: LinkAccountHolder,
) {
    fun createInitialScreen(): List<EmbeddedNavigator.Screen> {
        val formHelper = createFormHelper()
        val paymentOptionsScreen = EmbeddedNavigator.Screen.PaymentOptions(
            interactor = createInteractor(formHelper),
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            sheetActivityState = sheetActivityStateHolder.state,
            onContinueClick = {
                sheetActivityStateHolder.setResult(
                    EmbeddedActivityResult.Complete(
                        selection = selectionHolder.selection.value,
                        previousNewSelections = selectionHolder.previousNewSelections,
                        hasBeenConfirmed = false,
                        customerState = customerStateHolder.customer.value,
                        shouldInvokeSelectionCallback = false,
                        launchMode = EmbeddedLaunchMode.PaymentOptions,
                    )
                )
            },
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

    private fun createFormHelper(): FormHelper {
        return embeddedFormHelperFactory.create(
            coroutineScope = viewModelScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            // Card scan auto-launch is only relevant in the form, not the list (as the form helper is used here).
            automaticallyLaunchedCardScanFormDataHelper = null,
            tapToAddHelper = null,
            selectionUpdater = { selectionHolder.setSelection(it) },
            setAsDefaultMatchesSaveForFutureUse = FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
        )
    }

    @Suppress("LongMethod")
    private fun createInteractor(formHelper: FormHelper): PaymentMethodVerticalLayoutInteractor {
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
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
        )
    }

    // The navigator is built from this initial screen (see EmbeddedActivityModule.provideEmbeddedNavigator), so
    // embeddedNavigatorProvider.get() can't be called synchronously here without recursing into the @Singleton
    // mid-construction. flow { } defers the get() until first collection, by which point the navigator exists.
    private fun isCurrentScreen(): StateFlow<Boolean> = flow {
        emitAll(embeddedNavigatorProvider.get().screen)
    }.map { screen ->
        screen is EmbeddedNavigator.Screen.PaymentOptions
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
        val isConfirmFlow = configuration.formSheetAction ==
            EmbeddedPaymentElement.FormSheetAction.Confirm
        if (isConfirmFlow) {
            val requiresFormScreen = paymentMethodCode != null &&
                formHelper.formTypeForCode(paymentMethodCode) == FormType.UserInteractionRequired
            return !requiresFormScreen
        }
        return true
    }

    private fun walletsState(): WalletsState? {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
        return WalletsState.create(
            isLinkAvailable = paymentMethodMetadata.linkState != null,
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
