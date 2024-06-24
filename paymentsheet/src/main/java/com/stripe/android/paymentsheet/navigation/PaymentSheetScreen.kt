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
import com.stripe.android.paymentsheet.ui.CvcRecollectionField
import com.stripe.android.paymentsheet.ui.EditPaymentMethod
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodTabLayoutUI
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodsTopContentPadding
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

    fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean

    @Composable
    fun Content(viewModel: BaseSheetViewModel, modifier: Modifier)

    object Loading : PaymentSheetScreen {

        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false
        override val canNavigateBack: Boolean = false

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean {
            return false
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            BottomSheetLoadingIndicator(modifier)
        }
    }

    data class SelectSavedPaymentMethods(
        val cvcRecollectionState: CvcRecollectionState = CvcRecollectionState.NotRequired,
    ) : PaymentSheetScreen {

        sealed interface CvcRecollectionState {
            data object NotRequired : CvcRecollectionState
            class Required(val cvcControllerFlow: StateFlow<CvcController>) : CvcRecollectionState
        }

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = false
        override val canNavigateBack: Boolean = false

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean {
            return isCompleteFlow
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            val state by viewModel.paymentOptionsState.collectAsState()
            val isEditing by viewModel.editing.collectAsState()
            val isProcessing by viewModel.processing.collectAsState()

            SavedPaymentMethodTabLayoutUI(
                state = state,
                isEditing = isEditing,
                isProcessing = isProcessing,
                onAddCardPressed = viewModel::transitionToAddPaymentScreen,
                onItemSelected = viewModel::handlePaymentMethodSelected,
                onModifyItem = viewModel::modifyPaymentMethod,
                onItemRemoved = viewModel::removePaymentMethod,
                modifier = modifier,
            )

            if (
                cvcRecollectionState is CvcRecollectionState.Required &&
                (state.selectedItem as? PaymentOptionsItem.SavedPaymentMethod)
                    ?.paymentMethod?.type == PaymentMethod.Type.Card
            ) {
                CvcRecollectionField(cvcControllerFlow = cvcRecollectionState.cvcControllerFlow, isProcessing)
            }
        }
    }

    object AddAnotherPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true
        override val canNavigateBack: Boolean = true

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean {
            return isCompleteFlow
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            AddPaymentMethod(viewModel, modifier)
        }
    }

    object AddFirstPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true
        override val canNavigateBack: Boolean = false

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean {
            return true
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            AddPaymentMethod(viewModel, modifier)
        }
    }

    data class EditPaymentMethod(
        val interactor: ModifiableEditPaymentMethodViewInteractor,
    ) : PaymentSheetScreen, Closeable {

        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false
        override val canNavigateBack: Boolean = true

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean {
            return false
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

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean {
            return true
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

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean {
            return showsWalletHeader
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

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean = false

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

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean = false

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            ManageOneSavedPaymentMethodUI(interactor = interactor)
        }
    }
}
