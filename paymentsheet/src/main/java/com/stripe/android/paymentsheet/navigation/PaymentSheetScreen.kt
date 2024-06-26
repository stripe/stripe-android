package com.stripe.android.paymentsheet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.CvcRecollectionField
import com.stripe.android.paymentsheet.ui.EditPaymentMethod
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodTabLayoutUI
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodsTopContentPadding
import com.stripe.android.paymentsheet.ui.SheetScreen
import com.stripe.android.paymentsheet.ui.SelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageOneSavedPaymentMethodInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageOneSavedPaymentMethodUI
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenUI
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutUI
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import java.io.Closeable

internal val PaymentSheetScreen.topContentPadding: Dp
    get() = when (this) {
        is PaymentSheetScreen.SelectSavedPaymentMethods -> {
            SavedPaymentMethodsTopContentPadding
        }
        is PaymentSheetScreen.Loading,
        is PaymentSheetScreen.VerticalMode,
        is PaymentSheetScreen.Form,
        is PaymentSheetScreen.AddFirstPaymentMethod,
        is PaymentSheetScreen.AddAnotherPaymentMethod,
        is PaymentSheetScreen.ManageSavedPaymentMethods,
        is PaymentSheetScreen.ManageOneSavedPaymentMethod,
        is PaymentSheetScreen.EditPaymentMethod -> {
            0.dp
        }
    }

internal sealed interface PaymentSheetScreen {

    val showsBuyButton: Boolean
    val showsContinueButton: Boolean
    val canNavigateBack: Boolean
    val sheetScreen: SheetScreen

    fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean>

    @Composable
    fun Content(viewModel: BaseSheetViewModel, modifier: Modifier)

    object Loading : PaymentSheetScreen {

        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false
        override val canNavigateBack: Boolean = false
        override val sheetScreen: SheetScreen = SheetScreen.LOADING

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(false)
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            BottomSheetLoadingIndicator(modifier)
        }
    }

    data class SelectSavedPaymentMethods constructor(
        val selectSavedPaymentMethodsInteractor: SelectSavedPaymentMethodsInteractor,
        val cvcRecollectionState: CvcRecollectionState = CvcRecollectionState.NotRequired,
    ) : PaymentSheetScreen {

        sealed interface CvcRecollectionState {
            data object NotRequired : CvcRecollectionState
            class Required(val cvcControllerFlow: StateFlow<CvcController>) : CvcRecollectionState
        }

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = false
        override val canNavigateBack: Boolean = false
        override val sheetScreen: SheetScreen = SheetScreen.SELECT_SAVED_PAYMENT_METHODS

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(isCompleteFlow)
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            val state by selectSavedPaymentMethodsInteractor.state.collectAsState()

            SavedPaymentMethodTabLayoutUI(
                state = state.paymentOptionsState,
                isEditing = state.isEditing,
                isProcessing = state.isProcessing,
                onAddCardPressed = {
                    selectSavedPaymentMethodsInteractor.handleViewAction(
                        SelectSavedPaymentMethodsInteractor.ViewAction.AddCardPressed
                    )
                },
                onItemSelected = {
                    selectSavedPaymentMethodsInteractor.handleViewAction(
                        SelectSavedPaymentMethodsInteractor.ViewAction.SelectPaymentMethod(
                            it
                        )
                    )
                },
                onModifyItem = {
                    selectSavedPaymentMethodsInteractor.handleViewAction(
                        SelectSavedPaymentMethodsInteractor.ViewAction.EditPaymentMethod(it)
                    )
                },
                onItemRemoved = {
                    selectSavedPaymentMethodsInteractor.handleViewAction(
                        SelectSavedPaymentMethodsInteractor.ViewAction.DeletePaymentMethod(it)
                    )
                },
                modifier = modifier,
            )

            if (
                cvcRecollectionState is CvcRecollectionState.Required &&
                (state.paymentOptionsState.selectedItem as? PaymentOptionsItem.SavedPaymentMethod)
                    ?.paymentMethod?.type == PaymentMethod.Type.Card
            ) {
                CvcRecollectionField(
                    cvcControllerFlow = cvcRecollectionState.cvcControllerFlow,
                    state.isProcessing
                )
            }
        }
    }

    data class AddAnotherPaymentMethod(
        val interactor: AddPaymentMethodInteractor,
    ) : PaymentSheetScreen, Closeable {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true
        override val canNavigateBack: Boolean = true
        override val sheetScreen: SheetScreen = SheetScreen.ADD_ANOTHER_PAYMENT_METHOD

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(isCompleteFlow)
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            AddPaymentMethod(interactor = interactor, modifier)
        }

        override fun close() {
            interactor.close()
        }
    }

    data class AddFirstPaymentMethod(
        val interactor: AddPaymentMethodInteractor,
    ) : PaymentSheetScreen, Closeable {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true
        override val canNavigateBack: Boolean = false
        override val sheetScreen: SheetScreen = SheetScreen.ADD_FIRST_PAYMENT_METHOD

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(true)
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            AddPaymentMethod(interactor = interactor, modifier)
        }

        override fun close() {
            interactor.close()
        }
    }

    data class EditPaymentMethod(
        val interactor: ModifiableEditPaymentMethodViewInteractor,
    ) : PaymentSheetScreen, Closeable {

        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false
        override val canNavigateBack: Boolean = true
        override val sheetScreen: SheetScreen = SheetScreen.EDIT_PAYMENT_METHOD

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(false)
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            EditPaymentMethod(interactor, modifier)
        }

        override fun close() {
            interactor.close()
        }
    }

    class VerticalMode(private val interactor: PaymentMethodVerticalLayoutInteractor) : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true
        override val canNavigateBack: Boolean = false
        override val sheetScreen: SheetScreen = SheetScreen.VERTICAL_MODE

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return interactor.showsWalletsHeader
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            PaymentMethodVerticalLayoutUI(interactor)
        }
    }

    class Form(
        private val interactor: VerticalModeFormInteractor,
        private val showsWalletHeader: Boolean = false,
    ) : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true
        override val canNavigateBack: Boolean = true
        override val sheetScreen: SheetScreen = SheetScreen.FORM

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> {
            return stateFlowOf(showsWalletHeader)
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            VerticalModeFormUI(interactor)
        }
    }

    class ManageSavedPaymentMethods(private val interactor: ManageScreenInteractor) : PaymentSheetScreen, Closeable {
        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false
        override val canNavigateBack: Boolean = true
        override val sheetScreen: SheetScreen = SheetScreen.MANAGE_SAVED_PAYMENT_METHODS

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> = stateFlowOf(false)

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
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
        override val canNavigateBack: Boolean = true
        override val sheetScreen: SheetScreen = SheetScreen.MANAGE_ONE_SAVED_PAYMENT_METHOD

        override fun showsWalletsHeader(isCompleteFlow: Boolean): StateFlow<Boolean> = stateFlowOf(false)

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            ManageOneSavedPaymentMethodUI(interactor = interactor)
        }
    }
}
