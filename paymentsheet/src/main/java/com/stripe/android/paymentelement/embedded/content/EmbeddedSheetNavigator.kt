package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.form.FormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.FormResult
import com.stripe.android.paymentelement.embedded.manage.InitialManageScreenFactory
import com.stripe.android.paymentelement.embedded.manage.ManageResult
import com.stripe.android.paymentelement.embedded.manage.ManageSavedPaymentMethodMutatorFactory
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.SavedPaymentMethodConfirmInteractor
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedSheetNavigator @Inject constructor(
    private val args: EmbeddedSheetArgs,
    private val eventReporter: EventReporter,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val initialManageScreenFactory: Lazy<InitialManageScreenFactory>,
    private val manageSavedPaymentMethodMutatorFactory: Lazy<ManageSavedPaymentMethodMutatorFactory>,
    private val formInteractor: Lazy<DefaultVerticalModeFormInteractor>,
    private val formStateHelper: Lazy<FormActivityStateHelper>,
    private val confirmInteractorFactory: Lazy<SavedPaymentMethodConfirmInteractor.Factory>,
    private val subcomponentFactory: EmbeddedSheetSubcomponent.Factory,
) {

    private var navigationHandler: NavigationHandler<EmbeddedSheetScreen>? = null
    private var savedPaymentMethodMutator: SavedPaymentMethodMutator? = null

    private val _result = MutableSharedFlow<EmbeddedSheetResult>(replay = 1)
    val result: SharedFlow<EmbeddedSheetResult> = _result.asSharedFlow()

    lateinit var currentScreen: StateFlow<EmbeddedSheetScreen>
        private set

    val canGoBack: Boolean
        get() = navigationHandler?.canGoBack ?: false

    init {
        when (args) {
            is EmbeddedSheetArgs.Manage -> initManageMode()
            is EmbeddedSheetArgs.Form -> Unit // Deferred to registerActivityDeps
        }
    }

    private fun initManageMode() {
        savedPaymentMethodMutator = manageSavedPaymentMethodMutatorFactory.get()
            .createSavedPaymentMethodMutator(
                navigateBack = ::navigateBack,
                close = ::close,
                navigateToUpdate = ::navigateToUpdate,
            )
        val initialScreen = initialManageScreenFactory.get().createInitialScreen(
            savedPaymentMethodMutator = requireNotNull(savedPaymentMethodMutator),
            close = ::close,
            navigateBack = ::navigateBack,
            canGoBack = { canGoBack },
        )
        onManageScreenShown(initialScreen)
        val handler = NavigationHandler(
            coroutineScope = coroutineScope,
            initialScreen = initialScreen,
            shouldRemoveInitialScreenOnTransition = false,
            poppedScreenHandler = ::onScreenPopped,
        )
        navigationHandler = handler
        currentScreen = handler.currentScreen
    }

    fun registerActivityDeps(caller: ActivityResultCaller, owner: LifecycleOwner) {
        if (args is EmbeddedSheetArgs.Form) {
            val subcomponent = subcomponentFactory.build(caller, owner)
            val stateHelper = formStateHelper.get()

            val screen = EmbeddedSheetScreen.Form(
                interactor = formInteractor.get(),
                stateHelper = stateHelper,
                confirmationHelper = subcomponent.confirmationHelper,
                eventReporter = eventReporter,
                selectionHolder = selectionHolder,
                confirmInteractorFactory = confirmInteractorFactory.get(),
                onProcessingCompleted = ::onFormProcessingCompleted,
                onDismissed = ::onFormDismissed,
            )
            currentScreen = MutableStateFlow(screen)

            coroutineScope.launch {
                stateHelper.result.collect { formResult ->
                    _result.tryEmit(EmbeddedSheetResult.Form(formResult))
                }
            }
        }
    }

    fun handleBack() {
        if (args is EmbeddedSheetArgs.Manage) {
            val screen = requireNotNull(navigationHandler).currentScreen.value
            if (screen.canDismiss()) {
                navigateBack()
            }
        }
    }

    fun onDismissed() {
        when (args) {
            is EmbeddedSheetArgs.Form -> onFormDismissed()
            is EmbeddedSheetArgs.Manage -> emitManageResult(shouldInvokeSelectionCallback = false)
        }
    }

    fun onFinishing() {
        val metadata = when (args) {
            is EmbeddedSheetArgs.Form -> args.formArgs.paymentMethodMetadata
            is EmbeddedSheetArgs.Manage -> args.manageArgs.paymentMethodMetadata
        }
        CheckoutInstances.markIntegrationDismissed(metadata)
        eventReporter.onDismiss()
    }

    fun closeScreens() {
        navigationHandler?.closeScreens()
    }

    // --- Manage mode internals ---

    private fun navigateBack() {
        val handler = requireNotNull(navigationHandler)
        val screen = handler.currentScreen.value
        onManageScreenHidden(screen)
        if (handler.canGoBack) {
            handler.pop()
        } else {
            emitManageResult(shouldInvokeSelectionCallback = false)
        }
    }

    private fun close(shouldInvokeRowSelectionCallback: Boolean) {
        val handler = requireNotNull(navigationHandler)
        val screen = handler.currentScreen.value
        onManageScreenHidden(screen)
        emitManageResult(shouldInvokeSelectionCallback = shouldInvokeRowSelectionCallback)
    }

    private fun navigateToUpdate(interactor: UpdatePaymentMethodInteractor) {
        val screen = EmbeddedSheetScreen.ManageUpdate(
            interactor = interactor,
            canGoBack = { canGoBack },
            onBack = ::navigateBack,
        )
        requireNotNull(navigationHandler).transitionToWithDelay(screen)
        eventReporter.onShowEditablePaymentOption()
    }

    @Suppress("UnusedParameter")
    private fun onScreenPopped(screen: EmbeddedSheetScreen) {
        // Closeable screens (ManageAll) will be closed by NavigationHandler
    }

    private fun onManageScreenShown(screen: EmbeddedSheetScreen) {
        when (screen) {
            is EmbeddedSheetScreen.ManageAll -> eventReporter.onShowManageSavedPaymentMethods()
            is EmbeddedSheetScreen.ManageUpdate -> eventReporter.onShowEditablePaymentOption()
            is EmbeddedSheetScreen.Form -> Unit
        }
    }

    private fun onManageScreenHidden(screen: EmbeddedSheetScreen) {
        when (screen) {
            is EmbeddedSheetScreen.ManageAll -> Unit
            is EmbeddedSheetScreen.ManageUpdate -> eventReporter.onHideEditablePaymentOption()
            is EmbeddedSheetScreen.Form -> Unit
        }
    }

    private fun emitManageResult(shouldInvokeSelectionCallback: Boolean) {
        _result.tryEmit(
            EmbeddedSheetResult.Manage(
                ManageResult.Complete(
                    customerState = requireNotNull(customerStateHolder.customer.value),
                    selection = selectionHolder.selection.value,
                    shouldInvokeSelectionCallback = shouldInvokeSelectionCallback,
                )
            )
        )
    }

    // --- Form mode internals ---

    private fun onFormProcessingCompleted() {
        _result.tryEmit(
            EmbeddedSheetResult.Form(
                FormResult.Complete(
                    selection = null,
                    hasBeenConfirmed = true,
                    customerState = customerStateHolder.customer.value,
                )
            )
        )
    }

    private fun onFormDismissed() {
        _result.tryEmit(
            EmbeddedSheetResult.Form(
                FormResult.Cancelled(
                    customerState = customerStateHolder.customer.value,
                )
            )
        )
    }
}
