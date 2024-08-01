package com.stripe.android.paymentsheet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionContract
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionScreen
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionViewModel
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.EditPaymentMethod
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodTabLayoutUI
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodsTopContentPadding
import com.stripe.android.paymentsheet.ui.SelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.verticalmode.CvcRecollectionInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageOneSavedPaymentMethodInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageOneSavedPaymentMethodUI
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutUI
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import java.io.Closeable
import com.stripe.android.R as PaymentsCoreR

internal val PaymentSheetScreen.topContentPadding: Dp
    get() = when (this) {
        is PaymentSheetScreen.SelectSavedPaymentMethods -> {
            SavedPaymentMethodsTopContentPadding
        }
        is PaymentSheetScreen.Loading,
        is PaymentSheetScreen.VerticalMode,
        is PaymentSheetScreen.VerticalModeForm,
        is PaymentSheetScreen.AddFirstPaymentMethod,
        is PaymentSheetScreen.AddAnotherPaymentMethod,
        is PaymentSheetScreen.ManageSavedPaymentMethods,
        is PaymentSheetScreen.ManageOneSavedPaymentMethod,
        is PaymentSheetScreen.EditPaymentMethod,
        is PaymentSheetScreen.CvcRecollection -> {
            0.dp
        }
    }

internal sealed interface PaymentSheetScreen {

    val showsBuyButton: Boolean
    val showsContinueButton: Boolean

    fun topBarState(): StateFlow<PaymentSheetTopBarState?>

    fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?>

    fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean>

    @Composable
    fun Content(modifier: Modifier)

