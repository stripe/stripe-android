package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.RemovePaymentMethodDialogUI
import com.stripe.android.paymentsheet.ui.SelectedBadge

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
                resources = LocalContext.current.resources,
                isEnabled = true,
                isSelected = isSelected,
                trailingContent = {
                    TrailingContent(
                        isSelected = isSelected,
                        isEditing = state.isEditing,
                        isModifiable = it.isModifiable(),
                        paymentMethod = it,
                        interactor = interactor,
                    )
                },
                onClick = {
                    if (!state.isEditing) {
                        interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(it))
                    }
                },
            )
        }
    }
}

@Composable
private fun TrailingContent(
    isSelected: Boolean,
    isEditing: Boolean,
    isModifiable: Boolean,
    paymentMethod: DisplayableSavedPaymentMethod,
    interactor: ManageScreenInteractor,
) {
    if (isEditing && isModifiable) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EditIcon(paymentMethod)
            DeleteIcon(paymentMethod, interactor)
        }
    } else if (isEditing) {
        DeleteIcon(paymentMethod, interactor)
    } else if (isSelected) {
        SelectedBadge()
    }
}

@Composable
private fun DeleteIcon(paymentMethod: DisplayableSavedPaymentMethod, interactor: ManageScreenInteractor) {
    val openRemoveDialog = rememberSaveable { mutableStateOf(false) }
    val paymentMethodId = paymentMethod.paymentMethod.id

    TrailingIcon(
        backgroundColor = Color.Red,
        icon = Icons.Filled.Close,
        modifier = Modifier.testTag("${TEST_TAG_MANAGE_SCREEN_DELETE_ICON}_$paymentMethodId"),
        onClick = {
            openRemoveDialog.value = true
        },
    )

    if (openRemoveDialog.value) {
        RemovePaymentMethodDialogUI(paymentMethod = paymentMethod, onConfirmListener = {
            openRemoveDialog.value = false
            interactor.handleViewAction(ManageScreenInteractor.ViewAction.DeletePaymentMethod(paymentMethod))
        }, onDismissListener = {
            openRemoveDialog.value = false
        })
    }
}

@Composable
private fun EditIcon(paymentMethod: DisplayableSavedPaymentMethod) {
    val paymentMethodId = paymentMethod.paymentMethod.id

    TrailingIcon(
        backgroundColor = Color.Gray,
        icon = Icons.Filled.Edit,
        modifier = Modifier.testTag("${TEST_TAG_MANAGE_SCREEN_EDIT_ICON}_$paymentMethodId"),
        onClick = {},
    )
}

@Composable
private fun TrailingIcon(backgroundColor: Color, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .size(24.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
    }
}

internal const val TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST = "manage_screen_saved_pms_list"
internal const val TEST_TAG_MANAGE_SCREEN_EDIT_ICON = "manage_screen_edit_icon"
internal const val TEST_TAG_MANAGE_SCREEN_DELETE_ICON = "manage_screen_delete_icon"
