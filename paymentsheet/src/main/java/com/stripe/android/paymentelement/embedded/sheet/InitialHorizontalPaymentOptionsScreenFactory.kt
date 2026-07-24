package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.manage.EmbeddedUpdateScreenInteractorFactory
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher.Companion.HOSTED_SURFACE_PAYMENT_ELEMENT
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Provider

internal class InitialHorizontalPaymentOptionsScreenFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val eventReporter: EventReporter,
    private val embeddedNavigatorProvider: Provider<EmbeddedNavigator>,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val sheetActivityStateHolder: SheetActivityStateHolder,
    private val updateScreenInteractorFactory: EmbeddedUpdateScreenInteractorFactory,
    private val linkAccountHolder: LinkAccountHolder,
    private val savedPaymentMethodMutator: SavedPaymentMethodMutator,
    private val embeddedFormHelperFactory: EmbeddedFormHelperFactory,
    private val paymentMethodMessagePromotionsHelper: PaymentMethodMessagePromotionsHelper,
    private val confirmationHelper: SheetActivityConfirmationHelper,
) {
    /**
     * When there are saved payment methods we show the horizontal tab layout of those methods (with an "add" button
     * that navigates to the add screen). Otherwise we open directly into the horizontal tabs + form add screen.
     */
    fun createInitialScreen(): EmbeddedNavigator.Screen {
        return if (customerStateHolder.paymentMethods.value.isEmpty()) {
            createAddPaymentMethodScreen(
                initiallySelectedPaymentMethodCode = initiallySelectedPaymentMethodCode(),
                onContinueClick = ::completePaymentOptions,
                onProcessingCompleted = {},
            )
        } else {
            createSelectSavedPaymentMethodsScreen()
        }
    }

    /**
     * Changing form details in the horizontal layout opens the horizontal tabs + form add screen with the tapped
     * payment method selected. Completion mirrors the vertical form screen: [confirmationHelper] handles both the
     * Continue and Confirm form sheet actions, so the returned result carries the [launchMode].
     */
    fun createInitialScreen(launchMode: EmbeddedLaunchMode.Form): EmbeddedNavigator.Screen.AddPaymentMethod {
        return createAddPaymentMethodScreen(
            initiallySelectedPaymentMethodCode = launchMode.selectedPaymentMethodCode,
            onContinueClick = confirmationHelper::confirm,
            onProcessingCompleted = { completeConfirmedForm(launchMode) },
        )
    }

    private fun createSelectSavedPaymentMethodsScreen(): EmbeddedNavigator.Screen.HorizontalPaymentOptions {
        val linkAccount = linkAccountHolder.linkAccountInfo.value.account
        val interactor = DefaultSelectSavedPaymentMethodsInteractor(
            paymentOptionsItems = savedPaymentMethodMutator.paymentOptionsItems,
            editing = savedPaymentMethodMutator.editing,
            canEdit = savedPaymentMethodMutator.canEdit,
            canRemove = customerStateHolder.canRemove,
            toggleEdit = savedPaymentMethodMutator::toggleEditing,
            isProcessing = stateFlowOf(false),
            isCurrentScreen = stateFlowOf(true),
            currentSelection = selectionHolder.selection,
            mostRecentlySelectedSavedPaymentMethod = customerStateHolder.mostRecentlySelectedSavedPaymentMethod,
            onAddCardPressed = {
                embeddedNavigatorProvider.get().performAction(
                    EmbeddedNavigator.Action.GoToScreen(
                        createAddPaymentMethodScreen(
                            initiallySelectedPaymentMethodCode = initiallySelectedPaymentMethodCode(),
                            onContinueClick = ::completePaymentOptions,
                            onProcessingCompleted = {},
                        )
                    )
                )
            },
            onUpdatePaymentMethod = { savedPaymentMethod ->
                navigateToUpdateScreen(savedPaymentMethod)
            },
            updateSelection = { selection, _ ->
                selectionHolder.setSelection(selection)
            },
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            linkBrand = paymentMethodMetadata.effectiveLinkBrand(linkAccount),
        )
        return EmbeddedNavigator.Screen.HorizontalPaymentOptions(
            interactor = interactor,
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            sheetActivityState = sheetActivityStateHolder.state,
            onContinueClick = ::completePaymentOptions,
        )
    }

    private fun createAddPaymentMethodScreen(
        initiallySelectedPaymentMethodCode: PaymentMethodCode,
        onContinueClick: () -> Unit,
        onProcessingCompleted: () -> Unit,
    ): EmbeddedNavigator.Screen.AddPaymentMethod {
        return EmbeddedNavigator.Screen.AddPaymentMethod(
            interactor = createAddPaymentMethodInteractor(initiallySelectedPaymentMethodCode),
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            eventReporter = eventReporter,
            sheetActivityState = sheetActivityStateHolder.state,
            onContinueClick = onContinueClick,
            onProcessingCompleted = onProcessingCompleted,
        )
    }

    private fun createAddPaymentMethodInteractor(
        initiallySelectedPaymentMethodCode: PaymentMethodCode,
    ): AddPaymentMethodInteractor {
        val hasSavedPaymentMethods = customerStateHolder.paymentMethods.value.isNotEmpty()
        val formHelper = embeddedFormHelperFactory.create(
            coroutineScope = viewModelScope,
            paymentMethodMetadata = paymentMethodMetadata,
            eventReporter = eventReporter,
            // Card scan auto-launch is only relevant in the vertical form screen, not the horizontal add screen.
            automaticallyLaunchedCardScanFormDataHelper = null,
            tapToAddHelper = null,
            selectionUpdater = { selectionHolder.setSelection(it) },
            // If no saved payment methods, then first saved payment method is automatically set as default.
            setAsDefaultMatchesSaveForFutureUse = !hasSavedPaymentMethods,
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
        )
        return DefaultAddPaymentMethodInteractor(
            initiallySelectedPaymentMethodType = initiallySelectedPaymentMethodCode,
            selection = selectionHolder.selection,
            processing = sheetActivityStateHolder.state.mapAsStateFlow { it.isProcessing },
            // Embedded does not support validation at the moment. Should update here once it does.
            validationRequested = MutableSharedFlow(),
            incentive = PaymentMethodIncentiveInteractor(
                paymentMethodMetadata.paymentMethodIncentive
            ).displayedIncentive,
            supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods(),
            createFormArguments = formHelper::createFormArguments,
            formElementsForCode = formHelper::formElementsForCode,
            clearErrorMessages = { sheetActivityStateHolder.updateError(null) },
            reportFieldInteraction = eventReporter::onPaymentMethodFormInteraction,
            onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
            reportPaymentMethodTypeSelected = eventReporter::onSelectPaymentMethod,
            reportPromotionDisplayed = { code ->
                paymentMethodMessagePromotionsHelper.reportPromotionDisplayed(code, paymentMethodMetadata)
            },
            createUSBankAccountFormArguments = { code ->
                createUsBankAccountFormArguments(code, hasSavedPaymentMethods)
            },
            coroutineScope = viewModelScope,
            uiContext = Dispatchers.Main,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { visiblePaymentMethods, hiddenPaymentMethods ->
                eventReporter.onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
                    visiblePaymentMethods = visiblePaymentMethods,
                    hiddenPaymentMethods = hiddenPaymentMethods,
                    walletsState = null,
                    isVerticalLayout = false,
                )
            },
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
        )
    }

    private fun createUsBankAccountFormArguments(
        paymentMethodCode: PaymentMethodCode,
        hasSavedPaymentMethods: Boolean,
    ): USBankAccountFormArguments {
        return USBankAccountFormArguments.createForEmbedded(
            paymentMethodMetadata = paymentMethodMetadata,
            selectedPaymentMethodCode = paymentMethodCode,
            hostedSurface = HOSTED_SURFACE_PAYMENT_ELEMENT,
            setSelection = selectionHolder::setSelection,
            hasSavedPaymentMethods = hasSavedPaymentMethods,
            onAnalyticsEvent = eventReporter::onUsBankAccountFormEvent,
            onMandateTextChanged = { mandateText, _ ->
                sheetActivityStateHolder.updateMandate(mandateText)
            },
            onUpdatePrimaryButtonUIState = sheetActivityStateHolder::updatePrimaryButton,
            onError = sheetActivityStateHolder::updateError,
            onFormCompleted = {
                eventReporter.onPaymentMethodFormCompleted(PaymentMethod.Type.USBankAccount.code)
            },
        )
    }

    private fun initiallySelectedPaymentMethodCode(): PaymentMethodCode {
        return when (val selection = selectionHolder.selection.value) {
            is PaymentSelection.New -> selection.paymentMethodCreateParams.typeCode
            is PaymentSelection.ExternalPaymentMethod -> selection.type
            is PaymentSelection.CustomPaymentMethod -> selection.id
            else -> paymentMethodMetadata.supportedPaymentMethodTypes().first()
        }
    }

    private fun completePaymentOptions() {
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
    }

    private fun completeConfirmedForm(launchMode: EmbeddedLaunchMode.Form) {
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

    private fun navigateToUpdateScreen(savedPaymentMethod: DisplayableSavedPaymentMethod) {
        val screen = EmbeddedNavigator.Screen.ManageUpdate(
            interactor = updateScreenInteractorFactory.createUpdateScreenInteractor(
                displayableSavedPaymentMethod = savedPaymentMethod
            )
        )
        embeddedNavigatorProvider.get().performAction(EmbeddedNavigator.Action.GoToScreen(screen))
    }
}
