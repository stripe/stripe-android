package com.stripe.android.link.ui.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.linkColors
import com.stripe.android.ui.core.FormController
import com.stripe.android.ui.core.FormUI
import kotlinx.coroutines.flow.Flow

@Composable
internal fun Form(
    formController: FormController,
    enabledFlow: Flow<Boolean>
) {
    FormUI(
        formController.hiddenIdentifiers,
        enabledFlow,
        formController.elements,
        formController.lastTextFieldIdentifier
    ) {
        Row(
            modifier = Modifier
                .height(100.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.linkColors.buttonLabel,
                strokeWidth = 2.dp
            )
        }
    }
}
