package com.stripe.android.customersheet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.PaymentSheetScaffold
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.ui.core.elements.H4Text

@Composable
internal fun CustomerSheetScreen(
    header: String?,
    isLiveMode: Boolean,
    isProcessing: Boolean,
    isEditing: Boolean,
    onBackPressed: () -> Unit = {},
    onEdit: () -> Unit = {}
) {
    val horizontalPadding = dimensionResource(R.dimen.stripe_paymentsheet_outer_spacing_horizontal)
    val bottomPadding = dimensionResource(R.dimen.stripe_paymentsheet_button_container_spacing_bottom)

    PaymentSheetScaffold(
        topBar = {
            PaymentSheetTopBar(
                screen = PaymentSheetScreen.SelectSavedPaymentMethods,
                showEditMenu = isEditing,
                isLiveMode = isLiveMode,
                isProcessing = isProcessing,
                isEditing = isEditing,
                handleBackPressed = onBackPressed,
                toggleEditing = onEdit,
            )
        },
        content = {
            Column(
                modifier = Modifier.padding(horizontal = horizontalPadding)
            ) {
                H4Text(
                    text = header ?: stringResource(
                        R.string.stripe_paymentsheet_select_payment_method
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text("Hello world!")
            }
        },
        modifier = Modifier.padding(bottom = bottomPadding)
    )
}
