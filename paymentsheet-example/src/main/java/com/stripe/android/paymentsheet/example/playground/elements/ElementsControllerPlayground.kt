package com.stripe.android.paymentsheet.example.playground.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.elements.ElementsController

@Composable
internal fun ElementsControllerPlayground(
    elementsController: ElementsController,
    configurationResult: Result<Unit>?,
    lastShippingUpdate: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Elements Controller")
        when (val result = configurationResult) {
            null -> CircularProgressIndicator()
            else -> {
                if (result.isSuccess) {
                    Text("Express Checkout Element")
                    elementsController.expressCheckoutElement.Content()
                } else {
                    Text(
                        text = "Configuration failed: ${result.exceptionOrNull()?.message}",
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.testTag(ELEMENTS_CONTROLLER_ERROR_TAG),
                    )
                }
            }
        }
        lastShippingUpdate?.let {
            Text(it, modifier = Modifier.testTag(ELEMENTS_CONTROLLER_UPDATE_TAG))
        }
    }
}

internal const val ELEMENTS_CONTROLLER_ERROR_TAG = "elements_controller_error"
internal const val ELEMENTS_CONTROLLER_UPDATE_TAG = "elements_controller_shipping_update"
