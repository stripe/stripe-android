package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.stripe.android.ui.core.PaymentsTheme


@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun BsbElementUI(
    enabled: Boolean,
    element: BsbElement
) {
    val error by element.textElement.controller.error.collectAsState(null)
    val bankName by element.bankName.collectAsState(null)
    val sectionErrorString = error?.let {
        it.formatArgs?.let { args ->
            stringResource(
                it.errorMessage,
                *args
            )
        } ?: stringResource(it.errorMessage)
    }
    Column {
        Section(
            null,
            sectionErrorString,
            contentInCard = {
                TextField(element.textElement.controller, enabled = enabled)
            },
            contentOutsideCard = {
                bankName?.let {
                    Text(
                        it,
                        color = PaymentsTheme.colors.colorTextSecondary
                    )
                }
            }
        )
    }

}
