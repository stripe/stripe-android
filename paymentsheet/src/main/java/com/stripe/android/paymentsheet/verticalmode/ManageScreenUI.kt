package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun ManageScreenUI(interactor: ManageScreenInteractor) {
    val horizontalPadding = StripeTheme.getOuterFormInsets()

    val state by interactor.state.collectAsState()

    Column(
        modifier = Modifier
            .padding(horizontalPadding)
            .testTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.paymentMethods.forEach {
            val isSelected = it == state.currentSelection

            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = it,
                isEnabled = true,
                isSelected = isSelected,
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
                },
                trailingContent = {
                    TrailingContent(
                        isEditing = state.isEditing,
                        paymentMethod = it,
                    )
                }
            )
        }
    }
}

private fun rowOnClick(isEditing: Boolean, selectPaymentMethod: () -> Unit, updatePaymentMethod: () -> Unit) {
    if (isEditing) {
        updatePaymentMethod()
    } else {
        selectPaymentMethod()
    }
}

@Composable
private fun TrailingContent(
    paymentMethod: DisplayableSavedPaymentMethod,
    isEditing: Boolean,
) {
    if (isEditing) {
        ChevronIcon(paymentMethodId = paymentMethod.paymentMethod.id)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST = "manage_screen_saved_pms_list"
