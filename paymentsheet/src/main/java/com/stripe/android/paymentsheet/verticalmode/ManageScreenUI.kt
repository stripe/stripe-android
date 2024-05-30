package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R

@Composable
internal fun ManageScreenUI(interactor: ManageScreenInteractor) {
    val horizontalPadding = dimensionResource(
        id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )

    val state by interactor.state.collectAsState()

    Column(
        modifier = Modifier.padding(horizontal = horizontalPadding).testTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.paymentMethods.forEach {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = it,
                resources = LocalContext.current.resources,
                isEnabled = true,
                isSelected = false
            )
        }
    }
}

internal const val TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST = "manage_screen_saved_pms_list"