    object Loading : PaymentSheetScreen {

        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(null)
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(null)
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(false)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            BottomSheetLoadingIndicator(modifier)
        }
    }

    class SelectSavedPaymentMethods(
        private val interactor: SelectSavedPaymentMethodsInteractor,
        val cvcRecollectionState: CvcRecollectionState = CvcRecollectionState.NotRequired,
    ) : PaymentSheetScreen, Closeable {

        sealed interface CvcRecollectionState {
            data object NotRequired : CvcRecollectionState
            class Required(val cvcControllerFlow: StateFlow<CvcController>) : CvcRecollectionState
        }

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return interactor.state.mapAsStateFlow { state ->
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = false,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Maybe(
                        isEditing = state.isEditing,
                        canEdit = state.canEdit,
                        onEditIconPressed = {
                            interactor.handleViewAction(SelectSavedPaymentMethodsInteractor.ViewAction.ToggleEdit)
                        },
                    ),
                )
            }
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(
                if (isCompleteFlow && isWalletEnabled) {
                    null
                } else {
                    R.string.stripe_paymentsheet_select_payment_method.resolvableString
                }
            )
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(isCompleteFlow)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            SavedPaymentMethodTabLayoutUI(
                interactor = interactor,
                cvcRecollectionState = cvcRecollectionState,
                modifier = modifier,
            )
        }

        override fun close() {
            interactor.close()
        }
    }

    class AddAnotherPaymentMethod(
        private val interactor: AddPaymentMethodInteractor,
    ) : PaymentSheetScreen, Closeable {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = true,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return interactor.state.mapAsStateFlow { state ->
                if (isWalletEnabled || isCompleteFlow) {
                    null
                } else {
                    if (state.supportedPaymentMethods.singleOrNull()?.code == PaymentMethod.Type.Card.code) {
                        PaymentsCoreR.string.stripe_title_add_a_card.resolvableString
                    } else {
                        R.string.stripe_paymentsheet_choose_payment_method.resolvableString
                    }
                }
            }
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(isCompleteFlow)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            AddPaymentMethod(interactor = interactor, modifier)
        }

        override fun close() {
            interactor.close()
        }
    }

    class AddFirstPaymentMethod(
        private val interactor: AddPaymentMethodInteractor,
    ) : PaymentSheetScreen, Closeable {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = false,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return interactor.state.mapAsStateFlow { state ->
                if (isWalletEnabled) {
                    null
                } else if (isCompleteFlow) {
                    R.string.stripe_paymentsheet_add_payment_method_title.resolvableString
                } else {
                    if (state.supportedPaymentMethods.singleOrNull()?.code == PaymentMethod.Type.Card.code) {
                        PaymentsCoreR.string.stripe_title_add_a_card.resolvableString
                    } else {
                        R.string.stripe_paymentsheet_choose_payment_method.resolvableString
                    }
                }
            }
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(true)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            AddPaymentMethod(interactor = interactor, modifier)
        }

        override fun close() {
            interactor.close()
        }
    }

    class EditPaymentMethod(
        val interactor: ModifiableEditPaymentMethodViewInteractor,
        private val isLiveMode: Boolean,
    ) : PaymentSheetScreen, Closeable {

        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = true,
                    isLiveMode = isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(PaymentsCoreR.string.stripe_title_update_card.resolvableString)
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(false)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            EditPaymentMethod(interactor, modifier)
        }

        override fun close() {
            interactor.close()
        }
    }

    class VerticalMode(private val interactor: PaymentMethodVerticalLayoutInteractor) : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = false,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(
                if (isWalletEnabled) {
                    null
                } else if (isCompleteFlow) {
                    R.string.stripe_paymentsheet_select_payment_method.resolvableString
                } else {
                    R.string.stripe_paymentsheet_choose_payment_method.resolvableString
                }
            )
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return interactor.showsWalletsHeader
        }

        @Composable
        override fun Content(modifier: Modifier) {
            PaymentMethodVerticalLayoutUI(interactor)
        }
    }

    class VerticalModeForm(
        private val interactor: VerticalModeFormInteractor,
        private val showsWalletHeader: Boolean = false,
    ) : PaymentSheetScreen, Closeable {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = interactor.canGoBack(),
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(null)
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(showsWalletHeader)
        }

        @Composable
        override fun Content(modifier: Modifier) {
            VerticalModeFormUI(interactor)
        }

        override fun close() {
            interactor.close()
        }
    }

    class ManageSavedPaymentMethods(private val interactor: ManageScreenInteractor) : PaymentSheetScreen, Closeable {
        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return interactor.state.mapAsStateFlow { state ->
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = true,
                    isLiveMode = interactor.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Maybe(
                        isEditing = state.isEditing,
                        canEdit = state.canEdit,
                        onEditIconPressed = {
                            interactor.handleViewAction(ManageScreenInteractor.ViewAction.ToggleEdit)
                        },
                    ),
                )
            }
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return interactor.state.mapAsStateFlow { state ->
                val title = if (state.isEditing) {
                    R.string.stripe_paymentsheet_manage_payment_methods
                } else {
                    R.string.stripe_paymentsheet_select_payment_method
                }

                title.resolvableString
            }
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> =
            stateFlowOf(false)

        @Composable
        override fun Content(modifier: Modifier) {
            ManageScreenUI(interactor = interactor)
        }

        override fun close() {
            interactor.close()
        }
    }

    class ManageOneSavedPaymentMethod(private val interactor: ManageOneSavedPaymentMethodInteractor) :
        PaymentSheetScreen {
        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = true,
                    isLiveMode = interactor.state.isLiveMode,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean): StateFlow<ResolvableString?> {
            return stateFlowOf(R.string.stripe_paymentsheet_remove_pm_title.resolvableString)
        }

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> =
            stateFlowOf(false)

        @Composable
        override fun Content(modifier: Modifier) {
            ManageOneSavedPaymentMethodUI(interactor = interactor)
        }
    }

    class CvcRecollection(
        private val args: CvcRecollectionContract.Args,
        private val interactor: CvcRecollectionInteractor
    ) : PaymentSheetScreen, Closeable {
        override val showsBuyButton = false
        override val showsContinueButton = false

        override fun topBarState(): StateFlow<PaymentSheetTopBarState?> {
            return stateFlowOf(
                PaymentSheetTopBarStateFactory.create(
                    hasBackStack = false,
                    isLiveMode = false,
                    editable = PaymentSheetTopBarState.Editable.Never,
                )
            )
        }

        override fun title(isCompleteFlow: Boolean, isWalletEnabled: Boolean) = stateFlowOf(null)

        override fun showsWalletsHeader(isCompleteFlow: Boolean) = stateFlowOf(false)

        @Composable
        override fun Content(modifier: Modifier) {
            val viewModel = viewModel<CvcRecollectionViewModel>(
                factory = CvcRecollectionViewModel.Factory(args)
            )
            val state = viewModel.viewState.collectAsState()

            LaunchedEffect(state) {
                viewModel.result.collectLatest { result ->
                    interactor.handleEffect(CvcRecollectionInteractor.Effect.SendCvcRecollectionEffect(result))
                }
            }

            CvcRecollectionScreen(
                cardBrand = state.value.cardBrand,
                lastFour = state.value.lastFour,
                displayMode = state.value.displayMode,
                viewActionHandler = viewModel::handleViewAction
            )
        }

        override fun close() {
        }

    }
}
