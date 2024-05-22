package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.ui.EditPaymentMethod
import com.stripe.android.paymentsheet.ui.FormElement
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.NewPaymentMethodVerticalLayoutUI
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodTabLayoutUI
import com.stripe.android.paymentsheet.ui.SavedPaymentMethodsTopContentPadding
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.utils.collectAsStateSafely
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

    data object SelectSavedPaymentMethods : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = false
        override val canNavigateBack: Boolean = false

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean {
            return isCompleteFlow
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            val state by viewModel.paymentOptionsState.collectAsStateSafely()
            val isEditing by viewModel.editing.collectAsStateSafely()
            val isProcessing by viewModel.processing.collectAsStateSafely()

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

    object VerticalMode : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true
        override val canNavigateBack: Boolean = false

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean {
            return true
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            val context = LocalContext.current
            val imageLoader = remember {
                StripeImageLoader(context.applicationContext)
            }

            val paymentMethodMetadata by viewModel.paymentMethodMetadata.collectAsStateSafely()

            val supportedPaymentMethods = remember(paymentMethodMetadata) {
                paymentMethodMetadata?.sortedSupportedPaymentMethods() ?: emptyList()
            }

            val isProcessing by viewModel.processing.collectAsStateSafely()

            NewPaymentMethodVerticalLayoutUI(
                paymentMethods = supportedPaymentMethods,
                selectedIndex = -1,
                isEnabled = !isProcessing,
                onViewMorePaymentMethods = { viewModel.transitionTo(ManageSavedPaymentMethods) },
                onItemSelectedListener = { viewModel.transitionTo(Form(it.code)) },
                imageLoader = imageLoader,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }

    data class Form(private val selectedPaymentMethodCode: String) : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true
        override val canNavigateBack: Boolean = true

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean {
            return false
        }

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            val usBankAccountFormArguments = remember(selectedPaymentMethodCode) {
                USBankAccountFormArguments.create(viewModel, selectedPaymentMethodCode)
            }
            val formElements = remember(selectedPaymentMethodCode) {
                viewModel.formElementsForCode(selectedPaymentMethodCode)
            }
            val formArguments = remember(selectedPaymentMethodCode) {
                viewModel.createFormArguments(selectedPaymentMethodCode)
            }
            val isProcessing by viewModel.processing.collectAsStateSafely()

            FormElement(
                enabled = !isProcessing,
                selectedPaymentMethodCode = selectedPaymentMethodCode,
                formElements = formElements,
                formArguments = formArguments,
                usBankAccountFormArguments = usBankAccountFormArguments,
                horizontalPadding = 20.dp,
                onFormFieldValuesChanged = { formValues ->
                    viewModel.onFormFieldValuesChanged(formValues, selectedPaymentMethodCode)
                },
                onInteractionEvent = {
                    viewModel.reportFieldInteraction(selectedPaymentMethodCode)
                },
            )
        }
    }

    data object ManageSavedPaymentMethods : PaymentSheetScreen {
        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false
        override val canNavigateBack: Boolean = true

        override fun showsWalletsHeader(isCompleteFlow: Boolean): Boolean = false

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            Text("Manage your saved PMs here")
        }
    }
}

@Composable
internal fun PaymentSheetScreen.Content(viewModel: BaseSheetViewModel) {
    Content(viewModel, modifier = Modifier)
}
