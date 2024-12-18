package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.SelectedBadge
import com.stripe.android.uicore.utils.collectAsState

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
@Composable
internal fun ManageScreenUI(interactor: ManageScreenInteractor) {
    val horizontalPadding = dimensionResource(
        id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )

    val state by interactor.state.collectAsState()

    Column(
        modifier = Modifier
            .padding(horizontal = horizontalPadding)
            .testTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.paymentMethods.forEach {
            val isSelected = it == state.currentSelection

            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = it,
                isEnabled = true,
                isClickable = FeatureFlags.useNewUpdateCardScreen.isEnabled || !state.isEditing,
                isSelected = isSelected,
                trailingContent = {
                    TrailingContent(
                        isSelected = isSelected,
                        isEditing = state.isEditing,
                        isModifiable = it.isModifiable(),
                        canRemove = state.canRemove,
                        paymentMethod = it,
                        deletePaymentMethod = { paymentMethod ->
                            interactor.handleViewAction(
                                ManageScreenInteractor.ViewAction.DeletePaymentMethod(paymentMethod)
                            )
                        },
                        editPaymentMethod = { paymentMethod ->
                            interactor.handleViewAction(
                                ManageScreenInteractor.ViewAction.EditPaymentMethod(paymentMethod)
                            )
                        }
                    )
                },
                onClick = {
                    rowOnClick(
                        isEditing = state.isEditing,
                        selectPaymentMethod = {
                            interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(it))
                        },
                        updatePaymentMethod = {
                            interactor.handleViewAction(ManageScreenInteractor.ViewAction.UpdatePaymentMethod(it))
                        }
                    )
                }
            )
        }
    }
}

private fun rowOnClick(isEditing: Boolean, selectPaymentMethod: () -> Unit, updatePaymentMethod: () -> Unit) {
    if (isEditing && FeatureFlags.useNewUpdateCardScreen.isEnabled) {
        updatePaymentMethod()
    }

    if (!isEditing) {
        selectPaymentMethod()
    }
}

@Composable
private fun TrailingContent(
    isSelected: Boolean,
    isEditing: Boolean,
    isModifiable: Boolean,
    canRemove: Boolean,
    paymentMethod: DisplayableSavedPaymentMethod,
    deletePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    editPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
) {
    if (FeatureFlags.useNewUpdateCardScreen.isEnabled) {
        ChevronTrailingContent(
            paymentMethod = paymentMethod,
            isEditing = isEditing,
            isSelected = isSelected,
        )
    } else {
        TrailingContentWithIcons(
            isEditing,
            isModifiable,
            paymentMethod,
            editPaymentMethod,
            canRemove,
            deletePaymentMethod,
            isSelected
        )
    }
}

@Composable
private fun ChevronTrailingContent(
    paymentMethod: DisplayableSavedPaymentMethod,
    isEditing: Boolean,
    isSelected: Boolean,
) {
    if (isEditing) {
        ChevronIcon(paymentMethodId = paymentMethod.paymentMethod.id)
    } else if (isSelected) {
        SelectedBadge()
    }
}

@Composable
private fun TrailingContentWithIcons(
    isEditing: Boolean,
    isModifiable: Boolean,
    paymentMethod: DisplayableSavedPaymentMethod,
    editPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    canRemove: Boolean,
    deletePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    isSelected: Boolean
) {
    if (isEditing && isModifiable) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EditIcon(paymentMethod, editPaymentMethod)
            if (canRemove) {
                DeleteIcon(paymentMethod, deletePaymentMethod)
            }
        }
    } else if (isEditing && canRemove) {
        DeleteIcon(paymentMethod, deletePaymentMethod)
    } else if (isSelected) {
        SelectedBadge()
    }
}

internal const val TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST = "manage_screen_saved_pms_list"
