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
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.verticalmode.DefaultPaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodIncentiveInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.ui.core.elements.FORM_ELEMENT_SET_DEFAULT_MATCHES_SAVE_FOR_FUTURE_DEFAULT_VALUE
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
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
    fun createInitialScreen(): EmbeddedNavigator.Screen.PaymentOptions {
        val interactor = createInteractor()
        return EmbeddedNavigator.Screen.PaymentOptions(
            interactor = interactor,
            isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            sheetActivityState = sheetActivityStateHolder.state,
            onContinueClick = {
                sheetActivityStateHolder.setResult(
                    EmbeddedActivityResult.Complete(
                        selection = selectionHolder.selection.value,
                        hasBeenConfirmed = false,
                        customerState = customerStateHolder.customer.value,
                        shouldInvokeSelectionCallback = false,
                        launchMode = EmbeddedLaunchMode.PaymentOptions,
                    )
                )
            },
        )
    }

    @Suppress("LongMethod")
    private fun createInteractor(): PaymentMethodVerticalLayoutInteractor {
        val formHelper = embeddedFormHelperFactory.create(
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
            isCurrentScreen = stateFlowOf(true),
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
            linkBrand = linkAccountHolder.linkAccountInfo.mapAsStateFlow {
                paymentMethodMetadata.effectiveLinkBrand(it.account)
            },
            paymentMethodMessagePromotionsHelper = paymentMethodMessagePromotionsHelper,
        )
    }

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
